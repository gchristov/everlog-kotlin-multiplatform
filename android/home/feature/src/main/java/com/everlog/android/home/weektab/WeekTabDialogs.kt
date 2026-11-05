package com.everlog.android.home.weektab

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppDialog
import com.everlog.android.common.compose.elements.AppDialogAction
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.home.R

@Composable
internal fun WeekTabDialogs(prompt: WeekTabPrompt?, onSkipConfirmed: () -> Unit, onDismiss: () -> Unit) {
    when (prompt) {
        WeekTabPrompt.SkipDay -> AppDialog(
            title = stringResource(DesignR.string.home_week_plan_skip),
            body = stringResource(R.string.home_week_plan_skip_prompt),
            onDismissRequest = onDismiss,
            primaryAction = AppDialogAction(stringResource(DesignR.string.home_week_plan_skip), onSkipConfirmed),
            secondaryAction = AppDialogAction(stringResource(DesignR.string.cancel), onDismiss),
        )
        WeekTabPrompt.RestDay -> AppDialog(
            title = stringResource(DesignR.string.plan_details_rest_title),
            body = stringResource(DesignR.string.plan_details_rest_prompt),
            onDismissRequest = onDismiss,
            primaryAction = AppDialogAction(stringResource(DesignR.string.ok), onDismiss),
        )
        null -> Unit
    }
}
