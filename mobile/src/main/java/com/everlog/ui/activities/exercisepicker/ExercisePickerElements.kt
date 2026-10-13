package com.everlog.ui.activities.exercisepicker

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.everlog.R
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// The picker's own pieces, not in the design system (yet): the search bar with filter tokens, the
// chips under it, the menu that opens from the bar, and the rows' thumbnail and check.

private object PickerSizes {
    val BarMinHeight = 48.dp
    val TokenHeight = 30.dp
    val TokenCorner = 8.dp
    val TokenValueMaxWidth = 180.dp
    val ChipHeight = 36.dp
    val ChipIcon = 16.dp
    val InputMinWidth = 80.dp
    val MenuMaxHeight = 480.dp
    val Thumbnail = 48.dp
    val ThumbnailCorner = 12.dp
    val Check = 24.dp
    val CheckIcon = 14.dp
    val Box = 22.dp
    val BoxCorner = 6.dp
}

// The search bar: the filters as tokens, then the text field
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PickerSearchBar(
    tokens: List<FilterToken>,
    library: PickerLibrary?,
    text: String,
    placeholder: String,
    // Off while the library loads
    enabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onTextChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    onEditToken: (FilterKey) -> Unit,
    onToggleOperator: (FilterKey) -> Unit,
    onRemoveToken: (FilterKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PickerSizes.BarMinHeight)
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.surface)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { focusRequester.requestFocus() }
            .padding(horizontal = Theme.spacing.large, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIcon(
            modifier = Modifier.padding(top = 3.dp).size(24.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_search_white),
            tint = Theme.contentColors.secondary,
        )
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (library != null) tokens.forEach { token ->
                FilterTokenView(
                    token = token,
                    library = library,
                    onEdit = { onEditToken(token.key) },
                    onToggleOperator = { onToggleOperator(token.key) },
                    onRemove = { onRemoveToken(token.key) },
                )
            }
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = PickerSizes.InputMinWidth)
                    .height(PickerSizes.TokenHeight)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onFocused() },
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                singleLine = true,
                textStyle = Theme.typography.body.copy(color = Theme.contentColors.primary),
                cursorBrush = SolidColor(Theme.contentColors.action),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            AppText(text = placeholder, color = Theme.contentColors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        field()
                    }
                },
            )
        }
    }
}

// "Equipment is Barbell, Dumbbell ×": each part is tappable on its own
@Composable
private fun FilterTokenView(
    token: FilterToken,
    library: PickerLibrary,
    onEdit: () -> Unit,
    onToggleOperator: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(PickerSizes.TokenHeight)
            .clip(RoundedCornerShape(PickerSizes.TokenCorner))
            .background(Theme.backgrounds.surfaceRaised),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenPart(text = stringResource(token.key.label), color = Theme.contentColors.secondary, onClick = onEdit, start = Theme.spacing.small)
        TokenPart(
            text = stringResource(if (token.not) R.string.exercise_picker_is_not else R.string.exercise_picker_is),
            color = if (token.not) Theme.contentColors.action else Theme.contentColors.secondary,
            onClick = onToggleOperator,
        )
        TokenPart(
            modifier = Modifier.widthIn(max = PickerSizes.TokenValueMaxWidth),
            text = token.values.sortedBy { library.valueOrder(token.key, it) }.joinToString(", ") { library.valueName(token.key, it) },
            color = Theme.contentColors.primary,
            onClick = onEdit,
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onRemove)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                modifier = Modifier.size(PickerSizes.CheckIcon),
                imageVector = ImageVector.vectorResource(R.drawable.ic_clear_white),
                contentDescription = stringResource(R.string.exercise_picker_remove_filter, stringResource(token.key.label)),
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
    start: androidx.compose.ui.unit.Dp = 4.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(start = start, end = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(modifier = modifier, text = text, color = color, style = Theme.typography.button, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Modifier.fillMaxHeight() = this.then(Modifier.height(PickerSizes.TokenHeight))

// The keys not in the bar yet, then Clear all when there's a filter
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
            PickerChip(
                text = stringResource(key.label),
                color = Theme.contentColors.primary,
                border = Theme.backgrounds.separator,
                background = Theme.backgrounds.surface,
                showChevron = true,
                onClick = { onOpenKey(key) },
            )
        }
        if (showClearAll) {
            PickerChip(
                text = stringResource(R.string.exercise_picker_clear_all),
                color = Theme.contentColors.action,
                border = Theme.contentColors.action,
                background = Color.Transparent,
                showChevron = false,
                onClick = onClearAll,
            )
        }
    }
}

@Composable
private fun PickerChip(
    text: String,
    color: Color,
    border: Color,
    background: Color,
    showChevron: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(PickerSizes.ChipHeight)
            .clip(Theme.shapes.button)
            .border(1.dp, border, Theme.shapes.button)
            .background(background)
            .clickable(onClick = onClick)
            .padding(start = Theme.spacing.large, end = if (showChevron) Theme.spacing.medium else Theme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        AppText(text = text, color = color, style = Theme.typography.button, maxLines = 1)
        if (showChevron) {
            AppIcon(
                modifier = Modifier.size(PickerSizes.ChipIcon),
                imageVector = ImageVector.vectorResource(R.drawable.ic_keyboard_down),
                tint = color,
            )
        }
    }
}

// The menu under the search bar
@Composable
internal fun PickerMenuCard(
    menu: MenuContent,
    library: PickerLibrary,
    onRecent: (List<FilterToken>) -> Unit,
    onOpenKey: (FilterKey) -> Unit,
    onToggleValue: (FilterKey, String) -> Unit,
    onSuggestion: (Suggestion) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The list scrolls within the space above the keyboard, behind Done, which stays pinned at the
    // bottom over a blur of it, like the screen's floating footer
    val hazeState = remember { HazeState() }
    var doneHeight by remember { mutableIntStateOf(0) }
    val separator = Theme.backgrounds.separator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = PickerSizes.MenuMaxHeight)
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.surface)
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.surface),
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
                            AppListItem(title = describe(search, library), onClick = { onRecent(search) })
                        }
                    }
                    MenuLabel(stringResource(R.string.exercise_picker_filter_by))
                    menu.keys.forEach { (key, hint) ->
                        AppListItem(
                            title = stringResource(key.label),
                            subtitle = hint,
                            onClick = { onOpenKey(key) },
                        )
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
                        trailing = { Count(menu.nameCount) },
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
                        tint = HazeTint(Theme.backgrounds.surface.copy(alpha = 0.6f)),
                        blurRadius = 20.dp,
                        noiseFactor = 0f,
                        fallbackTint = HazeTint(Theme.backgrounds.surface),
                    ),
                )
                // A hairline on top, like AppFooter's border; the sides and bottom are the menu's own edges
                .drawWithContent {
                    drawContent()
                    drawLine(separator, start = Offset.Zero, end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
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
internal fun describe(search: List<FilterToken>, library: PickerLibrary): String =
    search.map { token ->
        stringResource(token.key.label) + " " +
            stringResource(if (token.not) R.string.exercise_picker_is_not else R.string.exercise_picker_is) + " " +
            token.values.sortedBy { library.valueOrder(token.key, it) }.joinToString(", ") { library.valueName(token.key, it) }
    }.joinToString(" · ")

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

@Composable
private fun PickBox(picked: Boolean) {
    Box(
        modifier = Modifier
            .size(PickerSizes.Box)
            .clip(RoundedCornerShape(PickerSizes.BoxCorner))
            .border(2.dp, if (picked) Theme.contentColors.action else Theme.backgrounds.separator, RoundedCornerShape(PickerSizes.BoxCorner))
            .background(if (picked) Theme.contentColors.action else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (picked) {
            AppIcon(modifier = Modifier.size(PickerSizes.CheckIcon), imageVector = ImageVector.vectorResource(R.drawable.ic_check), tint = Theme.contentColors.onAction)
        }
    }
}

// Rows

@Composable
internal fun ExerciseThumbnail(imageUrl: String?) {
    val modifier = Modifier
        .size(PickerSizes.Thumbnail)
        .clip(RoundedCornerShape(PickerSizes.ThumbnailCorner))
    if (imageUrl != null) {
        AsyncImage(
            modifier = modifier.background(Color.White),
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.background(Theme.backgrounds.surfaceRaised), contentAlignment = Alignment.Center) {
            AppIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_barbell), tint = Theme.contentColors.secondary)
        }
    }
}

@Composable
internal fun PickCheck(picked: Boolean) {
    Box(
        modifier = Modifier
            .size(PickerSizes.Check)
            .clip(CircleShape)
            .border(2.dp, if (picked) Theme.contentColors.action else Theme.backgrounds.separator, CircleShape)
            .background(if (picked) Theme.contentColors.action else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (picked) {
            AppIcon(modifier = Modifier.size(PickerSizes.CheckIcon), imageVector = ImageVector.vectorResource(R.drawable.ic_check), tint = Theme.contentColors.onAction)
        }
    }
}

@Composable
internal fun ShowAllChevron(expanded: Boolean) {
    AppIcon(
        modifier = Modifier.rotate(if (expanded) 180f else 0f),
        imageVector = ImageVector.vectorResource(R.drawable.ic_keyboard_down),
        tint = Theme.contentColors.action,
    )
}

// Dims the list behind the menu; a tap closes the menu
@Composable
internal fun PickerScrim(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.backgrounds.primary.copy(alpha = 0.72f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    )
}

// Loading: the chips and a couple of groups in the list's layout, with a shimmer sweeping across
@Composable
internal fun PickerSkeleton() {
    val sweep by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "sweep",
    )
    val base = Theme.backgrounds.surfaceRaised
    val brush = Brush.linearGradient(
        colors = listOf(base, Theme.contentColors.secondary.copy(alpha = 0.2f).compositeOver(base), base),
        start = Offset(sweep * 1000f, 0f),
        end = Offset(sweep * 1000f + 600f, 600f),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        Row(modifier = Modifier.padding(horizontal = Theme.spacing.large), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            listOf(88.dp, 108.dp, 104.dp, 84.dp).forEach { width ->
                Box(modifier = Modifier.size(width = width, height = PickerSizes.ChipHeight).clip(Theme.shapes.button).background(brush))
            }
        }
        repeat(2) { group ->
            Column(modifier = Modifier.padding(horizontal = Theme.spacing.large), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                Box(modifier = Modifier.size(width = if (group == 0) 180.dp else 110.dp, height = 14.dp).clip(RoundedCornerShape(PickerSizes.BoxCorner)).background(brush))
                Column(modifier = Modifier.fillMaxWidth().clip(Theme.shapes.surface).background(Theme.backgrounds.surface)) {
                    repeat(if (group == 0) 4 else 3) { index ->
                        Row(
                            modifier = Modifier.padding(Theme.spacing.large),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
                        ) {
                            Box(modifier = Modifier.size(PickerSizes.Thumbnail).clip(RoundedCornerShape(PickerSizes.ThumbnailCorner)).background(brush))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                                Box(modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.55f).height(14.dp).clip(RoundedCornerShape(PickerSizes.BoxCorner)).background(brush))
                                Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(PickerSizes.BoxCorner)).background(brush))
                            }
                        }
                    }
                }
            }
        }
    }
}
