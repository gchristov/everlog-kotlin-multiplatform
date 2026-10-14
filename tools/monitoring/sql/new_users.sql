-- One row per real new user (Android, installed since 2026-01-01), with the flags every
-- new-user metric is built from. No ids: the dashboard only ever needs counts.
-- New users = new accounts (user_registered) on real installs, plus real installs that never
-- got an account. Returning users logging in to an older account are left out.
-- Definitions: tools/monitoring/README.md.
WITH ev AS (
  SELECT user_pseudo_id AS device, user_id, event_name, TIMESTAMP_MICROS(event_timestamp) AS ts,
    app_info.version AS version, app_info.install_source AS source, geo.country AS country,
    traffic_source.medium AS medium, traffic_source.name AS campaign,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'firebase_screen') AS screen,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'step') AS step,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'outcome') AS outcome,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'userId') AS param_uid
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20260101'
),
-- Devices that ever used the Test Lab account (robots) or the owner's account
excluded AS (
  SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*`
  WHERE user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
),
installs AS (
  SELECT device, MIN(ts) AS installed, ANY_VALUE(version) AS version,
    ANY_VALUE(medium) AS medium, ANY_VALUE(campaign) AS campaign
  FROM ev
  WHERE event_name = 'first_open' AND IFNULL(source, '') != 'manual_install' AND IFNULL(country, '') != ''
    AND IFNULL(version, '') NOT LIKE '%nightly%'
  GROUP BY device
  HAVING device NOT IN (SELECT device FROM excluded)
),
regs AS (
  SELECT e.device, COALESCE(e.user_id, e.param_uid) AS uid, MIN(e.ts) AS signed_up
  FROM ev e JOIN installs USING (device)
  WHERE e.event_name = 'user_registered' AND COALESCE(e.user_id, e.param_uid) IS NOT NULL
  GROUP BY 1, 2
),
-- First signup per account
accounts AS (
  SELECT uid, ARRAY_AGG(device ORDER BY signed_up LIMIT 1)[OFFSET(0)] AS device FROM regs GROUP BY uid
),
no_account AS (
  SELECT device FROM installs
  WHERE device NOT IN (SELECT device FROM ev WHERE user_id IS NOT NULL)
    AND device NOT IN (SELECT device FROM regs)
),
users AS (
  SELECT uid AS user_key, uid, device FROM accounts
  UNION ALL
  SELECT CONCAT('dev:', device), NULL, device FROM no_account
),
-- Each user's events: their account's on any device, plus anonymous ones on the install device
uev AS (
  SELECT u.user_key, e.* FROM users u JOIN ev e ON e.user_id = u.uid
  UNION ALL
  SELECT u.user_key, e.* FROM users u JOIN ev e ON e.device = u.device AND e.user_id IS NULL
),
x AS (
  SELECT u.user_key, i.installed, i.version AS install_version, i.medium AS install_medium, i.campaign AS install_campaign,
    e.event_name, e.ts, e.screen, e.step, e.outcome,
    TIMESTAMP_DIFF(e.ts, i.installed, SECOND) / 86400 AS d
  FROM users u JOIN installs i USING (device) JOIN uev e USING (user_key)
  WHERE e.ts >= TIMESTAMP_SUB(i.installed, INTERVAL 1 MINUTE)
),
completions AS (
  SELECT user_key, ARRAY_AGG(d ORDER BY ts LIMIT 2) AS ds
  FROM x WHERE event_name IN ('workout_completed', 'workout_service_workout_completed')
  GROUP BY user_key
),
agg AS (
  SELECT user_key,
    ANY_VALUE(installed) AS installed, ANY_VALUE(install_version) AS install_version,
    ANY_VALUE(install_medium) AS medium, ANY_VALUE(install_campaign) AS campaign,
    LOGICAL_OR(screen = 'screen_home' OR event_name = 'screen_home') AS reached_home,
    -- Onboarding (2.16.0+): steps seen and completed, and how it ended
    STRING_AGG(DISTINCT IF(event_name = 'onboarding_step_viewed', step, NULL)) AS ob_viewed,
    STRING_AGG(DISTINCT IF(event_name = 'onboarding_step_completed', step, NULL)) AS ob_completed,
    ARRAY_AGG(IF(event_name = 'onboarding_finished', outcome, NULL) IGNORE NULLS ORDER BY ts LIMIT 1)[SAFE_OFFSET(0)] AS ob_outcome,
    -- Setup on day one: kept the starter templates, created a routine, exercise or plan, or started a plan.
    -- Onboarding's own settings_*_modified events don't count.
    LOGICAL_OR(d < 1 AND (
      (event_name = 'onboarding_finished' AND outcome IN ('starter_templates', 'own_template'))
      OR event_name IN ('routine_created', 'exercise_created', 'exercise_created_suggestion', 'plan_created', 'plan_started'))) AS setup_d0,
    LOGICAL_OR(d < 1 AND event_name = 'workout_started') AS started_d0,
    LOGICAL_OR(d < 7 AND event_name = 'workout_started') AS started_w1,
    COUNTIF(d < 7 AND event_name IN ('workout_completed', 'workout_service_workout_completed')) AS completed_w1,
    COUNTIF(d >= 7 AND d < 30 AND event_name IN ('workout_completed', 'workout_service_workout_completed')) AS completed_d7_30,
    MIN(IF(event_name = 'app_remove', d, NULL)) AS uninstall_d
  FROM x GROUP BY user_key
)
SELECT
  FORMAT_TIMESTAMP('%F', installed) AS install_date,
  ROUND(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), installed, SECOND) / 86400, 2) AS days_observed,
  install_version,
  CASE WHEN campaign LIKE 'UAC - Installs - Onboarding v1%' THEN campaign
       WHEN medium = 'cpc' THEN 'Other paid'
       ELSE 'Organic' END AS channel,
  a.user_key LIKE 'dev:%' AS no_account,
  reached_home, ob_viewed, ob_completed, ob_outcome,
  setup_d0, started_d0, started_w1, completed_w1, completed_d7_30,
  ROUND(c.ds[SAFE_OFFSET(0)], 3) AS first_completion_d,
  ROUND(c.ds[SAFE_OFFSET(1)], 3) AS second_completion_d,
  ROUND(uninstall_d, 3) AS uninstall_d
FROM agg a LEFT JOIN completions c USING (user_key)
ORDER BY install_date
