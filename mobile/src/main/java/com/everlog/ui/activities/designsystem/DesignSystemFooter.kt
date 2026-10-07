package com.everlog.ui.activities.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// The footer pinned to the bottom like on a real screen: a week of routines to scroll behind it and
// see the blur, and a footer with a note and two actions
@Composable
internal fun DesignSystemFooter(onButtonClick: () -> Unit) {
    ShowcasePage(
        title = "Footer",
        footer = {
            AppFooter(
                header = "You can change any of these later from Routines.",
                actions = listOf(
                    AppFooterAction(
                        text = "Start workout",
                        onClick = onButtonClick,
                    ),
                    AppFooterAction(
                        text = "Later",
                        onClick = onButtonClick,
                        style = AppFooterAction.Style.Secondary,
                    ),
                )
            )
        },
    ) {
        SampleWeek.forEach { (day, exercises) ->
            group(
                key = day,
                header = { day },
            ) {
                items(count = exercises.size, key = { it }) { index ->
                    AppListItem(
                        title = exercises[index],
                        subtitle = "3 × 8–12",
                    )
                }
            }
        }
    }
}

private val SampleWeek = listOf(
    "Push · Monday" to listOf("Bench Press", "Military Press", "Dumbbell Incline Press", "Lateral Raise", "Cable Pushdown"),
    "Pull · Wednesday" to listOf("Barbell Deadlift", "Pull Up", "Bent Over Row", "Lat Pulldown", "Cable Face Pull"),
    "Legs · Friday" to listOf("Parallel Squat", "Straight Leg Deadlift", "Forward Lunge", "Hip Thrust", "Single Leg Calf Raise"),
)

@Preview
@Composable
private fun DesignSystemFooterPreview() {
    Theme {
        DesignSystemFooter(onButtonClick = {})
    }
}
