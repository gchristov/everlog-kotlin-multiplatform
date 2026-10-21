package com.everlog.ui.design.elements.keypad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.LocalAppScreenHazeState
import com.everlog.ui.design.theme.Theme
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * What the open keypad types into: its [label] and [title] over the keys (e.g. SET 2 OF 4 · KG over the exercise),
 * and the wide key that moves on from it ([mainKey], e.g. Next or Complete set).
 *
 * @param decimal Whether "." can be typed, e.g. for a weight
 * @param toggle Options for what's typed, beside the title, e.g. Reps or Time
 */
data class AppKeypadEntry(
    val label: String,
    val title: String,
    val mainKey: AppKeypadAction,
    val decimal: Boolean = false,
    val toggle: AppKeypadToggle? = null,
)

data class AppKeypadAction(
    val text: String,
    val onClick: () -> Unit,
)

/**
 * One of [options] picked, as segments. Only the keypad has it for now.
 */
data class AppKeypadToggle(
    val options: List<String>,
    val selected: Int,
    val onSelect: (Int) -> Unit,
)

/**
 * The keys typed. The keypad keeps no text of its own: the caller turns the keys into a value and shows it where
 * it's typed into.
 */
interface AppKeypadInput {
    // 0 to 9
    fun onDigit(digit: Char)

    fun onDecimal()

    fun onBackspace()

    // − is -1, + is 1
    fun onStep(direction: Int)
}

/**
 * A number keypad in a card, blurring what's behind it like [com.everlog.ui.design.elements.AppFooter], so it can be
 * docked over the bottom of a screen's content (place it in AppScreen's footer with the screen's margins). Closed,
 * it's a row of controls: a keyboard button to open it, small pill [actions] and a [primaryAction]. Open ([entry]
 * not null), the keys grow out of it with the entry's title over them and a ⌄ to close it again. [strips] sit
 * above either, e.g. [AppKeypadTimer] and [AppKeypadNote].
 *
 * @param onShow The keyboard button, while closed. None when null.
 * @param onHide The ⌄ beside the title, while open
 */
@Composable
fun AppKeypad(
    entry: AppKeypadEntry?,
    input: AppKeypadInput,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    onShow: (() -> Unit)? = null,
    actions: List<AppKeypadAction> = emptyList(),
    primaryAction: AppKeypadAction? = null,
    strips: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(KeypadSizes.Corner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .keypadBlur()
            .border(1.dp, Theme.backgrounds.separator, shape)
            // Takes the touches on its empty space, so they don't reach the content behind it
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(KeypadSizes.Padding),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        // The last entry, so the keys keep their contents while they close
        var shown by remember { mutableStateOf(entry) }
        if (entry != null) shown = entry
        AnimatedVisibility(
            visible = entry != null,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            shown?.let { EntryHeader(it, onHide) }
        }
        strips()
        AnimatedVisibility(
            visible = entry != null,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            shown?.let { Keys(it, input) }
        }
        AnimatedVisibility(
            visible = entry == null,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Controls(onShow, actions, primaryAction)
        }
    }
}

// A small button in the keypad's inset: Add exercise in the closed row, a timer's ±10s in a strip
@Composable
internal fun KeypadPill(
    text: String,
    onClick: () -> Unit,
    height: Dp = KeypadSizes.Chip,
) {
    Box(
        modifier = Modifier
            .height(height)
            .clip(Theme.shapes.button)
            .background(KeypadColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.button, maxLines = 1)
    }
}

// The keypad's own colours and sizes, which only it uses for now. Its keys, toggle, pills and strips are all
// fully rounded, like the app's buttons.
internal object KeypadColors {
    // The keys, toggle and pills sit darker than the card's tint
    val Inset = Color.Black.copy(alpha = 0.32f)
}

internal object KeypadSizes {
    val Corner = 30.dp
    val Padding = 14.dp
    val Key = 48.dp
    val Gap = 6.dp
    val StripHeight = 50.dp
    // The circle buttons and the toggle: as tall as AppButton
    val Button = 42.dp
    val Chip = 36.dp
    val ToggleInset = 3.dp
    val KeyIcon = 22.dp
}

// ⌄, what's being typed into, and its toggle
@Composable
private fun EntryHeader(entry: AppKeypadEntry, onHide: () -> Unit) {
    Row(
        modifier = Modifier.padding(bottom = Theme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        CircleButton(
            icon = ImageVector.vectorResource(R.drawable.ic_keyboard_down),
            contentDescription = stringResource(R.string.keypad_hide),
            onClick = onHide,
        )
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = entry.label, style = Theme.typography.small, color = Theme.contentColors.secondary, maxLines = 1)
            AppText(text = entry.title, style = Theme.typography.bodyBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        entry.toggle?.let { Toggle(it) }
    }
}

// The keyboard, the pills, and the main action at the end
@Composable
private fun Controls(onShow: (() -> Unit)?, actions: List<AppKeypadAction>, primaryAction: AppKeypadAction?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        onShow?.let {
            CircleButton(
                icon = ImageVector.vectorResource(R.drawable.ic_keyboard),
                contentDescription = stringResource(R.string.keypad_show),
                onClick = it,
            )
        }
        actions.forEach { KeypadPill(text = it.text, onClick = it.onClick, height = KeypadSizes.Button) }
        Spacer(modifier = Modifier.weight(1f))
        primaryAction?.let { AppButton(onClick = it.onClick, text = it.text) }
    }
}

@Composable
private fun CircleButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(KeypadSizes.Button)
            .clip(CircleShape)
            .background(KeypadColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AppIconButton(onClick = onClick, icon = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun Toggle(toggle: AppKeypadToggle) {
    val shape = Theme.shapes.button
    Row(
        modifier = Modifier
            .height(KeypadSizes.Button)
            .clip(shape)
            .background(KeypadColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, shape)
            .padding(KeypadSizes.ToggleInset),
    ) {
        toggle.options.forEachIndexed { index, option ->
            val selected = index == toggle.selected
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(shape)
                    .background(if (selected) Theme.contentColors.action else Color.Transparent)
                    .clickable { toggle.onSelect(index) }
                    .padding(horizontal = Theme.spacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = option,
                    style = Theme.typography.button,
                    color = if (selected) Theme.contentColors.onAction else Theme.contentColors.secondary,
                )
            }
        }
    }
}

// 1 2 3 −, 4 5 6 +, 7 8 9 ⌫, . 0 and the wide main key
@Composable
private fun Keys(entry: AppKeypadEntry, input: AppKeypadInput) {
    Column(verticalArrangement = Arrangement.spacedBy(KeypadSizes.Gap)) {
        KeyRow {
            DigitKey('1', input)
            DigitKey('2', input)
            DigitKey('3', input)
            Key(text = "−", onClick = { input.onStep(-1) })
        }
        KeyRow {
            DigitKey('4', input)
            DigitKey('5', input)
            DigitKey('6', input)
            Key(text = "+", onClick = { input.onStep(1) })
        }
        KeyRow {
            DigitKey('7', input)
            DigitKey('8', input)
            DigitKey('9', input)
            Key(
                icon = ImageVector.vectorResource(R.drawable.ic_backspace),
                contentDescription = stringResource(R.string.keypad_backspace),
                onClick = input::onBackspace,
            )
        }
        KeyRow {
            Key(text = ".", enabled = entry.decimal, onClick = input::onDecimal)
            DigitKey('0', input)
            // Keeps the row's gaps the same as the rows above, so "." and "0" line up with their columns
            Spacer(modifier = Modifier.width(0.dp))
            Key(text = entry.mainKey.text, primary = true, weight = 2f, onClick = entry.mainKey.onClick)
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KeypadSizes.Gap), content = content)
}

@Composable
private fun RowScope.DigitKey(digit: Char, input: AppKeypadInput) {
    Key(text = "$digit", onClick = { input.onDigit(digit) })
}

// A key in the keypad's darker inset; the main one filled
@Composable
private fun RowScope.Key(
    onClick: () -> Unit,
    text: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    weight: Float = 1f,
) {
    val shape = Theme.shapes.button
    val content = if (primary) Theme.contentColors.onAction else Theme.contentColors.primary
    val gap = with(LocalDensity.current) { KeypadSizes.Gap.roundToPx() }
    Box(
        modifier = Modifier
            .weight(weight)
            // A key two columns wide also covers the gap between them (after the zero-width spacer)
            .then(
                if (weight > 1f) {
                    Modifier.layout { measurable, constraints ->
                        val width = constraints.maxWidth + gap
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(constraints.maxWidth, placeable.height) { placeable.place(-gap, 0) }
                    }
                } else {
                    Modifier
                },
            )
            .height(KeypadSizes.Key)
            .clip(shape)
            .background(if (primary) Theme.contentColors.action else KeypadColors.Inset)
            .border(1.dp, if (primary) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val color = if (enabled) content else content.copy(alpha = 0.3f)
        when {
            icon != null -> AppIcon(modifier = Modifier.size(KeypadSizes.KeyIcon), imageVector = icon, tint = color, contentDescription = contentDescription)
            text != null && text.length == 1 -> AppText(text = text, style = keypadNumberStyle(), color = color)
            text != null -> AppText(text = text, style = Theme.typography.button, color = color)
        }
    }
}

// Figures the same width, so numbers don't jiggle as they change
@Composable
internal fun keypadNumberStyle() = Theme.typography.heading.copy(fontFeatureSettings = "tnum")

// The same blur and tint as AppFooter
@Composable
private fun Modifier.keypadBlur(): Modifier {
    val hazeState = LocalAppScreenHazeState.current
    return if (hazeState != null) {
        hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Theme.backgrounds.primary,
                tint = HazeTint(Theme.backgrounds.blurOverlay),
                blurRadius = 20.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(Theme.backgrounds.primary),
            ),
        )
    } else {
        background(Theme.backgrounds.primary)
    }
}

private object NoInput : AppKeypadInput {
    override fun onDigit(digit: Char) = Unit
    override fun onDecimal() = Unit
    override fun onBackspace() = Unit
    override fun onStep(direction: Int) = Unit
}

@Preview
@Composable
private fun AppKeypadPreview() {
    Theme {
        AppKeypad(
            entry = AppKeypadEntry(
                label = "SET 2 OF 4 · KG",
                title = "Bench Press",
                mainKey = AppKeypadAction("Complete set") {},
                decimal = true,
                toggle = AppKeypadToggle(listOf("Reps", "Time"), selected = 0) {},
            ),
            input = NoInput,
            onHide = {},
        )
    }
}

@Preview
@Composable
private fun AppKeypadClosedPreview() {
    Theme {
        AppKeypad(
            entry = null,
            input = NoInput,
            onHide = {},
            onShow = {},
            actions = listOf(AppKeypadAction("Add exercise") {}),
            primaryAction = AppKeypadAction("Complete set") {},
        )
    }
}
