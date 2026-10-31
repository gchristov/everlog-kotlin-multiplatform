package com.everlog.android.exercise.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.exercise.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.everlog.android.common.design.R as DesignR

// The picker's own pieces, around the design system's (AppSearchBar, AppChip, AppCheck,
// AppThumbnail): the filter tokens in the search bar, the menu under it, and the loading skeleton.

private object PickerSizes {
    val TokenHeight = 30.dp
    val TokenCorner = 8.dp
    val TokenValueMaxWidth = 180.dp
    val TokenIcon = 14.dp
    val TokenRemovePadding = 6.dp
    // The menu's outline and the line over its Done button
    val Hairline = 1.dp
    val MenuMaxHeight = 480.dp
    val MenuBlur = 20.dp
    val Box = 22.dp
    val BoxCorner = 6.dp
    val BoxRing = 2.dp
    val Tick = 14.dp
    val SkeletonThumbnail = 48.dp
    val SkeletonThumbnailCorner = 12.dp
    val SkeletonLineCorner = 6.dp
    val SkeletonChipHeight = 36.dp
    val SkeletonChipWidths = listOf(88.dp, 108.dp, 104.dp, 84.dp)
    val SkeletonHeaderWidths = listOf(180.dp, 110.dp)
    val SkeletonHeaderHeight = 14.dp
    val SkeletonTitleHeight = 14.dp
    val SkeletonSubtitleHeight = 10.dp
    const val MenuBlurTint = 0.6f
    const val ScrimAlpha = 0.72f
}

/**
 * "Equipment is Barbell, Dumbbell ×": the key and values open the menu on that key, "is" switches
 * to "is not", × removes it. TalkBack reads the whole token as one phrase.
 */
@Composable
internal fun FilterTokenView(
    token: FilterToken,
    index: PickerIndex,
    onEdit: () -> Unit,
    onToggleOperator: () -> Unit,
    onRemove: () -> Unit,
) {
    val key = stringResource(token.key.label)
    val operator = stringResource(if (token.not) R.string.exercise_picker_is_not else R.string.exercise_picker_is)
    val values = token.values.sortedBy { index.valueOrder(token.key, it) }.joinToString(", ") { index.valueName(token.key, it) }
    val phrase = "$key $operator $values"
    Row(
        modifier = Modifier
            .height(PickerSizes.TokenHeight)
            .clip(RoundedCornerShape(PickerSizes.TokenCorner))
            .background(Theme.backgrounds.surfaceRaised)
            .semantics(mergeDescendants = false) { contentDescription = phrase },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenPart(text = key, color = Theme.contentColors.secondary, onClick = onEdit, start = Theme.spacing.small)
        TokenPart(
            text = operator,
            color = if (token.not) Theme.contentColors.action else Theme.contentColors.secondary,
            onClick = onToggleOperator,
        )
        TokenPart(
            modifier = Modifier.widthIn(max = PickerSizes.TokenValueMaxWidth),
            text = values,
            color = Theme.contentColors.primary,
            onClick = onEdit,
        )
        Box(
            modifier = Modifier
                .height(PickerSizes.TokenHeight)
                .clickable(onClick = onRemove)
                .padding(horizontal = PickerSizes.TokenRemovePadding),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                modifier = Modifier.size(PickerSizes.TokenIcon),
                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_clear_white),
                contentDescription = stringResource(R.string.exercise_picker_remove_filter, key),
                tint = Theme.contentColors.secondary,
            )
        }
    }
}

@Composable
private fun TokenPart(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    start: Dp = Theme.spacing.extraSmall,
) {
    Box(
        modifier = Modifier
            .height(PickerSizes.TokenHeight)
            .clickable(onClick = onClick)
            .padding(start = start, end = Theme.spacing.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        AppText(modifier = modifier, text = text, color = color, style = Theme.typography.button, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// The keys not in the search bar yet, then Clear all when there's a filter
@Composable
internal fun PickerShortcuts(
    keys: List<FilterKey>,
    showClearAll: Boolean,
    onOpenKey: (FilterKey) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        keys.forEach { key ->
            AppChip(text = stringResource(key.label), onClick = { onOpenKey(key) }, dropdown = true)
        }
        if (showClearAll) {
            AppChip(text = stringResource(R.string.exercise_picker_clear_all), onClick = onClearAll, style = AppChipStyle.Action)
        }
    }
}

/**
 * The menu under the search bar. Its list scrolls within the space above the keyboard, behind Done,
 * which stays pinned at the bottom over a blur of it, like the screen's floating footer.
 */
@Composable
internal fun PickerMenuCard(
    menu: MenuContent,
    index: PickerIndex,
    onRecent: (List<FilterToken>) -> Unit,
    onOpenKey: (FilterKey) -> Unit,
    onToggleValue: (FilterKey, String) -> Unit,
    onSuggestion: (Suggestion) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = remember { HazeState() }
    var doneHeight by remember { mutableIntStateOf(0) }
    val separator = Theme.backgrounds.separator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = PickerSizes.MenuMaxHeight)
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.surface)
            .border(PickerSizes.Hairline, Theme.backgrounds.separator, Theme.shapes.surface),
    ) {
        Column(
            modifier = Modifier
                .hazeSource(hazeState)
                .verticalScroll(rememberScrollState())
                .padding(top = Theme.spacing.small, bottom = with(LocalDensity.current) { doneHeight.toDp() }),
        ) {
            when (menu) {
                is MenuContent.Start -> {
                    if (menu.recent.isNotEmpty()) {
                        MenuLabel(stringResource(R.string.exercise_picker_recent))
                        menu.recent.forEach { search ->
                            AppListItem(title = describe(search, index), onClick = { onRecent(search) })
                        }
                    }
                    MenuLabel(stringResource(R.string.exercise_picker_filter_by))
                    menu.keys.forEach { (key, hint) ->
                        AppListItem(title = stringResource(key.label), subtitle = hint, onClick = { onOpenKey(key) })
                    }
                }

                is MenuContent.Values -> {
                    val label = stringResource(menu.key.label)
                    MenuLabel(stringResource(if (menu.not) R.string.exercise_picker_values_not else R.string.exercise_picker_values, label))
                    menu.options.forEach { option ->
                        AppListItem(
                            title = option.value.name,
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                                    Count(option.count)
                                    PickBox(picked = option.picked)
                                }
                            },
                            onClick = { onToggleValue(menu.key, option.value.id) },
                        )
                    }
                }

                is MenuContent.Typing -> {
                    val keys = FilterKey.entries.map { it to stringResource(it.label) }
                        .filter { (_, name) -> searchWords(name).any { it.startsWith(menu.lastWord) } }
                    if (keys.isNotEmpty() || menu.suggestions.isNotEmpty()) {
                        MenuLabel(stringResource(R.string.exercise_picker_filters))
                    }
                    keys.forEach { (key, name) ->
                        AppListItem(title = name, onClick = { onOpenKey(key) })
                    }
                    menu.suggestions.forEach { suggestion ->
                        AppListItem(
                            header = stringResource(suggestion.key.label),
                            title = suggestion.value.name,
                            trailing = { Count(suggestion.count) },
                            onClick = { onSuggestion(suggestion) },
                        )
                    }
                    MenuLabel(stringResource(R.string.exercise_picker_names))
                    AppListItem(
                        title = stringResource(R.string.exercise_picker_names_containing, menu.text),
                        trailing = { Count(menu.resultCount) },
                        onClick = onDone,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { doneHeight = it.height }
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = Theme.backgrounds.surface,
                        tint = HazeTint(Theme.backgrounds.surface.copy(alpha = PickerSizes.MenuBlurTint)),
                        blurRadius = PickerSizes.MenuBlur,
                        noiseFactor = 0f,
                        fallbackTint = HazeTint(Theme.backgrounds.surface),
                    ),
                )
                // A hairline on top, like AppFooter's border; the sides and bottom are the menu's own edges
                .drawWithContent {
                    drawContent()
                    drawLine(separator, start = Offset.Zero, end = Offset(size.width, 0f), strokeWidth = PickerSizes.Hairline.toPx())
                }
                .padding(Theme.spacing.large),
        ) {
            AppSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDone,
                text = stringResource(R.string.exercise_picker_done),
            )
        }
    }
}

// "Muscle is Chest · Equipment is Barbell, Dumbbell"
@Composable
internal fun describe(search: List<FilterToken>, index: PickerIndex): String {
    val isText = stringResource(R.string.exercise_picker_is)
    val isNotText = stringResource(R.string.exercise_picker_is_not)
    return search.map { token ->
        stringResource(token.key.label) + " " + (if (token.not) isNotText else isText) + " " +
            token.values.sortedBy { index.valueOrder(token.key, it) }.joinToString(", ") { index.valueName(token.key, it) }
    }.joinToString(" · ")
}

@Composable
private fun MenuLabel(text: String) {
    AppText(
        modifier = Modifier.padding(start = Theme.spacing.large, end = Theme.spacing.large, top = Theme.spacing.medium, bottom = Theme.spacing.extraSmall),
        text = text,
        style = Theme.typography.caption,
        color = Theme.contentColors.secondary,
    )
}

@Composable
private fun Count(count: Int) {
    AppText(text = count.toString(), style = Theme.typography.small, color = Theme.contentColors.secondary)
}

// A menu value's checkbox: several can be picked, so it's square rather than AppCheck's circle
@Composable
private fun PickBox(picked: Boolean) {
    val shape = RoundedCornerShape(PickerSizes.BoxCorner)
    Box(
        modifier = Modifier
            .size(PickerSizes.Box)
            .clip(shape)
            .border(PickerSizes.BoxRing, if (picked) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .background(if (picked) Theme.contentColors.action else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (picked) {
            AppIcon(modifier = Modifier.size(PickerSizes.Tick), imageVector = ImageVector.vectorResource(DesignR.drawable.ic_check), tint = Theme.contentColors.onAction)
        }
    }
}

// Show all / Show fewer's arrow
@Composable
internal fun ShowAllChevron(expanded: Boolean) {
    AppIcon(
        modifier = Modifier.rotate(if (expanded) 180f else 0f),
        imageVector = ImageVector.vectorResource(DesignR.drawable.ic_keyboard_down),
        tint = Theme.contentColors.action,
    )
}

// Dims the list behind the menu; a tap closes the menu
@Composable
internal fun PickerScrim(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.backgrounds.primary.copy(alpha = PickerSizes.ScrimAlpha))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .clearAndSetSemantics {},
    )
}

// Loading: the chips and two groups of rows in the list's layout, shimmering
@Composable
internal fun PickerSkeleton() {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Theme.spacing.medium)
            .clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        Row(modifier = Modifier.padding(horizontal = Theme.spacing.large), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            PickerSizes.SkeletonChipWidths.forEach { width ->
                Box(modifier = Modifier.size(width = width, height = PickerSizes.SkeletonChipHeight).clip(Theme.shapes.button).background(brush))
            }
        }
        repeat(2) { group ->
            Column(modifier = Modifier.padding(horizontal = Theme.spacing.large), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                Box(modifier = Modifier.size(width = PickerSizes.SkeletonHeaderWidths[group], height = PickerSizes.SkeletonHeaderHeight).clip(RoundedCornerShape(PickerSizes.SkeletonLineCorner)).background(brush))
                Column(modifier = Modifier.fillMaxWidth().clip(Theme.shapes.surface).background(Theme.backgrounds.surface)) {
                    repeat(if (group == 0) 4 else 3) { row ->
                        Row(
                            modifier = Modifier.padding(Theme.spacing.large),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
                        ) {
                            Box(modifier = Modifier.size(PickerSizes.SkeletonThumbnail).clip(RoundedCornerShape(PickerSizes.SkeletonThumbnailCorner)).background(brush))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                                Box(modifier = Modifier.fillMaxWidth(if (row % 2 == 0) 0.7f else 0.55f).height(PickerSizes.SkeletonTitleHeight).clip(RoundedCornerShape(PickerSizes.SkeletonLineCorner)).background(brush))
                                Box(modifier = Modifier.fillMaxWidth(0.4f).height(PickerSizes.SkeletonSubtitleHeight).clip(RoundedCornerShape(PickerSizes.SkeletonLineCorner)).background(brush))
                            }
                        }
                    }
                }
            }
        }
    }
}
