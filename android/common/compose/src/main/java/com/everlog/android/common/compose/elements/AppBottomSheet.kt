package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import kotlinx.coroutines.launch

/**
 * What an [AppBottomSheet]'s content can do besides lay itself out: close the sheet, then act on what was picked.
 */
interface AppBottomSheetScope : ColumnScope {
    // Slides the sheet away, then runs [then], e.g. what a row was picked for
    fun dismiss(then: () -> Unit = {})
}

/**
 * A sheet that slides up over the screen, on the screen's background, with an optional [title] over its content
 * (usually list groups, `--space-lg` apart and from the sides). Tapping outside it or swiping it down calls
 * [onDismissRequest]; the content closes it with [AppBottomSheetScope.dismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable AppBottomSheetScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Theme.backgrounds.primary,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.large)
                .padding(bottom = Theme.spacing.large)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            title?.let { AppText(text = it, style = Theme.typography.heading) }
            val sheetScope = remember(this) {
                object : AppBottomSheetScope, ColumnScope by this {
                    override fun dismiss(then: () -> Unit) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismissRequest()
                            then()
                        }
                    }
                }
            }
            sheetScope.content()
        }
    }
}

@Preview
@Composable
private fun AppBottomSheetPreview() {
    Theme {
        AppBottomSheet(onDismissRequest = {}, title = "Workout options") {
            AppListGroup {
                row { AppListItem(title = "Edit workout", subtitle = "Reorder or delete exercises", onClick = { dismiss() }) }
            }
        }
    }
}
