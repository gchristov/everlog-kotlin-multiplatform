package com.everlog.application

import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.navigation.AppSettingsPrompt
import com.everlog.android.common.navigation.DesignSystem
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.ExerciseDetails
import com.everlog.android.common.navigation.ExerciseGroupsEditor
import com.everlog.android.common.navigation.ExerciseLibrary
import com.everlog.android.common.navigation.ExercisePicker
import com.everlog.android.common.navigation.ExerciseTab
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.Home
import com.everlog.android.common.navigation.Login
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.Onboarding
import com.everlog.android.common.navigation.PlanCongratulations
import com.everlog.android.common.navigation.NewPlan
import com.everlog.android.common.navigation.PlanDetails
import com.everlog.android.common.navigation.PlayStoreListing
import com.everlog.android.common.navigation.PrivacyPolicy
import com.everlog.android.common.navigation.ProBuy
import com.everlog.android.common.navigation.ProBuyOrManage
import com.everlog.android.common.navigation.ProCongratulations
import com.everlog.android.common.navigation.RatePrompt
import com.everlog.android.common.navigation.ResultDestination
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.SetTypePicker
import com.everlog.android.common.navigation.ShareApp
import com.everlog.android.common.navigation.ShareWorkout
import com.everlog.android.common.navigation.StartEmptyWorkout
import com.everlog.android.common.navigation.StartWorkout
import com.everlog.android.common.navigation.StartWorkoutConfirmation
import com.everlog.android.common.navigation.TemplateDetails
import com.everlog.android.common.navigation.TemplateEditor
import com.everlog.android.common.navigation.TemplatePicker
import com.everlog.android.common.navigation.Terms
import com.everlog.android.common.navigation.WebLink
import com.everlog.android.common.navigation.WorkoutDetails
import com.everlog.android.exercise.details.ExerciseDetailsActivity
import com.everlog.android.exercise.picker.ExercisePickerActivity
import com.everlog.android.plan.congratulate.CongratulateActivity
import com.everlog.android.plan.details.PlanDetailsActivity
import com.everlog.android.template.picker.RoutinePickerActivity
import com.everlog.android.template.settype.SetTypePickerActivity
import com.everlog.constants.ELConstants
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.apprate.RatePromptTrigger
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.activities.home.routine.create.templateEditorIntent
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

// Opens each destination through ELNavigator, which knows the screens' activities
@ContributesBinding(AppScope::class)
@Inject
class AppNavigator : Navigator {

    override fun open(context: Context, destination: Destination) {
        val navigator = ELNavigator(context)
        when (destination) {
            is ResultDestination<*> -> context.startActivity(intent(context, destination))
            Home -> navigator.openHome()
            Login -> navigator.openLogin()
            Onboarding -> navigator.openOnboarding()
            is Terms -> navigator.openWebView(ELConstants.URL_TERMS, destination.title)
            is PrivacyPolicy -> navigator.openWebView(ELConstants.URL_PRIVACY, destination.title)
            PlayStoreListing -> navigator.openPlayStoreAppDetails()
            is WebLink -> navigator.openUrl(destination.url)
            ShareApp -> navigator.shareApp()
            is ShareWorkout -> navigator.shareWorkout(destination.workout)
            is AppSettingsPrompt -> navigator.promptForAppSettings(destination.rationale)
            is RatePrompt -> navigator.requestRatePrompt(RatePromptTrigger.WorkoutCompleted(destination.workout), destination.delayMillis)
            is FeedbackForm -> navigator.openFeedbackForm(destination.source)
            SetSuggestions -> navigator.openMuscleGoal()
            DesignSystem -> navigator.openDesignSystem()
            ProBuy -> navigator.openProBuy()
            ProBuyOrManage -> navigator.openProBuyOrManage()
            ProCongratulations -> navigator.openCongratulate(CongratulateActivity.Type.PRO)
            is PlanDetails -> context.startActivity(PlanDetailsActivity.launchIntent(context, destination.planUuid))
            PlanCongratulations -> navigator.openCongratulate(CongratulateActivity.Type.PLAN_FINISH)
            NewPlan -> navigator.openEditPlan(null)
            ExerciseLibrary -> navigator.openExercises()
            is ExerciseDetails -> navigator.openExerciseDetails(
                ExerciseDetailsActivity.Companion.Properties().exercise(destination.exercise).type(
                    when (destination.tab) {
                        ExerciseTab.Info -> ExerciseDetailsActivity.Companion.Type.INFO
                        ExerciseTab.History -> ExerciseDetailsActivity.Companion.Type.HISTORY
                    },
                ),
            )
            is TemplateDetails -> navigator.openRoutineDetails(destination.routine, destination.viewOnly)
            is StartWorkout -> navigator.startWorkout(destination.routine, true, destination.fromPlan)
            StartEmptyWorkout -> navigator.startWorkout(ELRoutine.buildEmptyWorkout(), false, false)
            is StartWorkoutConfirmation -> navigator.openPerformRoutineConfirmation(destination.routine, destination.fromPlan)
            is WorkoutDetails -> navigator.openWorkoutDetails(destination.workout, destination.justFinished, destination.fromPlan)
        }
    }

    override fun canOpen(destination: Destination): Boolean = when (destination) {
        is FeedbackForm -> RemoteConfigManager.manager.feedbackFormUrl(destination.source) != null
        else -> true
    }

    override fun intent(context: Context, destination: ResultDestination<*>): Intent = when (destination) {
        ExercisePicker -> ExercisePickerActivity.launchIntent(context, true)
        is SetTypePicker -> Intent(context, SetTypePickerActivity::class.java)
            .putExtra(ELConstants.EXTRA_NUMBER_OF_ITEMS, destination.exerciseCount)
        TemplatePicker -> Intent(context, RoutinePickerActivity::class.java)
        is TemplateEditor -> templateEditorIntent(
            context,
            CreateRoutineActivity.Companion.Properties().routine(destination.routine).showDetailsOnSuccess(destination.showDetailsOnSuccess),
        )
        is ExerciseGroupsEditor -> DefaultCreateExerciseGroupsActivity.launchIntent(
            context,
            DefaultCreateExerciseGroupsActivity.Companion.Properties().groups(destination.groups),
        )
    }
}
