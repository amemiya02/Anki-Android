// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import anki.search.BrowserColumns
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun BrowserColumnsPage(
    vm: CardBrowserViewModel,
    initialMode: CardsOrNotes,
    onClose: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val title = stringResource(CommonString.browse_manage_columns_main_heading)
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    val columns by produceState<Map<String, BrowserColumns.Column>>(emptyMap()) {
        value = withCol { allBrowserColumns().associateBy { it.key } }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .semantics { paneTitle = title },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = foundation.spacing.screenMargin, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowserIcon(CommonR.drawable.symbol_arrow_back, stringResource(R.string.browse_back_from_columns), onClose)
            Text(
                title,
                Modifier.weight(1f).semantics { heading() },
                color = colors.onBackground,
                style = foundation.type.pageTitle,
            )
            BrowserModeSwitch(mode) { mode = it }
        }
        if (columns.isNotEmpty()) {
            key(mode) {
                var shown by remember { mutableStateOf(vm.columnsForMode(mode)) }
                BrowserColumnList(shown, { columns.getValue(it.ankiColumnKey).getLabel(mode) }) { updated ->
                    if (vm.updateActiveColumns(updated, mode)) shown = updated
                }
            }
        }
    }
}

@Composable
private fun BrowserColumnList(
    shown: List<CardBrowserColumn>,
    labelFor: (CardBrowserColumn) -> String,
    onChange: (List<CardBrowserColumn>) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val listState = rememberLazyListState()
    var dragged by remember { mutableStateOf<CardBrowserColumn?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun move(
        from: Int,
        to: Int,
    ) {
        if (to == from || to !in shown.indices) return
        onChange(shown.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun finishDrag() {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == dragged?.ankiColumnKey }
        if (item != null) {
            val center = item.offset + item.size / 2f + dragOffset
            val target =
                listState.layoutInfo.visibleItemsInfo
                    .filter { candidate -> shown.any { it.ankiColumnKey == candidate.key } }
                    .minByOrNull { kotlin.math.abs(center - (it.offset + it.size / 2f)) }
            val start = shown.indexOfFirst { it == dragged }
            val end = shown.indexOfFirst { it.ankiColumnKey == target?.key }
            if (start >= 0) move(start, end)
        }
        dragged = null
        dragOffset = 0f
    }

    val drop by rememberUpdatedState(::finishDrag)
    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(
                start = foundation.spacing.screenMargin,
                end = foundation.spacing.screenMargin,
                bottom = 12.dp,
            ),
    ) {
        item {
            Text(
                stringResource(R.string.browse_columns_explanation),
                Modifier.padding(vertical = 8.dp),
                color = colors.onBackgroundMuted,
                style = foundation.type.body,
            )
            BrowserColumnSection(stringResource(R.string.browse_columns_shown))
        }
        itemsIndexed(shown, key = { _, column -> column.ankiColumnKey }) { index, column ->
            val label = labelFor(column)
            val moving = dragged == column
            Column(
                Modifier
                    .zIndex(if (moving) 1f else 0f)
                    .graphicsLayer { if (moving) translationY = dragOffset }
                    .background(colors.background),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = foundation.spacing.touchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrowserColumnMoveButton(
                        label = label,
                        canMoveUp = index > 0,
                        canMoveDown = index < shown.lastIndex,
                        onMove = { move(index, index + it) },
                        modifier =
                            Modifier.pointerInput(column, shown.size) {
                                detectDragGestures(
                                    onDragStart = { dragged = column },
                                    onDragEnd = { drop() },
                                    onDragCancel = {
                                        dragged = null
                                        dragOffset = 0f
                                    },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                }
                            },
                    )
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(label, color = colors.onBackground, style = foundation.type.rowTitle)
                        if (index == 0) {
                            Text(
                                stringResource(R.string.browse_columns_row_title),
                                color = colors.onBackgroundMuted,
                                style = foundation.type.caption,
                            )
                        }
                    }
                    BrowserColumnToggle(
                        shown = true,
                        enabled = shown.size > 1,
                        label = label,
                        onClick = { onChange(shown - column) },
                    )
                }
                BrowserColumnRule()
            }
        }
        item { BrowserColumnSection(stringResource(CommonString.user_potential_columns)) }
        items(browserColumnOrder.filterNot { it in shown }, key = { it.ankiColumnKey }) { column ->
            val label = labelFor(column)
            Row(
                Modifier.fillMaxWidth().heightIn(min = foundation.spacing.touchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrowserColumnToggle(shown = false, enabled = true, label = label, onClick = { onChange(shown + column) })
                Text(
                    label,
                    Modifier.weight(1f).padding(vertical = 8.dp),
                    color = colors.onBackground,
                    style = foundation.type.rowTitle,
                )
            }
            BrowserColumnRule()
        }
    }
}

private val browserColumnOrder =
    (
        listOf(
            CardBrowserColumn.QUESTION,
            CardBrowserColumn.ANSWER,
            CardBrowserColumn.TAGS,
            CardBrowserColumn.NOTE_TYPE,
            CardBrowserColumn.INTERVAL,
            CardBrowserColumn.EASE,
            CardBrowserColumn.CREATED,
            CardBrowserColumn.EDITED,
        ) + CardBrowserColumn.entries
    ).distinct()

@Composable
private fun BrowserColumnSection(label: String) {
    val foundation = MaterialTheme.foundation
    Text(
        label,
        Modifier.padding(top = 16.dp, bottom = 8.dp).semantics { heading() },
        color = foundation.colors.onBackgroundMuted,
        style = foundation.type.caption,
    )
    BrowserColumnRule()
}

@Composable
private fun BrowserColumnRule() {
    val foundation = MaterialTheme.foundation
    HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
}

@Composable
private fun BrowserColumnToggle(
    shown: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val tint = if (shown) foundation.colors.onBackgroundMuted else foundation.colors.emphasis
    IconButton(onClick, modifier = Modifier.size(foundation.spacing.touchTarget), enabled = enabled) {
        Box(Modifier.size(20.dp).border(1.5.dp, tint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(if (shown) CommonR.drawable.symbol_remove else CommonR.drawable.symbol_add),
                stringResource(if (shown) R.string.browse_columns_hide else R.string.browse_columns_show, label),
                Modifier.size(14.dp),
                tint,
            )
        }
    }
}

@Composable
private fun BrowserColumnMoveButton(
    label: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    modifier: Modifier,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    var open by remember { mutableStateOf(false) }
    val moveUp = stringResource(R.string.browse_columns_move_up)
    val moveDown = stringResource(R.string.browse_columns_move_down)
    Box {
        IconButton(
            onClick = { open = true },
            enabled = canMoveUp || canMoveDown,
            modifier =
                modifier
                    .size(foundation.spacing.touchTarget)
                    .semantics {
                        customActions =
                            buildList {
                                if (canMoveUp) {
                                    add(
                                        CustomAccessibilityAction(moveUp) {
                                            onMove(-1)
                                            true
                                        },
                                    )
                                }
                                if (canMoveDown) {
                                    add(
                                        CustomAccessibilityAction(moveDown) {
                                            onMove(1)
                                            true
                                        },
                                    )
                                }
                            }
                    },
        ) {
            Icon(
                painterResource(R.drawable.symbol_drag_indicator),
                stringResource(R.string.browse_columns_reorder, label),
                tint = colors.onBackgroundMuted,
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = foundation.shapes.button,
            shadowElevation = 0.dp,
            containerColor = colors.background,
        ) {
            if (canMoveUp) {
                DropdownMenuItem(
                    text = { Text(moveUp, style = foundation.type.body, color = colors.onBackground) },
                    onClick = {
                        open = false
                        onMove(-1)
                    },
                )
            }
            if (canMoveDown) {
                DropdownMenuItem(
                    text = { Text(moveDown, style = foundation.type.body, color = colors.onBackground) },
                    onClick = {
                        open = false
                        onMove(1)
                    },
                )
            }
        }
    }
}
