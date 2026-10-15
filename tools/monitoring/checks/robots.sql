-- Installs per month and how many each robot filter removes (Android, since 2025-01-01).
-- A health check: if "real" jumps without a campaign, a new kind of robot is getting through.
-- A device can match several filters, so the columns don't add up.
WITH ev AS (
  SELECT user_pseudo_id, user_id, event_name, event_date, app_info.version AS v,
    app_info.install_source AS src, geo.country AS country
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX >= '20250101' AND _TABLE_SUFFIX NOT LIKE 'intraday%'
),
installs AS (
  SELECT user_pseudo_id, SUBSTR(MIN(event_date), 1, 4) || '-' || SUBSTR(MIN(event_date), 5, 2) AS month,
    ANY_VALUE(v) AS v, ANY_VALUE(src) AS src, ANY_VALUE(country) AS country
  FROM ev WHERE event_name = 'first_open' GROUP BY 1
),
testlab AS (
  SELECT DISTINCT user_pseudo_id FROM `everlog-prod.analytics_173607556.events_*`
  WHERE user_id = 'V23IEC4EeqadnnbeqUGWZEgFXmC2'
),
owner AS (
  SELECT DISTINCT user_pseudo_id FROM `everlog-prod.analytics_173607556.events_*`
  WHERE user_id = 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2'
),
-- Devices that ever ran a nightly or local build, even if they installed a Play build first
test_builds AS (
  SELECT DISTINCT user_pseudo_id FROM `everlog-prod.analytics_173607556.events_*`
  WHERE app_info.version LIKE '%nightly%' OR app_info.version LIKE '%-local%'
)
SELECT
  month,
  COUNT(*) AS installs,
  COUNTIF(t.user_pseudo_id IS NOT NULL) AS testlab_account,
  COUNTIF(src = 'manual_install') AS manual_install,
  COUNTIF(IFNULL(country, '') = '') AS no_country,
  COUNTIF(b.user_pseudo_id IS NOT NULL) AS nightly_or_local,
  COUNTIF(o.user_pseudo_id IS NOT NULL) AS owner,
  COUNTIF(t.user_pseudo_id IS NULL AND o.user_pseudo_id IS NULL AND b.user_pseudo_id IS NULL
    AND IFNULL(src, '') != 'manual_install' AND IFNULL(country, '') != '') AS real_installs
FROM installs i
LEFT JOIN testlab t USING (user_pseudo_id)
LEFT JOIN owner o USING (user_pseudo_id)
LEFT JOIN test_builds b USING (user_pseudo_id)
GROUP BY month
ORDER BY month
