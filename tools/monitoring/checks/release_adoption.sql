-- How each app version is doing over the last 7 days, for watching a release as it rolls out: devices on it, new
-- installs, workouts started and finished, workouts resumed after the app was killed, and handled exceptions.
-- Change the INTERVAL to look further back. Real users only, with the dashboard's filters (see the README's
-- "Real users only"). Counts only.
WITH excluded AS (
  SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND (
    user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
    OR app_info.version LIKE '%nightly%' OR app_info.version LIKE '%-local%'
    OR app_info.install_source = 'manual_install')
),
ev AS (
  SELECT user_pseudo_id AS device, event_name, app_info.version AS version
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID'
    AND _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
    AND IFNULL(geo.country, '') != ''
    AND user_pseudo_id NOT IN (SELECT device FROM excluded)
)
SELECT
  version,
  COUNT(DISTINCT device) AS devices,
  COUNT(DISTINCT IF(event_name = 'first_open', device, NULL)) AS installs,
  COUNTIF(event_name = 'workout_started') AS started,
  COUNTIF(event_name IN ('workout_completed', 'workout_service_workout_completed')) AS finished,
  COUNT(DISTINCT IF(event_name IN ('workout_completed', 'workout_service_workout_completed'), device, NULL)) AS lifters,
  COUNTIF(event_name = 'workout_resumed') AS resumed,
  COUNTIF(event_name = 'app_exception') AS exceptions
FROM ev
GROUP BY version
ORDER BY version DESC
