// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ichi2.compose.theme.foundation
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** The transient drag handle can skip directly to a position without rendering intervening rows. */
@Composable
internal fun BrowserScrollbar(
    list: LazyListState,
    rowCount: Int,
    modifier: Modifier = Modifier,
) {
    if (rowCount <= 1) return
    var visible by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(list.isScrollInProgress, dragging) {
        if (list.isScrollInProgress || dragging) {
            visible = true
        } else {
            delay(650)
            visible = false
        }
    }
    if (!visible) return

    val foundation = MaterialTheme.foundation
    val endIsRight = LocalLayoutDirection.current == LayoutDirection.Ltr
    Canvas(
        modifier
            .fillMaxHeight()
            .width(foundation.spacing.touchTarget)
            .pointerInput(list, rowCount) {
                fun jump(y: Float) {
                    val fraction = (y / size.height).coerceIn(0f, 1f)
                    list.requestScrollToItem((fraction * rowCount).roundToInt())
                }
                detectVerticalDragGestures(
                    onDragStart = { position ->
                        dragging = true
                        jump(position.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        jump(change.position.y)
                    },
                )
            },
    ) {
        val width = 4.dp.toPx()
        val height = 40.dp.toPx().coerceAtMost(size.height)
        val fraction = (list.firstVisibleItemIndex.toFloat() / rowCount).coerceIn(0f, 1f)
        val left = if (endIsRight) size.width - 7.dp.toPx() else 3.dp.toPx()
        drawRoundRect(
            color = foundation.colors.onBackgroundMuted,
            topLeft = Offset(left, fraction * (size.height - height)),
            size = Size(width, height),
            cornerRadius = CornerRadius(width / 2),
        )
    }
}
