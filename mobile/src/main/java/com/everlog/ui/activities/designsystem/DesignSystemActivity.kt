package com.everlog.ui.activities.designsystem

import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers

// Debug-only showcase of the Compose design system, opened from Settings.
class DesignSystemActivity : CommonComposeActivity() {
    private val viewModel by viewModels<DesignSystemViewModel> {
        createViewModelFactory { DesignSystemViewModel(dispatcher = Dispatchers.Main) }
    }

    @Composable
    override fun Content() = DesignSystemScreen(viewModel = viewModel)
}

@Composable
internal fun DesignSystemScreen(viewModel: DesignSystemViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DesignSystemState(
        state = state,
        onButtonClick = viewModel::onButtonClick
    )
}

@Composable
private fun DesignSystemState(
    state: DesignSystemViewModel.State,
    onButtonClick: () -> Unit
) {
    AppScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(text = stringResource(R.string.design_system_typography))
            TypographyExamples()
            SectionHeader(text = stringResource(R.string.design_system_buttons))
            Buttons(
                buttonClicks = state.buttonClicks,
                onButtonClick = onButtonClick
            )
            SectionHeader(text = stringResource(R.string.design_system_card))
            Card(onButtonClick = onButtonClick)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    AppText(
        text = text,
        style = Theme.typography.subtitle,
        color = Theme.contentColors.secondary,
    )
}

@Composable
private fun TypographyExamples() {
    val styles = listOf(
        "title" to Theme.typography.title,
        "heading" to Theme.typography.heading,
        "subtitle" to Theme.typography.subtitle,
        "body" to Theme.typography.body,
        "bodyBold" to Theme.typography.bodyBold,
        "caption" to Theme.typography.caption,
        "small" to Theme.typography.small,
        "button" to Theme.typography.button,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        styles.forEach { (name, style) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                    text = name,
                    style = style,
                )
                AppText(
                    modifier = Modifier.alignByBaseline(),
                    text = style.description(),
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                )
            }
        }
    }
}

private fun TextStyle.description(): String {
    val weight = when (fontWeight) {
        FontWeight.Medium -> "Medium"
        FontWeight.Bold -> "Bold"
        else -> "Regular"
    }
    return "${fontSize.value.toInt()}sp · $weight"
}

@Composable
private fun Buttons(
    buttonClicks: Int,
    onButtonClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(
            text = pluralStringResource(R.plurals.design_system_button_clicks, buttonClicks, buttonClicks),
            style = Theme.typography.caption,
            color = Theme.contentColors.secondary,
        )
        ButtonRow(
            enabled = true,
            onButtonClick = onButtonClick
        )
        ButtonRow(
            enabled = false,
            onButtonClick = onButtonClick
        )
    }
}

@Composable
private fun Card(onButtonClick: () -> Unit) {
    AppSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(
                    text = stringResource(R.string.design_system_card_title),
                    style = Theme.typography.heading,
                )
                AppText(
                    text = stringResource(R.string.design_system_card_body),
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            }
            ButtonRow(
                enabled = true,
                onButtonClick = onButtonClick
            )
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onButtonClick,
                text = stringResource(R.string.design_system_full_width),
            )
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onButtonClick,
                text = stringResource(R.string.design_system_full_width),
            )
        }
    }
}

@Composable
private fun ButtonRow(
    enabled: Boolean,
    onButtonClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(
            onClick = onButtonClick,
            text = stringResource(if (enabled) R.string.design_system_primary else R.string.design_system_disabled),
            enabled = enabled,
        )
        AppSecondaryButton(
            onClick = onButtonClick,
            text = stringResource(if (enabled) R.string.design_system_secondary else R.string.design_system_disabled),
            enabled = enabled,
        )
    }
}

@Preview
@Composable
private fun DesignSystemScreenPreview() {
    Theme {
        DesignSystemState(
            state = DesignSystemViewModel.State(),
            onButtonClick = {}
        )
    }
}
