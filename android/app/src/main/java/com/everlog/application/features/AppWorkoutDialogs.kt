package com.everlog.application.features

import android.content.Context
import com.everlog.android.workout.WorkoutDialogs
import com.everlog.android.workout.details.WorkoutTextField
import com.everlog.ui.dialog.AppDialogs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.Date
import rx.Observable

@ContributesBinding(AppScope::class)
@Inject
class AppWorkoutDialogs : WorkoutDialogs {
    override fun showTextPrompt(context: Context, value: String?, field: WorkoutTextField): Observable<String> =
        AppDialogs.showInputStringDialog(context, value, when (field) {
            WorkoutTextField.Name -> AppDialogs.StringDialogType.WORKOUT_NAME
            WorkoutTextField.Note -> AppDialogs.StringDialogType.WORKOUT_NOTE
        })

    override fun showDateTimePrompt(context: Context, date: Date): Observable<Date> = AppDialogs.showDateTimeDialog(context, date)
}
