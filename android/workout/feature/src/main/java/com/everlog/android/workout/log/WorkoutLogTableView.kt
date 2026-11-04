package com.everlog.android.workout.log

import com.everlog.android.workoutbuilder.ui.exercisetable.Countdown
import com.everlog.android.workoutbuilder.ui.exercisetable.TableView

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
    lastTimeLoading = historyLoading,
)
