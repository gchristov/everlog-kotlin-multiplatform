-- One row per workout start (all real users, 2020-2026, Android): outcome, duration, sets, notification use, start type
-- The per-workout table behind the Funnel baseline (2020–2026) in Notion.
WITH ev AS (
  SELECT user_pseudo_id AS device, user_id, event_name, TIMESTAMP_MICROS(event_timestamp) AS ts,
    SUBSTR(_TABLE_SUFFIX, 1, 4) AS yr,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'source') AS start_source
  FROM `everlog-prod.analytics_173607556.events_*`
  WHERE platform = 'ANDROID' AND _TABLE_SUFFIX NOT LIKE 'intraday%'
    AND (event_name IN ('workout_started','workout_quick_started','workout_from_routine_started','workout_completed',
      'workout_service_workout_completed','workout_stopped','workout_timer_started_rest','workout_timer_started_exercise')
      OR event_name LIKE 'workout_service_%')
),
robots AS (SELECT DISTINCT user_pseudo_id AS device FROM `everlog-prod.analytics_173607556.events_*` WHERE user_id = 'V23IEC4EeqadnnbeqUGWZEgFXmC2'),
e AS (SELECT * FROM ev WHERE device NOT IN (SELECT device FROM robots)),
starts AS (
  SELECT device, ANY_VALUE(user_id) AS user_id, ts AS started, yr, ANY_VALUE(start_source) src,
    LEAD(ts) OVER (PARTITION BY device ORDER BY ts) AS next_start
  FROM e WHERE event_name = 'workout_started' GROUP BY device, ts, yr
),
ends AS (
  SELECT s.device, s.started,
    MIN(IF(x.event_name IN ('workout_completed','workout_service_workout_completed') AND x.ts > s.started, x.ts, NULL)) AS completed,
    MIN(IF(x.event_name = 'workout_stopped' AND x.ts > s.started, x.ts, NULL)) AS stopped
  FROM starts s JOIN e x ON x.device = s.device AND x.ts > s.started AND x.ts <= COALESCE(s.next_start, TIMESTAMP_ADD(s.started, INTERVAL 6 HOUR))
  GROUP BY 1, 2
)
SELECT s.device, s.user_id, s.yr, s.started,
  COALESCE(s.src, IF(LOGICAL_OR(x.event_name='workout_quick_started' AND ABS(TIMESTAMP_DIFF(x.ts,s.started,SECOND))<=2),'quick',
                  IF(LOGICAL_OR(x.event_name='workout_from_routine_started' AND ABS(TIMESTAMP_DIFF(x.ts,s.started,SECOND))<=2),'routine','unknown'))) AS src,
  CASE WHEN en.completed IS NOT NULL AND (en.stopped IS NULL OR en.completed < en.stopped) THEN 'completed'
       WHEN en.stopped IS NOT NULL THEN 'discarded' ELSE 'left' END AS outcome,
  TIMESTAMP_DIFF(LEAST(COALESCE(en.completed, TIMESTAMP '2100-01-01'), COALESCE(en.stopped, TIMESTAMP '2100-01-01')), s.started, SECOND) / 60 AS minutes,
  COUNTIF(x.event_name = 'workout_timer_started_rest') AS rest_timers,
  COUNTIF(x.event_name LIKE 'workout_service_%' AND x.event_name != 'workout_service_workout_completed') AS notif_actions,
  COUNTIF(x.event_name = 'workout_service_workout_completed') AS notif_complete,
  COUNTIF(x.event_name = 'workout_timer_started_exercise') AS exercise_timers
FROM starts s LEFT JOIN ends en USING (device, started)
LEFT JOIN e x ON x.device = s.device AND x.ts >= TIMESTAMP_SUB(s.started, INTERVAL 3 SECOND)
  AND x.ts <= LEAST(COALESCE(en.completed, TIMESTAMP '2100-01-01'), COALESCE(en.stopped, TIMESTAMP '2100-01-01'), COALESCE(s.next_start, TIMESTAMP '2100-01-01'), TIMESTAMP_ADD(s.started, INTERVAL 6 HOUR))
GROUP BY s.device, s.user_id, s.yr, s.started, s.src, en.completed, en.stopped
