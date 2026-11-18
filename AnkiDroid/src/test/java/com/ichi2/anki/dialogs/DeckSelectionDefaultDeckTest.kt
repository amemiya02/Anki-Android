// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.anki.model.SelectableDeck
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DeckSelectionDefaultDeckTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `renamed default with cards only in a subdeck can be selected`() {
        col.decks.rename(col.decks.getDefault(), "Words")
        addNoteToDeck(addDeck("Words::Vocabulary"))

        val activity = startRegularActivity<AnkiActivity>()
        var selectedDeck: SelectableDeck? = null
        activity.registerDeckSelectedHandler { selectedDeck = it }
        activity.startDeckSelection(skipEmptyDefault = true)
        advanceRobolectricLooper()

        compose.onNodeWithText("Words").performClick()
        assertEquals(SelectableDeck.Deck(DEFAULT_DECK_ID, "Words"), selectedDeck)
    }
}
