-- Each new user's steps through the app, in order: screens in [brackets] and actions, with
-- repeats collapsed (xN) and later days marked (D1:, D7:, ...). New users are real installs that
-- signed up in the period, or never got an account.
-- Useful while new users are few: read each person's steps instead of percentages. Change the
-- start date to the period you're looking at.
WITH ev AS (
  SELECT user_pseudo_id AS device, user_id, event_name, TIMESTAMP_MICROS(event_timestamp) AS ts,
    app_info.version AS version, app_info.install_source AS source, geo.country AS country,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'firebase_screen') AS screen
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20260101'
),
installs AS (
  SELECT device, MIN(ts) AS installed, ANY_VALUE(country) AS country
  FROM ev
  WHERE event_name = 'first_open'
    AND IFNULL(source, '') != 'manual_install'
    AND IFNULL(country, '') != ''
    AND IFNULL(version, '') NOT LIKE '%nightly%'
  GROUP BY device
  HAVING device NOT IN (SELECT device FROM ev WHERE user_id IN ('V23IEC4EeqadnnbeqUGWZEgFXmC2', 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2'))
),
new_users AS (
  SELECT device, installed, country FROM installs
  WHERE device IN (SELECT device FROM ev WHERE event_name = 'user_registered')
     OR device NOT IN (SELECT device FROM ev WHERE user_id IS NOT NULL)
),
steps AS (
  SELECT n.device, n.installed, n.country, e.ts,
    DATE_DIFF(DATE(e.ts), DATE(n.installed), DAY) AS day,
    IF(e.event_name = 'screen_view', CONCAT('[', REPLACE(IFNULL(e.screen, '?'), 'screen_', ''), ']'),
       REPLACE(REPLACE(e.event_name, 'workout_', 'w_'), 'exercise_', 'ex_')) AS step
  FROM new_users n JOIN ev e USING (device)
  WHERE e.event_name NOT IN ('user_engagement', 'session_start', 'remote_config_fetched', 'first_open',
                             'notification_home_shown', 'user_identify', 'app_update', 'os_update', 'app_clear_data', 'firebase_campaign')
    AND NOT (e.event_name = 'screen_view' AND e.screen IS NULL)
),
marked AS (
  SELECT *, IF(step = LAG(step) OVER w AND day = LAG(day) OVER w, 0, 1) AS new_run
  FROM steps WINDOW w AS (PARTITION BY device ORDER BY ts)
),
runs AS (
  SELECT *, SUM(new_run) OVER (PARTITION BY device ORDER BY ts) AS run FROM marked
),
collapsed AS (
  SELECT device, installed, country, run, MIN(ts) AS ts, ANY_VALUE(day) AS day, ANY_VALUE(step) AS step, COUNT(*) AS n
  FROM runs GROUP BY device, installed, country, run
)
SELECT FORMAT_TIMESTAMP('%F', installed) AS installed, country,
  STRING_AGG(CONCAT(IF(day > 0, CONCAT('D', CAST(day AS STRING), ':'), ''), step, IF(n > 1, CONCAT('x', CAST(n AS STRING)), '')), ' > ' ORDER BY ts) AS path
FROM collapsed GROUP BY device, installed, country ORDER BY installed;
