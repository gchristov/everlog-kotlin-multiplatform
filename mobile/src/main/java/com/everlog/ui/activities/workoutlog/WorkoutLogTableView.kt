package com.everlog.ui.activities.workoutlog

import com.everlog.ui.components.exercisetable.Countdown
import com.everlog.ui.components.exercisetable.TableView

internal fun WorkoutLogState.tableView(unitLabel: String) = TableView(
    unit = unit,
    unitLabel = unitLabel,
    focus = focus,
    selected = selected,
    typed = typed,
    timing = (timer as? WorkoutTimer.SetTimer)?.let { Countdown(it.ref, timerRemainingSeconds()) },
    armed = focus?.ref.takeIf { mainKey == KeypadMainKey.CompleteSet || mainKey == KeypadMainKey.StartTimer },
    startsTimer = ::startsTimer,
    lastTime = lastTime,
)
