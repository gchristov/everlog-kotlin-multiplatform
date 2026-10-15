-- Workouts finished by all real users (Android, since 2025-01-01), and how many people
-- finished them, per week (Monday) and per calendar month. The loyal core is mostly 2020-2022
-- accounts, so this counts everyone, not only new users.
-- Real users only: leaves out test devices (below), and any event from a device with no country.
-- Test devices: any device that ever used the Test Lab or owner account, or ever ran a nightly,
-- local or sideloaded build. Only the owner and Play's robots run those builds.
WITH excluded AS (
  SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND (
    user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
    OR app_info.version LIKE '%nightly%' OR app_info.version LIKE '%-local%'
    OR app_info.install_source = 'manual_install')
),
workouts AS (
  SELECT PARSE_DATE('%Y%m%d', event_date) AS day, COALESCE(user_id, user_pseudo_id) AS lifter
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20250101' AND _TABLE_SUFFIX NOT LIKE 'intraday%'
    AND event_name IN ('workout_completed', 'workout_service_workout_completed')
    AND user_pseudo_id NOT IN (SELECT device FROM excluded)
    AND IFNULL(geo.country, '') != ''
)
SELECT 'week' AS period, FORMAT_DATE('%F', DATE_TRUNC(day, WEEK(MONDAY))) AS start,
  COUNT(*) AS workouts_completed, COUNT(DISTINCT lifter) AS lifters
FROM workouts GROUP BY start
UNION ALL
SELECT 'month', FORMAT_DATE('%F', DATE_TRUNC(day, MONTH)),
  COUNT(*), COUNT(DISTINCT lifter)
FROM workouts GROUP BY 2
ORDER BY period, start
