// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.widget.cardanalysis

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.BundleCompat
import com.ichi2.anki.BaseAnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.canUserAccessDeck
import com.ichi2.anki.common.android.AnkiBroadcastReceiver
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.ext.unregisterReceiverSilently
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.isCollectionEmpty
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.startup.ensureStorageIsReady
import com.ichi2.anki.ui.internationalization.trCase
import com.ichi2.anki.widgets.R
import com.ichi2.anki.withProgress
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.widget.AppWidgetId.Companion.INVALID_APPWIDGET_ID
import com.ichi2.widget.AppWidgetId.Companion.getAppWidgetId
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

/**
 * Configuration activity for [CardAnalysisWidget]. Only allows selecting a deck.
 *
 * Behavior:
 *  - shows the selected deck name (if any) and a button to trigger the
 *    deck selection dialog
 *  - when the user first adds the widget this activity will start with the deck selection dialog
 *    already open, if there is a deck selected then, the activity will start without the selection
 *    dialog
 *  - storing the user selection is done automatically on every deck change
 *  - handles user not selecting anything(widget also handles this state)
 *  - finishes immediately when the collection is empty and shows a toast('Collection is empty')
 *  - shows loading state if querying the collection takes time
 *
 * @see CardAnalysisWidget
 * @see CardAnalysisWidgetPreferences
 */
class CardAnalysisWidgetConfig : BaseAnkiActivity() {
    private var appWidgetId = INVALID_APPWIDGET_ID
    private var deck: SelectableDeck.Deck? by mutableStateOf(null)
    private lateinit var preferences: CardAnalysisWidgetPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)

        if (!ensureStorageIsReady()) {
            return
        }
        enableEdgeToEdge()
        preferences = CardAnalysisWidgetPreferences(this)
        appWidgetId = intent.getAppWidgetId()
        if (appWidgetId == INVALID_APPWIDGET_ID) {
            Timber.v("Invalid App Widget ID")
            finish()
            return
        }
        setContentView(
            CoordinatorLayout(this).apply {
                id = CommonR.id.root_layout
                addView(
                    ComposeView(context).apply {
                        setContent {
                            AnkiDroidTheme { SingleDeckWidgetSetup(deck?.name, ::showDeckSelectionDialog, ::close) }
                        }
                    },
                    CoordinatorLayout.LayoutParams(-1, -1),
                )
            },
        )
        if (savedInstanceState != null) {
            deck =
                BundleCompat.getParcelable(
                    savedInstanceState,
                    STATE_DECK,
                    SelectableDeck.Deck::class.java,
                )
        } else {
            loadContent()
        }
        registerReceiver(
            widgetRemovedReceiver,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_DELETED),
        )
        registerDeckSelectedHandler(action = ::onDeckSelected)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_DECK, deck)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiverSilently(widgetRemovedReceiver)
    }

    private fun onDeckSelected(deck: SelectableDeck?) {
        if (deck == null || deck !is SelectableDeck.Deck?) {
            showThemedToast(this, CommonString.something_wrong, false)
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        // if the deck was null before the selection then the widget was just added so update the
        // widget and finish
        val shouldClose = this.deck == null
        this.deck = deck
        preferences.saveSelectedDeck(appWidgetId, deck.deckId)
        CardAnalysisWidget.updateWidget(this, appWidgetId)
        if (shouldClose) {
            close()
        }
    }

    private fun loadContent() {
        launchCatchingTask {
            withProgress {
                if (isCollectionEmpty()) {
                    Timber.w("CardAnalysisWidgetConfig: collection is empty")
                    showThemedToast(
                        this@CardAnalysisWidgetConfig,
                        CommonString.no_cards_placeholder_title,
                        false,
                    )
                    finish()
                    return@withProgress
                }
                val selectedDeckId = preferences.getSelectedDeckIdFromPreferences(appWidgetId)
                if (selectedDeckId == null || !canUserAccessDeck(selectedDeckId)) {
                    showDeckSelectionDialog()
                } else {
                    deck = SelectableDeck.Deck.fromId(selectedDeckId)
                }
            }
        }
    }

    private fun showDeckSelectionDialog() {
        startDeckSelection(
            title = getString(CommonString.select_deck_title),
            allowAll = false,
            skipEmptyDefault = true,
        )
    }

    private fun close() {
        val intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId.id)
        setResult(RESULT_OK, intent)
        finish()
    }

    /** BroadcastReceiver to handle widget removal. */
    private val widgetRemovedReceiver =
        object : AnkiBroadcastReceiver() {
            override fun onReceiveBroadcast(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action != AppWidgetManager.ACTION_APPWIDGET_DELETED) {
                    return
                }

                val appWidgetId = intent.getAppWidgetId()
                if (appWidgetId == INVALID_APPWIDGET_ID) {
                    return
                }

                preferences.deleteDeckData(appWidgetId)
            }
        }

    companion object {
        private const val STATE_DECK = "key_deck"
    }
}

@Composable
internal fun SingleDeckWidgetSetup(
    selectedDeck: String?,
    onSelect: () -> Unit,
    onDone: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(paper.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = paper.spacing.screenMargin, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.widget_one_deck_title),
                Modifier.semantics { heading() },
                color = paper.colors.onBackground,
                style = paper.type.screenTitle,
            )
            HorizontalDivider(color = paper.colors.outline)
            selectedDeck?.let { Text(it, color = paper.colors.onBackground, style = paper.type.rowTitle) }
            TextButton(onClick = onSelect) { Text(trCase().selectDeck()) }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = paper.spacing.primaryButton),
            shape = paper.shapes.button,
        ) { Text(stringResource(CommonString.multimedia_editor_field_editing_done)) }
    }
}
