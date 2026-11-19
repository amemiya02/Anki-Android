// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer.whiteboard

import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.compose.theme.foundation
import com.ichi2.utils.toRGBAHex
import kotlin.math.roundToInt
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun WhiteboardControls(
    model: WhiteboardViewModel,
    overflowMenu: () -> Menu,
    onMenuItem: (MenuItem) -> Boolean,
    onEraser: () -> Unit,
    onBrush: (Int) -> Unit,
    onRemoveBrush: (Int) -> Unit,
) {
    val alignment by model.toolbarAlignment.collectAsStateWithLifecycle()
    val canUndo by model.canUndo.collectAsStateWithLifecycle()
    val canRedo by model.canRedo.collectAsStateWithLifecycle()
    val activeTool by model.activeTool.collectAsStateWithLifecycle()
    val activeIndex by model.activeBrushIndex.collectAsStateWithLifecycle()
    val brushes by model.brushes.collectAsStateWithLifecycle()
    val eraser = activeTool as? WhiteboardTool.Eraser
    val ink = eraser ?: model.eraser
    val foundation = MaterialTheme.foundation
    val content: @Composable () -> Unit = {
        IconButton(onClick = model::undo, enabled = canUndo) {
            Icon(painterResource(R.drawable.symbol_undo), stringResource(CommonString.undo))
        }
        IconButton(onClick = model::redo, enabled = canRedo) {
            Icon(painterResource(R.drawable.symbol_redo), stringResource(CommonString.redo))
        }
        ToolSwatch(ink.width, eraser != null, stringResource(CommonString.eraser), onEraser) {
            Icon(painterResource(if (ink.mode == EraserMode.INK) R.drawable.symbol_ink_eraser else R.drawable.symbol_edit_off), null)
        }
        Box {
            var menu by remember { mutableStateOf<Menu?>(null) }
            IconButton(onClick = { menu = overflowMenu() }) {
                Icon(painterResource(CommonR.drawable.symbol_more_vert), stringResource(CommonString.whiteboard_more_options))
            }
            val opened = menu
            if (opened != null) {
                DropdownMenu(expanded = true, onDismissRequest = { menu = null }) {
                    for (item in opened.children.filter(MenuItem::isVisible)) {
                        DropdownMenuItem(
                            text = { Text(item.title.toString()) },
                            enabled = item.isEnabled,
                            trailingIcon = if (item.isChecked) ({ Icon(painterResource(R.drawable.symbol_check), null) }) else null,
                            onClick = {
                                if (item.hasSubMenu()) {
                                    menu = item.subMenu
                                } else {
                                    menu = null
                                    onMenuItem(item)
                                }
                            },
                        )
                    }
                }
            }
        }
        brushes.forEachIndexed { index, brush ->
            ToolSwatch(
                brush.width,
                eraser == null && activeIndex == index,
                brush.color.toRGBAHex(),
                { onBrush(index) },
                { onRemoveBrush(index) },
            ) {
                Canvas(Modifier.size(24.dp)) {
                    drawCircle(Color(brush.color), radius = (brush.width / 2).coerceIn(2f, size.minDimension / 2))
                }
            }
        }
    }
    Surface(
        color = foundation.colors.background,
        contentColor = foundation.colors.onBackground,
        border = BorderStroke(foundation.shapes.hairline, foundation.colors.outline),
    ) {
        if (alignment == ToolbarAlignment.BOTTOM) {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { content() }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
    }
}

@Composable
private fun ToolSwatch(
    width: Float,
    checked: Boolean,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    preview: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .background(if (checked) colors.secondaryContainer else Color.Transparent)
            .combinedClickable(role = Role.RadioButton, onClick = onClick, onLongClick = onLongClick)
            .semantics {
                selected = checked
                contentDescription = description
            }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        preview()
        Text(width.roundToInt().toString(), style = MaterialTheme.foundation.type.label)
    }
}
