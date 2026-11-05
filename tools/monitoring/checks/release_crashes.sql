-- Crashes in the last 7 days by day, app version and issue, for watching a release as it rolls out. Change the
-- INTERVAL to look further back. From Crashlytics' realtime table, as the batch one lags about a day. Counts only.
-- Leaves out test builds (nightly, local), Play's robots (their accounts, the Test Lab key, OnePlus8Pro on
-- Android 11) and the owner's account, as the dashboard does. An issue groups by stack, so one issue can mix
-- messages: it's grouped by message too.
SELECT
  DATE(event_timestamp) AS day,
  application.display_version AS version,
  is_fatal,
  issue_title,
  SUBSTR((SELECT exception_message FROM UNNEST(exceptions) LIMIT 1), 1, 80) AS message,
  COUNT(*) AS events,
  COUNT(DISTINCT installation_uuid) AS devices
FROM `everlog-prod.firebase_crashlytics.com_everlog_ANDROID_REALTIME`
WHERE event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
  AND application.display_version NOT LIKE '%nightly%'
  AND application.display_version NOT LIKE '%local%'
  AND IFNULL(user.id, '') NOT IN (
    'V23IEC4EeqadnnbeqUGWZEgFXmC2', 'f8aOTWlqemh17KdPktGHFZiGBHl1', 'dUuB36KdrQaupzTq15zjeyDuKzZ2',
    'mjHRQjofoBWSnBOSAvBNE0PIS813', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2')
  AND NOT EXISTS (SELECT 1 FROM UNNEST(custom_keys) WHERE key = 'is_firebase_test_lab_run' AND value = 'true')
  AND NOT (device.model = 'OnePlus8Pro' AND operating_system.display_version LIKE '11%')
GROUP BY day, version, is_fatal, issue_title, message
ORDER BY day DESC, is_fatal DESC, events DESC
