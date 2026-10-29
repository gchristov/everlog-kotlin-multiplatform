package com.everlog.ui.activities.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppFooter
import com.everlog.android.common.compose.elements.AppFooterAction
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme

// The footer pinned to the bottom like on a real screen: a week of routines to scroll behind it and
// see the blur. Add updates its count in place; Next step changes the transitionKey, so the actions
// crossfade, like onboarding's steps.
@Composable
internal fun DesignSystemFooter(onButtonClick: () -> Unit) {
    var count by remember { mutableIntStateOf(1) }
    var step by remember { mutableIntStateOf(0) }
    ShowcasePage(
        title = "Footer",
        footer = {
            AppFooter(
                header = "You can change any of these later from Routines.",
                transitionKey = step,
                actions = if (step % 2 == 0) {
                    listOf(
                        AppFooterAction(
                            text = "Add $count",
                            onClick = {
                                onButtonClick()
                                count++
                            },
                        ),
                        AppFooterAction(
                            text = "Next step",
                            onClick = { step++ },
                            style = AppFooterAction.Style.Secondary,
                        ),
                    )
                } else {
                    listOf(
                        AppFooterAction(
                            text = "Start workout",
                            onClick = onButtonClick,
                        ),
                        AppFooterAction(
                            text = "Back",
                            onClick = { step++ },
                            style = AppFooterAction.Style.Tertiary,
                        ),
                    )
                },
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
