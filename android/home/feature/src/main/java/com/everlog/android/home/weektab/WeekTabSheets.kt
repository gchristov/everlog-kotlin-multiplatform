package com.everlog.android.home.weektab

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppBottomSheet
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.home.R

@Composable
internal fun WeekTabSheets(prompt: WeekTabPrompt?, onSkipConfirmed: () -> Unit, onDismiss: () -> Unit) {
    when (prompt) {
        WeekTabPrompt.SkipDay -> AppBottomSheet(onDismissRequest = onDismiss, title = stringResource(DesignR.string.home_week_plan_skip)) {
            AppText(text = stringResource(R.string.home_week_plan_skip_prompt), color = Theme.contentColors.secondary)
            AppButton(
                modifier = Modifier.fillMaxWidth().testTag(WeekTabTags.SkipConfirm),
                onClick = { dismiss(onSkipConfirmed) },
                text = stringResource(DesignR.string.home_week_plan_skip),
            )
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { dismiss() },
                text = stringResource(DesignR.string.cancel),
            )
        }
        WeekTabPrompt.RestDay -> AppBottomSheet(onDismissRequest = onDismiss, title = stringResource(DesignR.string.plan_details_rest_title)) {
            AppText(text = stringResource(DesignR.string.plan_details_rest_prompt), color = Theme.contentColors.secondary)
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { dismiss() },
                text = stringResource(DesignR.string.ok),
            )
        }
        null -> Unit
    }
}
