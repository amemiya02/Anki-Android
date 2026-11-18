// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences.reviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.preferences.SettingsDivider
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.preferences.SettingsSearchTarget
import com.ichi2.anki.preferences.SettingsSectionTitle
import com.ichi2.anki.preferences.settingsBackAction
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.preferences.settingsSearchTarget
import com.ichi2.anki.utils.ext.sharedPrefs
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import kotlin.math.roundToInt

class ReviewerMenuSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val repository = ReviewerMenuRepository(sharedPrefs())
        return settingsComposeView(R.id.settings_toolbar_actions) {
            ToolbarActionsPage(
                repository,
                settingsSearchTarget,
                settingsBackAction,
            )
        }
    }
}

private data class ToolbarDrag(
    val action: ViewerAction,
    val pointerY: Float,
    val grabOffset: Float,
)

private data class ToolbarDrop(
    val group: MenuDisplayType,
    val before: ViewerAction?,
)

private class ToolbarLayout {
    var viewport: LayoutCoordinates? = null
    val headers = mutableMapOf<MenuDisplayType, LayoutCoordinates>()
    val rows = mutableMapOf<ViewerAction, LayoutCoordinates>()

    fun target(
        y: Float,
        groups: Map<MenuDisplayType, List<ViewerAction>>,
        moving: ViewerAction,
    ): ToolbarDrop? {
        val group = MenuDisplayType.entries.lastOrNull { y >= (headers[it].top ?: Float.POSITIVE_INFINITY) } ?: return null
        val before =
            groups.getValue(group).firstOrNull { action ->
                val row = rows[action]
                action != moving && row != null && row.isAttached && y < row.positionInRoot().y + row.size.height / 2f
            }
        return ToolbarDrop(group, before)
    }

    fun insertionY(
        target: ToolbarDrop,
        groups: Map<MenuDisplayType, List<ViewerAction>>,
    ): Float? {
        target.before?.let { return rows[it].top }
        val last = groups.getValue(target.group).lastOrNull()?.let(rows::get)
        return last.bottom ?: headers[target.group].bottom
    }
}

private val LayoutCoordinates?.top: Float?
    get() = this?.takeIf { it.isAttached }?.positionInRoot()?.y

private val LayoutCoordinates?.bottom: Float?
    get() = this?.takeIf { it.isAttached }?.let { it.positionInRoot().y + it.size.height }

@Composable
private fun ToolbarActionsPage(
    repository: ReviewerMenuRepository,
    searchTarget: String?,
    onBack: (() -> Unit)?,
) {
    val layout = remember { ToolbarLayout() }
    val scroll = rememberScrollState()
    var groups by remember(repository) { mutableStateOf(repository.getActionsByMenuDisplayTypes()) }
    var drag by remember { mutableStateOf<ToolbarDrag?>(null) }
    var moveAction by rememberSaveable { mutableStateOf<ViewerAction?>(null) }
    var revealTarget by rememberSaveable { mutableStateOf(searchTarget) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val edge = with(density) { 56.dp.toPx() }
    val scrollSpeed = with(density) { 600.dp.toPx() }
    val markerOffset = with(density) { 8.dp.roundToPx() }

    fun move(
        action: ViewerAction,
        drop: ToolbarDrop,
    ) {
        val updated = groups.mapValues { (_, actions) -> (actions - action).toMutableList() }
        val destination = updated.getValue(drop.group)
        val index = destination.indexOf(drop.before)
        destination.add(if (index >= 0) index else destination.size, action)
        if (updated == groups) return
        revealTarget = null
        repository.setDisplayTypeActions(
            updated.getValue(MenuDisplayType.ALWAYS),
            updated.getValue(MenuDisplayType.MENU_ONLY),
            updated.getValue(MenuDisplayType.DISABLED),
        )
        groups = updated
    }

    LaunchedEffect(drag?.action) {
        if (drag == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                val elapsed = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.05f)
                previousFrame = frame
                val y = drag?.pointerY ?: return@withFrameNanos
                val top = layout.viewport.top ?: return@withFrameNanos
                val bottom = layout.viewport.bottom ?: return@withFrameNanos
                val direction =
                    when {
                        y < top + edge -> -(top + edge - y) / edge
                        y > bottom - edge -> (y - (bottom - edge)) / edge
                        else -> 0f
                    }
                if (direction != 0f) scroll.dispatchRawDelta(direction.coerceIn(-1f, 1f) * scrollSpeed * elapsed)
            }
        }
    }

    SettingsPage(stringResource(CommonString.toolbar_actions), onBack) {
        val foundation = MaterialTheme.foundation
        Box(Modifier.fillMaxSize().clipToBounds().onGloballyPositioned { layout.viewport = it }) {
            Column(Modifier.verticalScroll(scroll).padding(bottom = 24.dp)) {
                Text(
                    stringResource(R.string.settings_toolbar_drag_hint),
                    Modifier.padding(top = 8.dp),
                    style = foundation.type.body,
                    color = foundation.colors.onBackgroundMuted,
                )
                for ((group, actions) in groups) {
                    Box(Modifier.fillMaxWidth().onGloballyPositioned { layout.headers[group] = it }) {
                        SettingsSectionTitle(stringResource(group.title))
                    }
                    SettingsDivider()
                    for (action in actions) {
                        key(action) {
                            SettingsSearchTarget(revealTarget == action.name) { modifier, highlighted ->
                                ToolbarActionRow(
                                    action = action,
                                    modifier =
                                        modifier
                                            .onGloballyPositioned { layout.rows[action] = it }
                                            .alpha(if (drag?.action == action) 0f else 1f),
                                    highlighted = highlighted,
                                    onMove = { moveAction = action },
                                    onStartDrag = { y ->
                                        val row = layout.rows.getValue(action)
                                        val offset = (y - row.positionInRoot().y).coerceIn(0f, row.size.height.toFloat())
                                        drag = ToolbarDrag(action, y, offset)
                                    },
                                    onDrag = { y -> drag = drag?.copy(pointerY = y) },
                                    onDrop = {
                                        drag?.let { moving ->
                                            layout.target(moving.pointerY, groups, action)?.let { move(action, it) }
                                        }
                                        drag = null
                                    },
                                    onCancelDrag = { drag = null },
                                )
                            }
                        }
                    }
                }
            }
            val moving = drag
            if (moving != null) {
                ToolbarDropMarker(
                    position = {
                        // Re-evaluate screen coordinates when the list autoscrolls.
                        val y = layout.target(moving.pointerY, groups, moving.action)?.let { layout.insertionY(it, groups) }
                        val top = layout.viewport.top
                        if (y == null || top == null) -1_000_000 else (y - top).roundToInt() - markerOffset
                    },
                )
                Column(
                    Modifier
                        .offset { IntOffset(0, (moving.pointerY - (layout.viewport.top ?: 0f) - moving.grabOffset).roundToInt()) }
                        .graphicsLayer { rotationZ = -1.5f }
                        .background(foundation.colors.background, foundation.shapes.button)
                        .border(foundation.shapes.hairline, foundation.colors.emphasis, foundation.shapes.button)
                        .padding(horizontal = 8.dp),
                ) {
                    SettingsRow(
                        title = moving.action.title(context),
                        icon = moving.action.drawableRes,
                        trailing = { DragHandleIcon() },
                    )
                }
            }
        }
    }

    moveAction?.let { action ->
        val group = groups.entries.first { action in it.value }.key
        val items = groups.getValue(group)
        val index = items.indexOf(action)
        val groupChoices = MenuDisplayType.entries.filter { it != group }
        val choices =
            listOf(
                DialogChoice(stringResource(R.string.browse_columns_move_up), enabled = index > 0),
                DialogChoice(stringResource(R.string.browse_columns_move_down), enabled = index < items.lastIndex),
            ) + groupChoices.map { DialogChoice(stringResource(it.title)) }
        SingleChoiceDialog(
            title = action.title(context),
            choices = choices,
            selectedIndex = null,
            onSelect = {
                val drop =
                    when (it) {
                        0 -> ToolbarDrop(group, items[index - 1])
                        1 -> ToolbarDrop(group, items.getOrNull(index + 2))
                        else -> ToolbarDrop(groupChoices[it - 2], null)
                    }
                move(action, drop)
                moveAction = null
            },
            onDismissRequest = { moveAction = null },
        )
    }
}

@Composable
private fun ToolbarActionRow(
    action: ViewerAction,
    modifier: Modifier,
    highlighted: Boolean,
    onMove: () -> Unit,
    onStartDrag: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDrop: () -> Unit,
    onCancelDrag: () -> Unit,
) {
    val name = action.title(LocalContext.current)
    val coordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val start by rememberUpdatedState(onStartDrag)
    val drag by rememberUpdatedState(onDrag)
    val drop by rememberUpdatedState(onDrop)
    val cancel by rememberUpdatedState(onCancelDrag)
    SettingsRow(
        title = name,
        icon = action.drawableRes,
        modifier = modifier,
        highlighted = highlighted,
        trailing = {
            IconButton(
                onClick = onMove,
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates.value = it }
                        .pointerInput(action) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    coordinates.value
                                        ?.takeIf { it.isAttached }
                                        ?.localToRoot(offset)
                                        ?.y
                                        ?.let(start)
                                },
                                onVerticalDrag = { change, _ ->
                                    coordinates.value
                                        ?.takeIf { it.isAttached }
                                        ?.localToRoot(change.position)
                                        ?.y
                                        ?.let(drag)
                                },
                                onDragEnd = { drop() },
                                onDragCancel = { cancel() },
                            )
                        },
            ) {
                DragHandleIcon(stringResource(R.string.settings_toolbar_move_action, name))
            }
        },
    )
}

@Composable
private fun DragHandleIcon(description: String? = null) {
    Icon(
        painterResource(R.drawable.symbol_drag_indicator),
        contentDescription = description,
        tint = MaterialTheme.foundation.colors.onBackgroundMuted,
    )
}

@Composable
private fun ToolbarDropMarker(position: () -> Int) {
    val colors = MaterialTheme.foundation.colors
    Canvas(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, position()) }
            .height(16.dp)
            .clipToBounds()
            .background(colors.background),
    ) {
        val step = 8.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(colors.emphasis, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.dp.toPx())
            x += step
        }
    }
}
