-- Workouts finished per week by all real users (Android, since 2025-01-01), and how many
-- people finished them. The loyal core is mostly 2020-2022 accounts, so this counts everyone,
-- not only new users. Leaves out robots and the owner's devices.
WITH excluded AS (
  SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*`
  WHERE user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
)
SELECT
  FORMAT_DATE('%F', DATE_TRUNC(PARSE_DATE('%Y%m%d', event_date), WEEK(MONDAY))) AS week,
  COUNT(*) AS workouts_completed,
  COUNT(DISTINCT COALESCE(user_id, user_pseudo_id)) AS lifters
FROM `everlog-prod.analytics_173607556.events_*`
WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20250101' AND _TABLE_SUFFIX NOT LIKE 'intraday%'
  AND event_name IN ('workout_completed', 'workout_service_workout_completed')
  AND user_pseudo_id NOT IN (SELECT device FROM excluded)
  AND IFNULL(app_info.version, '') NOT LIKE '%nightly%'
GROUP BY week
ORDER BY week
