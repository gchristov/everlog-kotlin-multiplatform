package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.R
import com.everlog.ui.design.elements.keypad.AppKeypad
import com.everlog.ui.design.elements.keypad.AppKeypadAction
import com.everlog.ui.design.elements.keypad.AppKeypadEntry
import com.everlog.ui.design.elements.keypad.AppKeypadInput
import com.everlog.ui.design.elements.keypad.AppKeypadNote
import com.everlog.ui.design.elements.keypad.AppKeypadTimer
import com.everlog.ui.design.elements.keypad.AppKeypadTimerStyle
import com.everlog.ui.design.elements.keypad.AppKeypadToggle
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.delay

// The keypad docked at the bottom, as the workout screen has it. Tap a value to type into it; Next moves to the
// other. The strips can be switched on to see them above the keys or the closed controls, and Empty shows it with
// nothing to log yet, where the main action fills the row.
@Composable
internal fun DesignSystemKeypad(onButtonClick: () -> Unit) {
    var focused by remember { mutableStateOf<Field?>(null) }
    // The first key after focusing replaces the value, the ones after it add to it
    var typing by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf(mapOf(Field.Weight to "62.5", Field.Count to "8")) }
    var timed by remember { mutableStateOf(false) }
    var rest by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(false) }
    var empty by remember { mutableStateOf(false) }
    var restLeft by remember { mutableIntStateOf(RestSeconds) }

    LaunchedEffect(rest) {
        restLeft = RestSeconds
        while (rest && restLeft > 0) {
            delay(1000)
            restLeft--
        }
        rest = false
    }

    val input = object : AppKeypadInput {
        fun edit(transform: (String) -> String) {
            val field = focused ?: return
            values = values + (field to transform(values.getValue(field)).take(6))
        }

        fun type(transform: (String) -> String) {
            edit { transform(if (typing) it else "") }
            typing = true
        }

        override fun onDigit(digit: Char) = type { it + digit }
        override fun onDecimal() = type { if (it.contains('.')) it else "$it." }
        override fun onBackspace() = edit { it.dropLast(1) }
        override fun onStep(direction: Int) = edit { ((it.toFloatOrNull() ?: 0f) + direction).coerceAtLeast(0f).let(::format) }
    }

    ShowcasePage(
        title = "Keypad",
        footer = {
            AppKeypad(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small),
                entry = focused?.let { field ->
                    AppKeypadEntry(
                        label = if (field == Field.Weight) "SET 2 OF 4 · KG" else if (timed) "SET 2 OF 4 · TIME" else "SET 2 OF 4 · REPS",
                        title = "Bench Press",
                        decimal = field == Field.Weight,
                        mainKey = if (field == Field.Count) {
                            AppKeypadAction("Next") {
                                focused = Field.Weight
                                typing = false
                            }
                        } else {
                            AppKeypadAction(if (timed) "Start timer" else "Complete set", icon = ImageVector.vectorResource(if (timed) R.drawable.ic_timer else R.drawable.ic_check)) {
                                onButtonClick()
                                focused = null
                            }
                        },
                        toggle = AppKeypadToggle(listOf("Reps", "Time"), selected = if (timed) 1 else 0) { timed = it == 1 },
                    )
                },
                input = input,
                onHide = { focused = null },
                onShow = if (empty) {
                    null
                } else {
                    {
                        focused = Field.Count
                        typing = false
                    }
                },
                actions = if (empty) emptyList() else listOf(AppKeypadAction("Add exercise", onClick = onButtonClick)),
                primaryAction = if (empty) {
                    AppKeypadAction("Add exercise", onClick = onButtonClick)
                } else {
                    AppKeypadAction("Complete set", icon = ImageVector.vectorResource(R.drawable.ic_check), onClick = onButtonClick)
                },
                strips = {
                    if (empty) AppKeypadNote(text = "Add an exercise to start")
                    if (note) {
                        AppKeypadNote(text = "Suggested 62.5 kg, up 2.5 kg on last time", highlighted = true, action = AppKeypadAction("Why?", onClick = onButtonClick))
                    }
                    if (rest) {
                        AppKeypadTimer(
                            label = "REST",
                            time = "%d:%02d".format(restLeft / 60, restLeft % 60),
                            style = AppKeypadTimerStyle.Rest,
                            less = AppKeypadAction("−10s") { restLeft = (restLeft - 10).coerceAtLeast(0) },
                            more = AppKeypadAction("+10s") { restLeft += 10 },
                            end = AppKeypadAction("Skip") { rest = false },
                        )
                    }
                },
            )
        },
    ) {
        group(key = "values", header = { "Tap a value to type into it" }) {
            items(count = 2, key = { it }) { index ->
                val field = if (index == 0) Field.Count else Field.Weight
                AppListItem(
                    title = if (field == Field.Weight) "Weight" else if (timed) "Time" else "Reps",
                    subtitle = values.getValue(field).ifEmpty { "—" },
                    onClick = {
                        focused = field
                        typing = false
                    },
                )
            }
        }
        group(key = "strips", header = { "Strips and states" }) {
            items(count = 3, key = { it }) { index ->
                when (index) {
                    0 -> AppListItem(title = "Rest timer", subtitle = if (rest) "Counting down, tap to stop" else "Tap to start a 1:30 rest", onClick = { rest = !rest })
                    1 -> AppListItem(title = "Note", subtitle = if (note) "Shown, tap to hide" else "Tap to show a suggestion", onClick = { note = !note })
                    else -> AppListItem(title = "Empty", subtitle = if (empty) "Nothing to log, tap to go back" else "Tap to show it with nothing to log yet", onClick = {
                        empty = !empty
                        focused = null
                    })
                }
            }
        }
    }
}

private enum class Field {
    Count,
    Weight,
}

private const val RestSeconds = 90

private fun format(value: Float): String = if (value % 1f == 0f) value.toInt().toString() else value.toString()

@Preview
@Composable
private fun DesignSystemKeypadPreview() {
    Theme {
        DesignSystemKeypad(onButtonClick = {})
    }
}
