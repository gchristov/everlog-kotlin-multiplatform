-- People who finished at least one workout per month (Android, since 2025-01-01): the size of
-- the loyal core. Leaves out robots and the owner's devices.
WITH excluded AS (
  SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*`
  WHERE user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
)
SELECT
  SUBSTR(event_date, 1, 4) || '-' || SUBSTR(event_date, 5, 2) AS month,
  COUNT(DISTINCT COALESCE(user_id, user_pseudo_id)) AS lifters,
  COUNT(*) AS workouts_completed
FROM `everlog-prod.analytics_173607556.events_*`
WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20250101' AND _TABLE_SUFFIX NOT LIKE 'intraday%'
  AND event_name IN ('workout_completed', 'workout_service_workout_completed')
  AND user_pseudo_id NOT IN (SELECT device FROM excluded)
  AND IFNULL(app_info.version, '') NOT LIKE '%nightly%'
GROUP BY month
ORDER BY month
