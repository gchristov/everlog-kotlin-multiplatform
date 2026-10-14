-- One row per real new user (2020-2026, Android): new accounts (user_registered) plus installs that never got an account.
-- Excludes Play pre-launch/Test Lab robots and returning users who logged in to an older account.
-- The per-user table behind the Funnel baseline (2020–2026) in Notion; every user-level number there is sliced from it.
WITH ev AS (
  SELECT user_pseudo_id AS device, user_id, event_name, TIMESTAMP_MICROS(event_timestamp) AS ts,
    app_info.version AS version, app_info.install_source AS source, geo.country AS country,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'firebase_screen') AS screen,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'source') AS start_source,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'method') AS method,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'type') AS set_type,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'userId') AS param_uid
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX NOT LIKE 'intraday%'
),
robots AS (SELECT DISTINCT device FROM ev WHERE user_id = 'V23IEC4EeqadnnbeqUGWZEgFXmC2'),
installs AS (
  SELECT device, MIN(ts) AS installed, ANY_VALUE(version) AS version, ANY_VALUE(country) AS country
  FROM ev
  WHERE event_name = 'first_open' AND IFNULL(source, '') != 'manual_install' AND IFNULL(country, '') != ''
    AND IFNULL(version, '') NOT LIKE '%nightly%'
  GROUP BY device
  HAVING device NOT IN (SELECT device FROM robots)
),
regs AS (
  SELECT e.device, COALESCE(e.user_id, e.param_uid) AS uid, MIN(e.ts) AS signed_up, ANY_VALUE(e.method) AS method
  FROM ev e JOIN installs i USING (device)
  WHERE e.event_name = 'user_registered' AND COALESCE(e.user_id, e.param_uid) IS NOT NULL
  GROUP BY 1, 2
),
-- First signup per account
accounts AS (
  SELECT uid, ARRAY_AGG(STRUCT(device, signed_up, method) ORDER BY signed_up LIMIT 1)[OFFSET(0)] AS r
  FROM regs GROUP BY uid
),
-- Devices that never had any account
no_account AS (
  SELECT i.device FROM installs i
  WHERE i.device NOT IN (SELECT device FROM ev WHERE user_id IS NOT NULL)
    AND i.device NOT IN (SELECT device FROM regs)
),
users AS (
  SELECT a.uid AS user_key, a.uid, a.r.device AS device, a.r.signed_up, a.r.method, TRUE AS registered FROM accounts a
  UNION ALL
  SELECT CONCAT('dev:', device), NULL, device, NULL, NULL, FALSE FROM no_account
),
-- All events for each user: their account's events on any device, plus anonymous events on the signup device
uev AS (
  SELECT u.user_key, e.* FROM users u JOIN ev e ON e.user_id = u.uid
  UNION ALL
  SELECT u.user_key, e.* FROM users u JOIN ev e ON e.device = u.device AND e.user_id IS NULL
),
base AS (
  SELECT u.*, i.installed, i.version AS install_version, i.country FROM users u JOIN installs i USING (device)
),
x AS (
  SELECT b.user_key, b.registered, b.method, b.installed, b.signed_up, b.install_version, b.country,
    e.event_name, e.ts, e.screen, e.start_source, e.set_type,
    TIMESTAMP_DIFF(e.ts, b.installed, SECOND) / 86400 AS d
  FROM base b JOIN uev e USING (user_key)
  WHERE e.ts >= TIMESTAMP_SUB(b.installed, INTERVAL 1 MINUTE)
),
starts AS (
  -- workout starts with their type: source param (new) or the companion event within 2s (old)
  SELECT s.user_key, s.ts,
    COALESCE(s.start_source,
      IF(LOGICAL_OR(c.event_name = 'workout_quick_started'), 'quick',
      IF(LOGICAL_OR(c.event_name = 'workout_from_routine_started'), 'routine', 'unknown'))) AS src
  FROM x s LEFT JOIN x c ON c.user_key = s.user_key
    AND c.event_name IN ('workout_quick_started', 'workout_from_routine_started')
    AND ABS(TIMESTAMP_DIFF(c.ts, s.ts, SECOND)) <= 2
  WHERE s.event_name = 'workout_started'
  GROUP BY s.user_key, s.ts, s.start_source
),
first_start AS (
  SELECT user_key, ARRAY_AGG(STRUCT(ts, src) ORDER BY ts LIMIT 1)[OFFSET(0)] AS f FROM starts GROUP BY user_key
),
first_outcome AS (
  SELECT fs.user_key, fs.f.ts AS fw_start, fs.f.src AS fw_source,
    MIN(IF(x.event_name IN ('workout_completed', 'workout_service_workout_completed') AND x.ts > fs.f.ts, x.ts, NULL)) AS fw_completed,
    MIN(IF(x.event_name = 'workout_stopped' AND x.ts > fs.f.ts, x.ts, NULL)) AS fw_stopped,
    MIN(IF(x.event_name = 'workout_started' AND x.ts > fs.f.ts, x.ts, NULL)) AS next_start
  FROM first_start fs JOIN x USING (user_key)
  GROUP BY 1, 2, 3
),
first_sets AS (
  SELECT o.user_key,
    COUNTIF(x.event_name IN ('workout_timer_started_rest') AND x.ts >= o.fw_start
      AND x.ts <= LEAST(COALESCE(o.fw_completed, TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)), COALESCE(o.fw_stopped, TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)), COALESCE(o.next_start, TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)), TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR))) AS fw_sets,
    COUNTIF(x.event_name LIKE 'workout_service_%' AND x.ts >= o.fw_start
      AND x.ts <= LEAST(COALESCE(o.fw_completed, TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)), COALESCE(o.fw_stopped, TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)), TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR))) AS fw_notif_actions,
    COUNTIF(x.event_name IN ('screen_exercise_details') OR (x.event_name = 'screen_view' AND x.screen IN ('screen_exercise_details','screen_exercise_info','screen_exercise_history','screen_exercise_statistics'))
      AND x.ts >= o.fw_start AND x.ts <= TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)) AS fw_exercise_browse
  FROM first_outcome o JOIN x USING (user_key)
  GROUP BY 1
),
completions AS (
  SELECT user_key, ts, d FROM x WHERE event_name IN ('workout_completed', 'workout_service_workout_completed')
),
second_c AS (
  SELECT user_key, ARRAY_AGG(d ORDER BY ts LIMIT 2) AS ds FROM completions GROUP BY 1
),
agg AS (
  SELECT user_key, ANY_VALUE(registered) registered, ANY_VALUE(method) method, ANY_VALUE(installed) installed,
    ANY_VALUE(signed_up) signed_up, ANY_VALUE(install_version) install_version, ANY_VALUE(country) country,
    DATE_DIFF(CURRENT_DATE(), DATE(ANY_VALUE(installed)), DAY) AS days_observed,
    LOGICAL_OR(screen = 'screen_login') AS saw_login,
    LOGICAL_OR(screen = 'screen_home' AND d < 1) AS home_d0,
    LOGICAL_OR(screen = 'screen_home') AS home_ever,
    -- day-0 exploration
    LOGICAL_OR(d < 1 AND screen = 'screen_week') AS d0_week,
    LOGICAL_OR(d < 1 AND screen = 'screen_workouts') AS d0_workouts_tab,
    LOGICAL_OR(d < 1 AND screen IN ('screen_statistics','screen_activity')) AS d0_stats,
    LOGICAL_OR(d < 1 AND screen = 'screen_settings') AS d0_settings,
    LOGICAL_OR(d < 1 AND screen = 'screen_history') AS d0_history,
    LOGICAL_OR(d < 1 AND screen = 'screen_exercises') AS d0_exercises_list,
    LOGICAL_OR(d < 1 AND screen IN ('screen_exercise_details','screen_exercise_info','screen_exercise_history','screen_exercise_statistics')) AS d0_exercise_detail,
    LOGICAL_OR(d < 1 AND screen = 'screen_routine_details') AS d0_routine_details,
    LOGICAL_OR(d < 1 AND screen = 'screen_plan_details') AS d0_plan_details,
    LOGICAL_OR(d < 1 AND screen = 'screen_pro') AS d0_pro,
    LOGICAL_OR(d < 1 AND event_name = 'routine_created') AS d0_routine_created,
    LOGICAL_OR(d < 1 AND event_name = 'routine_deleted') AS d0_routine_deleted,
    LOGICAL_OR(d < 1 AND event_name = 'plan_created') AS d0_plan_created,
    LOGICAL_OR(d < 1 AND event_name = 'plan_started') AS d0_plan_started,
    LOGICAL_OR(d < 1 AND event_name IN ('exercise_created','exercise_created_suggestion')) AS d0_exercise_created,
    LOGICAL_OR(d < 1 AND event_name LIKE 'settings_%_modified') AS d0_settings_changed,
    LOGICAL_OR(d < 1 AND event_name = 'settings_weekly_goal_modified') AS d0_weekly_goal,
    LOGICAL_OR(d < 1 AND event_name = 'settings_muscle_goal_modified') AS d0_muscle_goal,
    LOGICAL_OR(d < 1 AND event_name = 'integration_connected') AS d0_integration,
    LOGICAL_OR(d < 1 AND event_name = 'workout_started') AS d0_started,
    COUNTIF(d < 1 AND event_name IN ('workout_completed','workout_service_workout_completed')) AS d0_completed,
    -- lifetime
    COUNTIF(event_name = 'workout_started') AS starts_total,
    COUNTIF(event_name IN ('workout_completed','workout_service_workout_completed')) AS completed_total,
    COUNTIF(event_name IN ('workout_completed','workout_service_workout_completed') AND d < 7) AS completed_w1,
    COUNTIF(event_name IN ('workout_completed','workout_service_workout_completed') AND d >= 7 AND d < 14) AS completed_w2,
    COUNTIF(event_name IN ('workout_completed','workout_service_workout_completed') AND d >= 7 AND d < 30) AS completed_d7_30,
    COUNTIF(event_name IN ('workout_completed','workout_service_workout_completed') AND d >= 30 AND d < 90) AS completed_d30_90,
    COUNTIF(event_name IN ('workout_completed','workout_service_workout_completed') AND d >= 90) AS completed_d90_plus,
    COUNT(DISTINCT IF(event_name = 'user_engagement', DATE(ts), NULL)) AS active_days,
    COUNT(DISTINCT IF(event_name = 'user_engagement' AND d >= 1 AND d < 7, DATE(ts), NULL)) AS active_days_d1_7,
    LOGICAL_OR(event_name = 'user_engagement' AND d >= 1 AND d < 8) AS back_d1_7,
    LOGICAL_OR(event_name = 'user_engagement' AND d >= 8 AND d < 31) AS back_d8_30,
    LOGICAL_OR(event_name = 'user_engagement' AND d >= 31 AND d < 91) AS back_d31_90,
    LOGICAL_OR(event_name = 'user_engagement' AND d >= 91 AND d < 366) AS back_d91_365,
    MAX(IF(event_name = 'user_engagement', d, NULL)) AS last_active_d,
    MIN(IF(event_name = 'app_remove', d, NULL)) AS uninstall_d,
    -- feature use (lifetime)
    COUNTIF(event_name = 'routine_created') AS routines_created,
    COUNTIF(event_name = 'plan_started') AS plans_started,
    COUNTIF(event_name = 'plan_day_complete') AS plan_days_completed,
    COUNTIF(event_name = 'plan_completed') AS plans_completed,
    COUNTIF(event_name IN ('exercise_created','exercise_created_suggestion')) AS exercises_created,
    COUNTIF(event_name = 'set_type_selected' AND IFNULL(set_type,'SINGLE') != 'SINGLE') AS advanced_set_types,
    COUNTIF(event_name LIKE 'workout_service_%') AS notif_actions,
    COUNTIF(event_name = 'workout_timer_started_rest') AS rest_timers,
    COUNTIF(event_name = 'workout_timer_started_exercise') AS exercise_timers,
    COUNTIF(event_name = 'workout_change_muscle_goal') AS muscle_goal_changes,
    COUNTIF(event_name = 'workout_details_saved_as_routine') AS saved_as_routine,
    COUNTIF(event_name = 'workout_details_shared') AS shared,
    COUNTIF(event_name = 'integration_connected') AS integrations,
    COUNTIF(event_name = 'screen_view' AND screen IN ('screen_statistics','screen_activity')) AS stats_views,
    COUNTIF(event_name = 'screen_view' AND screen IN ('screen_exercise_statistics','screen_exercise_history')) AS exercise_stats_views,
    COUNTIF(event_name = 'screen_view' AND screen = 'screen_pro') AS pro_views,
    COUNTIF(event_name = 'pro_buy_purchased') AS pro_purchases,
    COUNTIF(event_name = 'app_star_rating') AS ratings,
    COUNTIF(event_name = 'workout_stopped') AS discards_total
  FROM x GROUP BY user_key
)
SELECT a.*,
  o.fw_source, TIMESTAMP_DIFF(o.fw_start, a.installed, MINUTE) AS fw_minutes_after_install,
  CASE WHEN o.fw_start IS NULL THEN NULL
       WHEN o.fw_completed IS NOT NULL AND o.fw_completed < TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR)
         AND (o.fw_stopped IS NULL OR o.fw_completed < o.fw_stopped)
         AND (o.next_start IS NULL OR o.fw_completed < o.next_start) THEN 'finished'
       WHEN o.fw_stopped IS NOT NULL AND o.fw_stopped < TIMESTAMP_ADD(o.fw_start, INTERVAL 4 HOUR) THEN 'discarded'
       ELSE 'left' END AS fw_outcome,
  TIMESTAMP_DIFF(LEAST(COALESCE(o.fw_completed, TIMESTAMP '2100-01-01'), COALESCE(o.fw_stopped, TIMESTAMP '2100-01-01')), o.fw_start, MINUTE) AS fw_minutes,
  fs.fw_sets, fs.fw_notif_actions, fs.fw_exercise_browse,
  c.ds[SAFE_OFFSET(0)] AS first_completion_d, c.ds[SAFE_OFFSET(1)] AS second_completion_d
FROM agg a
LEFT JOIN first_outcome o USING (user_key)
LEFT JOIN first_sets fs USING (user_key)
LEFT JOIN second_c c USING (user_key)
