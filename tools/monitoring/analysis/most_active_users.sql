-- Most active users by workouts completed in the last 90 days. `user` is the Firebase Auth UID,
-- so a user's name and email are in Firestore at users/<uid>. Users who haven't logged in show
-- as anon:<device id>. Results identify people: save them only under tools/bigquery (gitignored).
SELECT
  COALESCE(user_id, CONCAT('anon:', user_pseudo_id)) AS user,
  COUNT(*) AS workouts_completed,
  COUNT(DISTINCT event_date) AS workout_days,
  FORMAT_TIMESTAMP('%F', TIMESTAMP_MICROS(MIN(event_timestamp))) AS first_workout,
  FORMAT_TIMESTAMP('%F', TIMESTAMP_MICROS(MAX(event_timestamp))) AS last_workout,
  MAX(app_info.version) AS latest_app_version,
  ANY_VALUE(geo.country) AS country
FROM `everlog-prod.analytics_173607556.events_*`
WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
  AND event_name IN ('workout_completed', 'workout_service_workout_completed')
  -- Exclude the Firebase Test Lab account used by Play pre-launch robots, and the owner
  AND IFNULL(user_id, '') NOT IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
GROUP BY user
ORDER BY workouts_completed DESC
LIMIT 50;
