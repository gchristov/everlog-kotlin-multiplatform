package com.everlog.ui.design.elements.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * A search field in a card: a search icon, then the text. It has no outline or colour change when
 * focused; what it opens (e.g. a menu of suggestions) shows it's active.
 *
 * @param leadingContent Shown before the text and wrapping with it onto more lines, e.g. the filters
 * applied so far as tokens. The text field takes the rest of the last line.
 * @param onFocused Called when the field gets focus, e.g. to open suggestions.
 * @param onSearch The keyboard's search key.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocused: () -> Unit = {},
    onSearch: () -> Unit = {},
    leadingContent: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SearchBarSizes.MinHeight)
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.surface)
            // The whole card focuses the field, not just the text
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focusRequester.requestFocus()
            }
            .padding(horizontal = Theme.spacing.large, vertical = SearchBarSizes.VerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppIcon(
            modifier = Modifier
                .padding(top = SearchBarSizes.IconTop)
                .size(SearchBarSizes.Icon),
            imageVector = ImageVector.vectorResource(DesignR.drawable.ic_search_white),
            tint = Theme.contentColors.secondary,
        )
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(SearchBarSizes.ContentGap),
            verticalArrangement = Arrangement.spacedBy(SearchBarSizes.ContentGap),
        ) {
            leadingContent?.invoke(this)
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = SearchBarSizes.InputMinWidth)
                    .height(SearchBarSizes.LineHeight)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onFocused() },
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                singleLine = true,
                textStyle = Theme.typography.body.copy(color = Theme.contentColors.primary),
                cursorBrush = SolidColor(Theme.contentColors.action),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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

private object SearchBarSizes {
    val MinHeight = 48.dp
    val VerticalPadding = 9.dp
    val Icon = 24.dp
    // Centres the icon on the first line
    val IconTop = 3.dp
    // A line of the field, and of anything in leadingContent
    val LineHeight = 30.dp
    val ContentGap = 6.dp
    val InputMinWidth = 80.dp
}

@Preview
@Composable
private fun AppSearchBarPreview() {
    Theme {
        AppSearchBar(text = "", onTextChange = {}, placeholder = "Search 168 exercises")
    }
}
