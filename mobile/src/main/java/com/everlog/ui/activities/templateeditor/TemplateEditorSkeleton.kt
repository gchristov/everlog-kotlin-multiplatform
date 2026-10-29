package com.everlog.ui.activities.templateeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.rememberShimmerBrush
import com.everlog.ui.design.theme.Theme

private object SkeletonSizes {
    val LineCorner = 6.dp
    val ThumbnailCorner = 12.dp
    val HeaderHeight = 14.dp
    val HeaderWidth = 110.dp
    val CaptionHeight = 10.dp
    val TitleHeight = 14.dp
    val SetNumberWidth = 20.dp
    val ValueWidth = 48.dp
    // As the exercise card's
    val RowHeight = 52.dp
    val RowPadding = 12.dp
    // The SET and REPS labels over the card
    val LabelsHeight = 16.dp
    val SetsPerExercise = listOf(3, 2)
    val NameWidths = listOf(0.6f, 0.45f)
}

// Loading: the details and two exercises, each with its grip, in the list's layout, shimmering
@Composable
internal fun TemplateEditorSkeleton(contentPadding: PaddingValues) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TemplateEditorTags.Loading)
            .clearAndSetSemantics {}
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.medium, bottom = contentPadding.calculateBottomPadding()),
    ) {
        Line(brush, SkeletonSizes.HeaderWidth, SkeletonSizes.HeaderHeight)
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        Column(modifier = Modifier.fillMaxWidth().clip(Theme.shapes.surface).background(Theme.backgrounds.surface)) {
            repeat(2) { row ->
                if (row > 0) HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                Column(modifier = Modifier.padding(Theme.spacing.large), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                    Line(brush, fraction = 0.2f, height = SkeletonSizes.CaptionHeight)
                    Line(brush, fraction = if (row == 0) 0.5f else 0.25f, height = SkeletonSizes.TitleHeight)
                }
            }
        }
        Spacer(modifier = Modifier.height(Theme.spacing.extraLarge))
        Line(brush, SkeletonSizes.HeaderWidth, SkeletonSizes.HeaderHeight)
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        SkeletonSizes.SetsPerExercise.forEachIndexed { exercise, sets ->
            if (exercise > 0) Spacer(modifier = Modifier.height(Theme.spacing.extraLarge))
            Exercise(brush, sets, SkeletonSizes.NameWidths[exercise])
        }
    }
}

@Composable
private fun Exercise(brush: Brush, sets: Int, nameWidth: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(Theme.sizes.leading).clip(CircleShape).background(brush))
        Spacer(modifier = Modifier.width(Theme.spacing.large))
        Box(modifier = Modifier.size(Theme.sizes.leading).clip(RoundedCornerShape(SkeletonSizes.ThumbnailCorner)).background(brush))
        Spacer(modifier = Modifier.width(Theme.spacing.medium))
        Line(brush, fraction = nameWidth, height = SkeletonSizes.TitleHeight)
    }
    Spacer(modifier = Modifier.height(Theme.spacing.medium + SkeletonSizes.LabelsHeight + Theme.spacing.extraSmall))
    Column(
        modifier = Modifier
            .padding(start = cardIndent())
            .fillMaxWidth()
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.surface),
    ) {
        repeat(sets) { set ->
            if (set > 0) HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
            Row(
                modifier = Modifier.height(SkeletonSizes.RowHeight).padding(horizontal = SkeletonSizes.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
            ) {
                Line(brush, SkeletonSizes.SetNumberWidth, SkeletonSizes.TitleHeight)
                Line(brush, SkeletonSizes.ValueWidth, SkeletonSizes.TitleHeight)
            }
        }
    }
}

@Composable
private fun Line(brush: Brush, width: Dp, height: Dp) {
    Box(modifier = Modifier.size(width, height).clip(RoundedCornerShape(SkeletonSizes.LineCorner)).background(brush))
}

@Composable
private fun Line(brush: Brush, fraction: Float, height: Dp) {
    Box(modifier = Modifier.fillMaxWidth(fraction).height(height).clip(RoundedCornerShape(SkeletonSizes.LineCorner)).background(brush))
}
