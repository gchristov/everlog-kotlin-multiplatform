package com.everlog.ui.activities.designsystem

import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppGroupScope
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppListItem
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
    AppScreen(
        topBar = {
            AppBar(
                title = stringResource(R.string.settings_design_system),
                showBack = true,
            )
        },
        bottomBar = {
            AppFooter(
                actions = listOf(
                    AppFooterAction(
                        text = stringResource(R.string.design_system_footer_primary),
                        onClick = onButtonClick,
                    ),
                    AppFooterAction(
                        text = stringResource(R.string.design_system_footer_secondary),
                        onClick = onButtonClick,
                        style = AppFooterAction.Style.Secondary,
                    ),
                )
            )
        },
    ) { contentPadding ->
        // Sections without rows are a header-only group followed by their content
        AppGroupedList(contentPadding = contentPadding) {
            group(key = "typography", header = { stringResource(R.string.design_system_typography) }) {}
            item(key = "typographyExamples") {
                TypographyExamples()
            }
            group(key = "icons", header = { stringResource(R.string.design_system_icons) }) {}
            item(key = "iconExamples") {
                IconExamples(onButtonClick = onButtonClick)
            }
            group(key = "buttons", header = { stringResource(R.string.design_system_buttons) }) {}
            item(key = "buttonExamples") {
                Buttons(
                    buttonClicks = state.buttonClicks,
                    onButtonClick = onButtonClick
                )
            }
            group(key = "list", header = { stringResource(R.string.design_system_list) }) {
                listRows(onButtonClick = onButtonClick)
            }
            group(key = "card", header = { stringResource(R.string.design_system_card) }) {}
            item(key = "cardExample") {
                Card(onButtonClick = onButtonClick)
            }
        }
    }
}

private fun AppGroupScope.listRows(onButtonClick: () -> Unit) {
    items(count = 3, key = { it }) { index ->
        when (index) {
            0 -> AppListItem(title = stringResource(R.string.design_system_list_title))
            1 -> AppListItem(
                title = stringResource(R.string.design_system_list_title_subtitle),
                subtitle = stringResource(R.string.design_system_list_subtitle),
            )
            else -> AppListItem(
                title = stringResource(R.string.design_system_list_tappable),
                subtitle = stringResource(R.string.design_system_list_tappable_subtitle),
                onClick = onButtonClick,
            )
        }
    }
}

@Composable
private fun IconExamples(onButtonClick: () -> Unit) {
    val icons = listOf(
        R.drawable.ic_back,
        R.drawable.ic_clear_white,
        R.drawable.ic_add,
        R.drawable.ic_remove_white,
        R.drawable.ic_edit,
        R.drawable.ic_delete,
        R.drawable.ic_share,
        R.drawable.ic_timer,
        R.drawable.ic_settings,
        R.drawable.ic_more,
    ).map { ImageVector.vectorResource(it) }
    val tints = listOf(
        Theme.contentColors.primary,
        Theme.contentColors.secondary,
        Theme.contentColors.action,
        Theme.contentColors.destructive,
    )
    val star = ImageVector.vectorResource(R.drawable.ic_star_filled)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // The app's vector drawables, in the default tint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            icons.forEach { icon ->
                AppIcon(imageVector = icon)
            }
        }
        // One icon in each content colour
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            tints.forEach { tint ->
                AppIcon(
                    imageVector = star,
                    tint = tint,
                )
            }
        }
        // Icon buttons, enabled and disabled
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIconButton(
                onClick = onButtonClick,
                icon = ImageVector.vectorResource(R.drawable.ic_add),
                tint = Theme.contentColors.action,
                contentDescription = stringResource(R.string.design_system_icon_button),
            )
            AppIconButton(
                onClick = onButtonClick,
                icon = ImageVector.vectorResource(R.drawable.ic_add),
                tint = Theme.contentColors.action,
                contentDescription = stringResource(R.string.design_system_icon_button),
                enabled = false,
            )
        }
    }
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
