package com.everlog.ui.design.elements.list

import android.os.Parcelable
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.AppSectionHeader
import com.everlog.ui.design.theme.Theme
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * A list of groups, each an optional [AppSectionHeader] above its rows drawn as one card, like the
 * sections in Settings.
 *
 * @param contentPadding Added to the list's own padding, e.g. AppScreen's padding so the last rows
 * scroll clear of its bottom bar.
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
            start = AppGroupedListSpacing,
            top = AppGroupedListSpacing + contentPadding.calculateTopPadding(),
            end = AppGroupedListSpacing,
            bottom = AppGroupedListSpacing + contentPadding.calculateBottomPadding(),
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
        content: AppGroupScope.() -> Unit
    ) {
        val isFirstGroup = groupCount == 0
        groupCount++

        lazyListScope.item(key = key) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Space between groups, like margin_26 between the sections in Settings
                if (!isFirstGroup) {
                    Spacer(modifier = Modifier.height(AppGroupSpacing))
                }
                if (header != null) {
                    AppSectionHeader(
                        modifier = Modifier.padding(bottom = AppGroupedListSpacing),
                        text = header(),
                    )
                }
            }
        }
        RealAppGroupScope(
            groupKey = key,
            lazyListScope = lazyListScope,
            showDividers = showDividers
        ).apply(content)
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
        val isAtLastRow = itemIndex == itemCount - 1
        val shape = when {
            itemCount == 1 -> Theme.shapes.groupSingle
            itemIndex == 0 -> Theme.shapes.groupStart
            isAtLastRow -> Theme.shapes.groupEnd
            else -> Theme.shapes.groupMiddle
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Theme.backgrounds.surface)
        ) {
            content()

            if (showDividers && !isAtLastRow) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Theme.backgrounds.separator,
                )
            }
        }
    }
}

@Parcelize
private data class AppCompositeKey(
    val groupKey: @RawValue Any,
    val itemKey: @RawValue Any
) : Parcelable

private val AppGroupedListSpacing = 16.dp
private val AppGroupSpacing = 26.dp
