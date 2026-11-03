package com.everlog.android.workout

import android.content.Context
import com.everlog.android.workout.details.WorkoutTextField
import java.util.Date
import rx.Observable

// The app's pickers for editing a finished workout
interface WorkoutDialogs {
    fun showTextPrompt(context: Context, value: String?, field: WorkoutTextField): Observable<String>

    fun showDateTimePrompt(context: Context, date: Date): Observable<Date>
}
