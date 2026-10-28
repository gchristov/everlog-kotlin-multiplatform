-- Per app version (real users, Android, since 2.15.0): how workouts end, and how they come back after the app opened
-- without the workout screen, e.g. after being swiped away mid-workout. Up to 2.21.1 home asked to resume or discard
-- (home_prompts); from 2.21.2 it reopens the workout straight away (launch_resumes) and only prompts if that loops.
-- Each workout's events run until the device's next workout start.
WITH excluded AS (
  SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND (
    user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
    OR app_info.version LIKE '%nightly%' OR app_info.version LIKE '%-local%'
    OR app_info.install_source = 'manual_install')
),
ev AS (
  SELECT user_pseudo_id AS device, app_info.version AS version, event_name, event_timestamp AS ts,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'source') AS source
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20261001' AND IFNULL(geo.country, '') != ''
    AND user_pseudo_id NOT IN (SELECT device FROM excluded)
    AND event_name IN ('workout_started', 'workout_completed', 'workout_service_workout_completed', 'workout_stopped',
      'workout_discard_prompt_shown', 'workout_resumed')
),
tagged AS (
  SELECT *, COUNTIF(event_name = 'workout_started') OVER (PARTITION BY device ORDER BY ts) AS workout
  FROM ev
),
workouts AS (
  SELECT device, workout,
    ANY_VALUE(IF(event_name = 'workout_started', version, NULL)) AS version,
    COUNTIF(event_name IN ('workout_completed', 'workout_service_workout_completed')) > 0 AS finished,
    COUNTIF(event_name = 'workout_stopped') > 0 AS discarded,
    COUNTIF(event_name = 'workout_discard_prompt_shown' AND source = 'home') AS home_prompts,
    COUNTIF(event_name = 'workout_resumed' AND source = 'launch') AS launch_resumes
  FROM tagged
  WHERE workout > 0
  GROUP BY device, workout
)
SELECT version,
  COUNT(*) AS workouts,
  COUNTIF(finished) AS finished,
  COUNTIF(discarded AND NOT finished) AS discarded,
  ROUND(COUNTIF(finished) / COUNT(*), 2) AS finish_rate,
  SUM(home_prompts) AS home_prompts,
  SUM(launch_resumes) AS launch_resumes,
  COUNTIF(home_prompts + launch_resumes > 0) AS workouts_brought_back,
  COUNTIF(home_prompts + launch_resumes > 0 AND finished) AS brought_back_finished,
  COUNTIF(home_prompts + launch_resumes > 0 AND discarded AND NOT finished) AS brought_back_discarded,
  COUNT(DISTINCT device) AS devices
FROM workouts
WHERE version IS NOT NULL
GROUP BY version
ORDER BY version
