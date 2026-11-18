// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.widget.deckpicker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.ichi2.anki.BaseAnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.dialogs.DeckSelectionDialog
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.startup.ensureStorageIsReady
import com.ichi2.anki.widgets.R
import com.ichi2.anki.withProgress
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.widget.AppWidgetId.Companion.INVALID_APPWIDGET_ID
import com.ichi2.widget.AppWidgetId.Companion.getAppWidgetId
import com.ichi2.widget.AppWidgetId.Companion.updateWidget
import kotlin.math.abs
import com.ichi2.anki.common.android.R as CommonR

private const val MAX_DECKS = 5

/** Configures the Deck list shown by a single launcher widget. */
class DeckPickerWidgetConfig : BaseAnkiActivity() {
    private var widgetId = INVALID_APPWIDGET_ID
    private lateinit var preferences: DeckPickerWidgetPreferences
    private var allDecks by mutableStateOf<List<SelectableDeck.Deck>?>(null)
    private var selected by mutableStateOf(emptyList<SelectableDeck.Deck>())
    private var savedIds = emptyList<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        if (!ensureStorageIsReady()) return
        widgetId = intent.getAppWidgetId()
        if (widgetId == INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED, Intent().updateWidget(widgetId))
        enableEdgeToEdge()
        preferences = DeckPickerWidgetPreferences(this)
        savedIds = preferences.getSelectedDeckIdsFromPreferences(widgetId).toList()

        setContentView(
            CoordinatorLayout(this).apply {
                id = CommonR.id.root_layout
                addView(
                    ComposeView(context).apply {
                        setContent {
                            AnkiDroidTheme {
                                BackHandler(onBack = ::close)
                                val decks = allDecks
                                DeckListWidgetSetup(
                                    selected = selected,
                                    canAdd = decks != null && selected.size < minOf(MAX_DECKS, decks.size),
                                    onClose = ::close,
                                    onAdd = ::selectDecks,
                                    onRemove = { id -> selected = selected.filterNot { it.deckId == id } },
                                    onMove = ::moveDeck,
                                    onSave = ::save,
                                )
                            }
                        }
                    },
                    CoordinatorLayout.LayoutParams(-1, -1),
                )
            },
        )
        registerDeckSelectedHandler { candidate -> addDeck(candidate as? SelectableDeck.Deck) }
        launchCatchingTask {
            withProgress {
                val decks = SelectableDeck.fromCollection(includeFiltered = true, skipEmptyDefault = true)
                if (decks.isEmpty()) {
                    showThemedToast(this@DeckPickerWidgetConfig, CommonString.empty_collection_state_in_widget, false)
                    finish()
                    return@withProgress
                }
                val byId = decks.associateBy(SelectableDeck.Deck::deckId)
                savedIds = savedIds.filter { it in byId }
                val restored = savedInstanceState?.getLongArray(STATE_DECKS)?.toList() ?: savedIds
                selected = restored.distinct().mapNotNull(byId::get).take(MAX_DECKS)
                allDecks = decks
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (allDecks != null) outState.putLongArray(STATE_DECKS, selected.map { it.deckId }.toLongArray())
    }

    private fun selectDecks() {
        startDeckSelection(
            title = getString(CommonString.select_decks_title),
            allowMultipleSelection = true,
            allowAll = false,
            skipEmptyDefault = true,
        )
    }

    private fun addDeck(deck: SelectableDeck.Deck?) {
        if (deck == null || selected.size >= MAX_DECKS) return
        if (selected.any { it.deckId == deck.deckId }) {
            showThemedToast(this, CommonString.deck_already_selected_message, false)
            return
        }
        allDecks?.let { available ->
            if (available.none { it.deckId == deck.deckId }) allDecks = available + deck
        }
        selected = selected + deck
        if (selected.size == MAX_DECKS || selected.size == allDecks?.size) {
            (supportFragmentManager.findFragmentByTag(DeckSelectionDialog.TAG) as? DeckSelectionDialog)?.dismiss()
        }
    }

    private fun moveDeck(
        from: Int,
        to: Int,
    ) {
        if (from == to || from !in selected.indices || to !in selected.indices) return
        selected = selected.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun close() {
        if (allDecks == null || selected.map { it.deckId } == savedIds) {
            finish()
        } else {
            DiscardChangesDialog.showDialog(this, positiveMethod = ::finish)
        }
    }

    private fun save() {
        preferences.saveSelectedDecks(widgetId, selected.map { it.deckId.toString() })
        DeckPickerWidget.updateWidget(this, widgetId)
        setResult(RESULT_OK, Intent().updateWidget(widgetId))
        finish()
    }

    companion object {
        private const val STATE_DECKS = "widget_selected_decks"
    }
}

@Composable
private fun DeckListWidgetSetup(
    selected: List<SelectableDeck.Deck>,
    canAdd: Boolean,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSave: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    val listState = rememberLazyListState()
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val drop by rememberUpdatedState {
        val from = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedId }
        if (from != null) {
            val center = from.offset + from.size / 2f + dragOffset
            val to =
                listState.layoutInfo.visibleItemsInfo
                    .filter { it.key is Long }
                    .minByOrNull { abs(center - (it.offset + it.size / 2f)) }
            onMove(selected.indexOfFirst { it.deckId == from.key }, selected.indexOfFirst { it.deckId == to?.key })
        }
        draggedId = null
        dragOffset = 0f
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(paper.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = paper.spacing.screenMargin),
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(CommonR.drawable.symbol_arrow_back), stringResource(R.string.widget_config_back))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            item {
                Text(
                    stringResource(R.string.widget_list_title),
                    Modifier.semantics { heading() },
                    style = paper.type.screenTitle,
                    color = paper.colors.onBackground,
                )
                Text(
                    stringResource(R.string.widget_config_lead, MAX_DECKS),
                    Modifier.padding(top = 8.dp, bottom = 18.dp),
                    style = paper.type.lead,
                    color = paper.colors.onBackgroundMuted,
                )
                Text(
                    stringResource(R.string.widget_config_chosen, selected.size, MAX_DECKS),
                    Modifier.padding(bottom = 8.dp),
                    style = paper.type.caption,
                    color = paper.colors.onBackgroundMuted,
                )
                HorizontalDivider(color = paper.colors.outline)
            }
            itemsIndexed(selected, key = { _, deck -> deck.deckId }) { index, deck ->
                val moving = deck.deckId == draggedId
                val name = deck.name.replace("::", " › ")
                Column(
                    Modifier
                        .zIndex(if (moving) 1f else 0f)
                        .graphicsLayer { if (moving) translationY = dragOffset }
                        .background(paper.colors.background),
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = paper.spacing.touchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReorderDeck(
                            name = name,
                            canMoveUp = index > 0,
                            canMoveDown = index < selected.lastIndex,
                            onMove = { onMove(index, index + it) },
                            modifier =
                                Modifier.draggable(
                                    state = rememberDraggableState { delta -> dragOffset += delta },
                                    orientation = Orientation.Vertical,
                                    enabled = selected.size > 1,
                                    onDragStarted = {
                                        draggedId = deck.deckId
                                        dragOffset = 0f
                                    },
                                    onDragStopped = { drop() },
                                ),
                        )
                        Text(name, Modifier.weight(1f), color = paper.colors.onBackground, style = paper.type.rowTitle)
                        IconButton(onClick = { onRemove(deck.deckId) }) {
                            Icon(
                                painterResource(CommonR.drawable.symbol_close),
                                stringResource(R.string.widget_config_remove, name),
                                tint = paper.colors.onBackgroundMuted,
                            )
                        }
                    }
                    HorizontalDivider(color = paper.colors.outline)
                }
            }
            if (canAdd) {
                item {
                    TextButton(onClick = onAdd, modifier = Modifier.heightIn(min = paper.spacing.touchTarget)) {
                        Icon(painterResource(CommonR.drawable.symbol_add), contentDescription = null)
                        Text(
                            stringResource(R.string.widget_config_add),
                            Modifier.padding(start = 10.dp),
                            style = paper.type.rowTitle,
                        )
                    }
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = paper.spacing.primaryButton),
            shape = paper.shapes.button,
        ) {
            Text(stringResource(CommonString.save))
        }
    }
}

@Composable
private fun ReorderDeck(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    modifier: Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val paper = MaterialTheme.foundation
    val moveUp = stringResource(R.string.widget_config_move_up)
    val moveDown = stringResource(R.string.widget_config_move_down)
    Box(modifier) {
        IconButton(
            onClick = { menu = true },
            enabled = canMoveUp || canMoveDown,
            modifier =
                Modifier.size(paper.spacing.touchTarget).semantics {
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
                painterResource(R.drawable.symbol_drag_indicator_widget),
                stringResource(R.string.widget_config_reorder, name),
                tint = paper.colors.onBackgroundMuted,
            )
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = paper.shapes.button,
            shadowElevation = 0.dp,
            containerColor = paper.colors.background,
        ) {
            if (canMoveUp) {
                DropdownMenuItem(text = { Text(moveUp) }, onClick = {
                    menu = false
                    onMove(-1)
                })
            }
            if (canMoveDown) {
                DropdownMenuItem(text = { Text(moveDown) }, onClick = {
                    menu = false
                    onMove(1)
                })
            }
        }
    }
}
