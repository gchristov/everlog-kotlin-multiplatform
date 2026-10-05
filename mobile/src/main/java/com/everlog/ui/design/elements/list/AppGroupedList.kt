package com.everlog.ui.design.elements.list

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.everlog.ui.design.elements.AppSectionHeader
import com.everlog.ui.design.theme.Theme
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * A list of groups, each an optional [AppSectionHeader] above its rows drawn as one card and an
 * optional footer note under them, like the sections in Settings. For a single group inside other
 * scrolling content, use [AppListGroup].
 *
 * @param contentPadding Added to the list's own padding, e.g. AppScreen's padding so the last rows
 * scroll clear of its footer.
 */
@Composable
fun AppGroupedList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    showDividers: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    content: AppGroupedListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = Theme.spacing.large,
            top = Theme.spacing.large + contentPadding.calculateTopPadding(),
            end = Theme.spacing.large,
            bottom = Theme.spacing.large + contentPadding.calculateBottomPadding(),
        )
    ) {
        RealAppGroupedListScope(
            lazyListScope = this,
            showDividers = showDividers
        ).content()
    }
}

@LazyScopeMarker
interface AppGroupedListScope {
    fun group(
        key: Any,
        header: @Composable (() -> String)? = null,
        footer: @Composable (() -> String)? = null,
        content: AppGroupScope.() -> Unit
    )

    fun item(
        key: Any,
        content: @Composable () -> Unit
    )
}

@LazyScopeMarker
interface AppGroupScope {
    val groupKey: Any

    fun items(
        count: Int,
        key: (index: Int) -> Any,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    )
}

@Suppress("unused")
inline fun <T> AppGroupScope.items(
    items: List<T>,
    noinline key: (item: T) -> Any,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size, { index: Int -> key(items[index]) }) {
    itemContent(items[it])
}

private class RealAppGroupedListScope(
    private val lazyListScope: LazyListScope,
    private val showDividers: Boolean
) : AppGroupedListScope {
    private var groupCount = 0

    override fun group(
        key: Any,
        header: @Composable (() -> String)?,
        footer: @Composable (() -> String)?,
        content: AppGroupScope.() -> Unit
    ) {
        val isFirstGroup = groupCount == 0
        groupCount++

        lazyListScope.item(key = key) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Space between groups, like margin_26 between the sections in Settings
                if (!isFirstGroup) {
                    Spacer(modifier = Modifier.height(Theme.spacing.extraLarge))
                }
                if (header != null) {
                    AppListGroupHeader(text = header())
                }
            }
        }
        RealAppGroupScope(
            groupKey = key,
            lazyListScope = lazyListScope,
            showDividers = showDividers
        ).apply(content)
        if (footer != null) {
            lazyListScope.item(key = AppCompositeKey(key, GroupFooterKey)) {
                AppListGroupFooter(text = footer())
            }
        }
    }

    override fun item(
        key: Any,
        content: @Composable () -> Unit
    ) {
        lazyListScope.item(key = key) {
            content()
        }
    }
}

private class RealAppGroupScope(
    override val groupKey: Any,
    private val lazyListScope: LazyListScope,
    private val showDividers: Boolean
) : AppGroupScope {
    private var itemCount = 0

    override fun items(
        count: Int,
        key: (index: Int) -> Any,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        val firstItemIndex = itemCount
        itemCount += count

        lazyListScope.items(
            count = count,
            key = { index -> AppCompositeKey(groupKey, key(index)) }
        ) { index ->
            this@RealAppGroupScope.Item(itemIndex = firstItemIndex + index) { itemContent(index) }
        }
    }

    @Composable
    private fun Item(
        itemIndex: Int,
        content: @Composable () -> Unit
    ) {
        AppListGroupRow(
            shape = appListGroupRowShape(index = itemIndex, count = itemCount),
            showDivider = showDividers && itemIndex < itemCount - 1,
            content = content,
        )
    }
}

@Parcelize
private data class AppCompositeKey(
    val groupKey: @RawValue Any,
    val itemKey: @RawValue Any
) : Parcelable

private const val GroupFooterKey = "footer"

