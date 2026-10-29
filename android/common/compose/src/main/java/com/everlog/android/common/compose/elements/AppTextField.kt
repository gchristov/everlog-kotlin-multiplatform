package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme

/**
 * A one-line text field in an inset box, outlined in the action colour while it has focus, e.g. a name in a dialog.
 *
 * @param maxLength Typing past it is ignored; text already longer can still be shortened
 * @param autoFocus Takes focus, which opens the keyboard, as soon as it shows
 * @param selectAllOnFocus Selects the text when it takes focus, so typing replaces it, e.g. renaming
 * @param onDone The keyboard's done key
 */
@Composable
fun AppTextField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    maxLength: Int? = null,
    autoFocus: Boolean = false,
    selectAllOnFocus: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onDone: () -> Unit = {},
) {
    // The field's own value carries the selection, which the text alone can't
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    // Text changed from outside starts again with the cursor at its end
    val value = if (field.text == text) field else TextFieldValue(text, TextRange(text.length))
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }
    LaunchedEffect(focused) {
        if (focused && selectAllOnFocus) field = TextFieldValue(text, TextRange(0, text.length))
    }
    val shape = Theme.shapes.button
    val selection = TextSelectionColors(handleColor = Theme.contentColors.action, backgroundColor = Theme.contentColors.action.copy(alpha = 0.4f))
    CompositionLocalProvider(LocalTextSelectionColors provides selection) {
        BasicTextField(
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = value,
            onValueChange = { value ->
                // Text already longer (e.g. saved before there was a limit) can still be shortened
                if (maxLength != null && value.text.length > maxLength && value.text.length > text.length) return@BasicTextField
                field = value
                if (value.text != text) onTextChange(value.text)
            },
            singleLine = true,
            interactionSource = interactions,
            textStyle = Theme.typography.body.copy(color = Theme.contentColors.primary),
            cursorBrush = SolidColor(Theme.contentColors.action),
            keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { input ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TextFieldSizes.MinHeight)
                        .clip(shape)
                        .background(Theme.backgrounds.inset)
                        .border(
                            if (focused) TextFieldSizes.FocusOutline else 1.dp,
                            if (focused) Theme.contentColors.action else Theme.backgrounds.separator,
                            shape,
                        )
                        .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.medium),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (text.isEmpty() && placeholder != null) {
                        AppText(text = placeholder, color = Theme.contentColors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    input()
                }
            },
        )
    }
}

private object TextFieldSizes {
    val MinHeight = 48.dp
    val FocusOutline = 1.5.dp
}

@Preview
@Composable
private fun AppTextFieldPreview() {
    Theme {
        AppTextField(text = "Push day", onTextChange = {}, placeholder = "Name")
    }
}
