package com.everlog.ui.activities.designsystem

import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppHeroHeader
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppTertiaryButton
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
    var showDialog by remember { mutableStateOf(false) }

    AppScreen(
        topBar = {
            AppBar(
                title = "Design system",
                showBack = true,
            )
        },
        footer = {
            AppFooter(
                actions = listOf(
                    AppFooterAction(
                        text = "Footer action",
                        onClick = onButtonClick,
                    ),
                    AppFooterAction(
                        text = "Secondary footer action",
                        onClick = onButtonClick,
                        style = AppFooterAction.Style.Secondary,
                    ),
                    AppFooterAction(
                        text = "Tertiary footer action",
                        onClick = onButtonClick,
                        style = AppFooterAction.Style.Tertiary,
                    ),
                )
            )
        },
    ) { contentPadding ->
        // Sections without rows are a header-only group followed by their content
        AppGroupedList(contentPadding = contentPadding) {
            group(key = "typography", header = { "Typography" }) {}
            item(key = "typographyExamples") {
                TypographyExamples()
            }
            group(key = "icons", header = { "Icons" }) {}
            item(key = "iconExamples") {
                IconExamples(onButtonClick = onButtonClick)
            }
            group(key = "buttons", header = { "Buttons" }) {}
            item(key = "buttonExamples") {
                Buttons(
                    buttonClicks = state.buttonClicks,
                    onButtonClick = onButtonClick
                )
            }
            group(
                key = "list",
                header = { "List" },
                footer = { "A footer note under a group's rows." },
            ) {
                listRows(onButtonClick = onButtonClick)
            }
            group(key = "card", header = { "Card" }) {}
            item(key = "cardExample") {
                Card(onButtonClick = onButtonClick)
            }
            group(key = "heroHeader", header = { "Hero header" }) {}
            item(key = "heroHeaderExample") {
                AppHeroHeader(
                    title = "Screen title",
                    body = "A line of body text under the title, at the top of a screen's content.",
                )
            }
            item(key = "heroHeaderImageExample") {
                AppHeroHeader(
                    // Out of the list's margins, as it would be on a screen
                    modifier = Modifier
                        .padding(top = Theme.spacing.extraLarge)
                        .fullBleed(Theme.spacing.large),
                    title = "Push day",
                    body = "With an image, here a plain colour",
                    image = ColorPainter(Theme.backgrounds.surfaceRaised),
                )
            }
            group(key = "dialog", header = { "Dialog" }) {}
            item(key = "dialogExample") {
                AppSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDialog = true },
                    text = "Show dialog",
                )
            }
        }
    }

    if (showDialog) {
        AppDialog(
            title = "Dialog title",
            body = "A line of body text. The primary action is the one we want the user to take.",
            onDismissRequest = { showDialog = false },
            primaryAction = AppDialogAction(text = "Primary", onClick = { showDialog = false }),
            secondaryAction = AppDialogAction(text = "Secondary", onClick = { showDialog = false }),
        )
    }
}

private fun AppGroupScope.listRows(onButtonClick: () -> Unit) {
    items(count = 5, key = { it }) { index ->
        when (index) {
            0 -> AppListItem(title = "Title only")
            1 -> AppListItem(
                title = "Title and subtitle",
                subtitle = "Like the rows in Settings",
            )
            2 -> AppListItem(
                title = "Tappable row",
                subtitle = "Counts as a button tap",
                onClick = onButtonClick,
            )
            3 -> AppListItem(
                header = "Header",
                title = "Title in the action colour",
                titleColor = Theme.contentColors.action,
                trailing = {
                    AppIcon(
                        modifier = Modifier.size(20.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
                        tint = Theme.contentColors.secondary,
                    )
                },
                onClick = onButtonClick,
            )
            else -> AppListItem(
                title = "Leading and trailing",
                leading = {
                    AppIcon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_timer),
                        tint = Theme.contentColors.secondary,
                    )
                },
                trailing = {
                    AppText(text = "18:00", style = Theme.typography.heading)
                },
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

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraLarge)) {
            tints.forEach { tint ->
                AppIcon(
                    imageVector = star,
                    tint = tint,
                )
            }
        }
        // Icon buttons, enabled and disabled
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            AppIconButton(
                onClick = onButtonClick,
                icon = ImageVector.vectorResource(R.drawable.ic_add),
                tint = Theme.contentColors.action,
                contentDescription = "Add",
            )
            AppIconButton(
                onClick = onButtonClick,
                icon = ImageVector.vectorResource(R.drawable.ic_add),
                tint = Theme.contentColors.action,
                contentDescription = "Add",
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
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        styles.forEach { (name, style) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        AppText(
            text = if (buttonClicks == 1) "Tapped 1 time" else "Tapped $buttonClicks times",
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
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                AppText(
                    text = "Card title",
                    style = Theme.typography.heading,
                )
                AppText(
                    text = "The card background sits on top of the screen background, like the cards in Settings.",
                    style = Theme.typography.body,
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
                text = "Full width",
            )
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onButtonClick,
                text = "Full width",
            )
        }
    }
}

@Composable
private fun ButtonRow(
    enabled: Boolean,
    onButtonClick: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppButton(
            onClick = onButtonClick,
            text = if (enabled) "Primary" else "Disabled",
            enabled = enabled,
        )
        AppSecondaryButton(
            onClick = onButtonClick,
            text = if (enabled) "Secondary" else "Disabled",
            enabled = enabled,
        )
        AppTertiaryButton(
            onClick = onButtonClick,
            text = if (enabled) "Tertiary" else "Disabled",
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

// Widens the content by margin on each side, so it reaches the screen edges from inside a padded list
private fun Modifier.fullBleed(margin: Dp) = layout { measurable, constraints ->
    val bleed = margin.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.maxWidth + bleed * 2,
            maxWidth = constraints.maxWidth + bleed * 2,
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-bleed, 0)
    }
}

