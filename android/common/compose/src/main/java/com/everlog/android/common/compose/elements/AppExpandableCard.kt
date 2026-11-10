package com.everlog.android.common.compose.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// A row in an open AppExpandableCard: an image, a name and a short value at the end, e.g. an exercise and its sets
data class AppExpandableCardRow(val title: String, val imageUrl: String?, val trailing: String)

// Rows done together, e.g. a superset: under [label], each image lettered (a, b, c), with no dividers between them
data class AppExpandableCardGroup(val rows: List<AppExpandableCardRow>, val label: String? = null)

/**
 * A card that opens in place, like a list group: its title over [subtitle] and a peek at the first few rows' images
 * when closed; every group of rows, then [footer], when open. Tapping the title opens and closes it.
 *
 * @param subtitle Under the title. Pass null to hide it, e.g. a summary the open rows already say.
 * @param footer Under the rows while it's open, e.g. buttons acting on the whole card.
 */
@Composable
fun AppExpandableCard(
    title: String,
    groups: List<AppExpandableCardGroup>,
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    AppSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = 0.dp,
        contentPadding = PaddingValues(),
    ) {
        Column(modifier = Modifier.animateContentSize(tween(ExpandableMotion.Collapse, easing = ExpandableMotion.Standard))) {
            Header(
                title = title,
                subtitle = subtitle,
                imageUrls = groups.flatMap { it.rows }.take(ExpandableSizes.PeekCount).map { it.imageUrl },
                open = open,
                onToggle = onToggle,
            )
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(tween(ExpandableMotion.Expand, ExpandableMotion.ExpandDelay, ExpandableMotion.EmphasizedDecelerate)) +
                        expandVertically(tween(ExpandableMotion.Collapse, easing = ExpandableMotion.Standard)),
                exit = fadeOut(tween(ExpandableMotion.FadeOut, easing = ExpandableMotion.EmphasizedAccelerate)) +
                        shrinkVertically(tween(ExpandableMotion.Collapse, easing = ExpandableMotion.Standard)),
            ) {
                Column {
                    groups.forEach { group ->
                        HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                        Group(group)
                    }
                    footer?.let {
                        HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                        Column(modifier = Modifier.padding(Theme.spacing.large)) { it() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(group: AppExpandableCardGroup) {
    val lettered = group.rows.size > 1
    group.label?.let {
        AppText(
            modifier = Modifier.padding(start = Theme.spacing.large, end = Theme.spacing.large, top = Theme.spacing.medium),
            text = it.uppercase(),
            style = Theme.typography.small,
            color = Theme.contentColors.secondary,
        )
    }
    group.rows.forEachIndexed { index, row ->
        AppListItem(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            title = row.title,
            leading = {
                AppThumbnail(
                    imageUrl = row.imageUrl,
                    size = Theme.sizes.leading,
                    corner = ExpandableSizes.Corner,
                    badge = if (lettered) 'a' + index else null,
                )
            },
            trailing = { AppText(text = row.trailing, style = Theme.typography.caption, color = Theme.contentColors.secondary) },
        )
    }
}

/**
 * A few images overlapping, each ringed in the card's colour so they read apart, e.g. a workout's first exercises.
 */
@Composable
fun AppThumbnailStack(imageUrls: List<String?>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(-ExpandableSizes.StackOverlap)) {
        imageUrls.forEach { imageUrl ->
            AppThumbnail(
                modifier = Modifier.border(ExpandableSizes.StackRing, Theme.backgrounds.surface, RoundedCornerShape(ExpandableSizes.Corner)),
                imageUrl = imageUrl,
                size = Theme.sizes.leading,
                corner = ExpandableSizes.Corner,
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String?,
    imageUrls: List<String?>,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(ExpandableMotion.Collapse, easing = ExpandableMotion.Standard),
        label = "chevron",
    )
    val stateDescription = stringResource(if (open) DesignR.string.expandable_card_open else DesignR.string.expandable_card_closed)
    AppListItem(
        modifier = Modifier.semantics(mergeDescendants = true) { this.stateDescription = stateDescription },
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                // Closed: a peek at the rows, which show once it's open
                AnimatedVisibility(
                    visible = !open,
                    enter = fadeIn(tween(ExpandableMotion.SummaryFade, easing = ExpandableMotion.Standard)),
                    exit = fadeOut(tween(ExpandableMotion.FadeOut, easing = ExpandableMotion.Standard)),
                ) {
                    AppThumbnailStack(imageUrls = imageUrls)
                }
                AppIcon(
                    modifier = Modifier.rotate(chevronRotation),
                    imageVector = ImageVector.vectorResource(DesignR.drawable.ic_keyboard_down),
                    tint = Theme.contentColors.secondary,
                )
            }
        },
        onClick = onToggle,
    )
}

private object ExpandableMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val Collapse = 250
    // The rows fade in as it opens
    const val Expand = 200
    const val ExpandDelay = 50
    // The peek fades back in once the rows have gone
    const val SummaryFade = 150
    const val FadeOut = 120
}

private object ExpandableSizes {
    // The workout screen's exercise images
    val Corner = 12.dp
    val StackOverlap = 14.dp
    val StackRing = 2.dp
    const val PeekCount = 3
}

@Preview
@Composable
private fun AppExpandableCardPreview() {
    Theme {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
            val groups = listOf(
                AppExpandableCardGroup(listOf(AppExpandableCardRow("Bench Press", null, "3 × 8"))),
                AppExpandableCardGroup(
                    label = "Superset",
                    rows = listOf(AppExpandableCardRow("Overhead Press", null, "3 × 10"), AppExpandableCardRow("Lateral Raise", null, "3 × 15")),
                ),
            )
            AppExpandableCard(title = "Push day", subtitle = "Done 2 days ago", groups = groups, open = false, onToggle = {})
            AppExpandableCard(title = "Pull day", subtitle = "Done yesterday", groups = groups, open = true, onToggle = {})
        }
    }
}
