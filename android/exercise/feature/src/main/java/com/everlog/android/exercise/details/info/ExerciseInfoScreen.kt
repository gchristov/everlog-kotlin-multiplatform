package com.everlog.android.exercise.details.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppImage
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.android.exercise.R

@Composable
internal fun ExerciseInfoScreen(viewModel: ExerciseInfoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExerciseInfoContent(state = state)
}

// The image, then How to do it and Tips, each only when the exercise has any. A short page, so a plain
// scrolling column of groups rather than AppGroupedList, which would add its top padding to the space
// TabsPage leaves under the tabs.
@Composable
internal fun ExerciseInfoContent(state: ExerciseInfoViewModel.State) {
    val guide = state.guide
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(bottom = Theme.spacing.large)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        ExerciseInfoImage(exercise = state.exercise)
        if (guide.instructions.isNotEmpty()) {
            AppListGroup(header = stringResource(R.string.exercise_info_instructions)) {
                guide.instructions.forEachIndexed { index, step ->
                    row {
                        AppListItem(
                            title = step,
                            leading = { StepNumber(number = index + 1) },
                            leadingAlignment = Alignment.Top,
                        )
                    }
                }
            }
        }
        if (guide.tips.isNotEmpty()) {
            AppListGroup(header = stringResource(R.string.exercise_info_tips)) {
                guide.tips.forEach { tip ->
                    row { AppListItem(title = tip) }
                }
            }
        }
    }
}

// The exercise's illustration, square and rounded on white as the XML screen showed it, or the first
// letter of its name when it has none (e.g. a user's own exercise without a photo)
@Composable
private fun ExerciseInfoImage(exercise: ExerciseInfoViewModel.Exercise) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(Theme.shapes.surface)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (exercise.imageUrl.isNullOrEmpty()) {
            AppText(
                text = exercise.name.take(1),
                color = Theme.backgrounds.primary,
                style = Theme.typography.body.copy(fontSize = InfoSizes.Letter, fontWeight = FontWeight.Medium),
            )
        } else {
            AppImage(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                imageUrl = exercise.imageUrl,
                contentDescription = exercise.name,
            )
        }
    }
}

@Composable
private fun StepNumber(number: Int) {
    Box(
        modifier = Modifier
            .size(InfoSizes.StepNumber)
            .clip(Theme.shapes.button)
            .background(Theme.backgrounds.surfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = number.toString(), style = Theme.typography.button)
    }
}

private object InfoSizes {
    val StepNumber = 28.dp
    val Letter = 120.sp
}

@Preview
@Composable
private fun ExerciseInfoContentPreview() {
    Theme {
        ExerciseInfoContent(
            state = ExerciseInfoViewModel.State(
                exercise = ExerciseInfoViewModel.Exercise(uuid = "bench", name = "Barbell Bench Press", imageUrl = null, own = false),
                guide = ExerciseGuide(
                    instructions = listOf("Lie on a flat bench with your feet flat on the floor.", "Lower the bar to your chest.", "Press it back up."),
                    tips = listOf("Keep your feet flat on the floor."),
                ),
            ),
        )
    }
}
