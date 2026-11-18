// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.anki.widget.deckpicker

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.DeckSelectionDialog
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.widgets.R
import com.ichi2.widget.AppWidgetId
import com.ichi2.widget.AppWidgetId.Companion.updateWidget
import com.ichi2.widget.deckpicker.DeckPickerWidgetConfig
import com.ichi2.widget.deckpicker.DeckPickerWidgetPreferences
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DeckPickerWidgetConfigTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `setup saves selected Decks in the displayed order`() =
        runTest {
            val first = addDeck("Japanese")
            val second = addDeck("Chinese")
            val preferences = DeckPickerWidgetPreferences(targetContext)
            val activity = openSetup()
            advanceUntilIdle()

            compose.onNodeWithText(activity.getString(CommonString.save)).assertIsNotEnabled()
            for (deck in listOf(SelectableDeck.Deck(first, "Japanese"), SelectableDeck.Deck(second, "Chinese"))) {
                activity.supportFragmentManager.setFragmentResult(
                    DeckSelectionDialog.REQUEST_SELECT_DECK,
                    bundleOf(DeckSelectionDialog.RESULT_SELECTED_DECK to deck),
                )
            }

            compose.onNodeWithText(activity.getString(R.string.widget_config_chosen, 2, 5)).assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.widget_config_reorder, "Japanese")).performClick()
            compose.onNodeWithText(activity.getString(R.string.widget_config_move_down)).performClick()
            compose.onNodeWithText(activity.getString(CommonString.save)).performClick()

            assertEquals(listOf(second, first), preferences.getSelectedDeckIdsFromPreferences(widgetId).toList())
            assertTrue(activity.isFinishing)
        }

    @Test
    fun `reconfiguration restores existing choices and allows a Deck to be removed`() =
        runTest {
            val first = addDeck("Japanese")
            val second = addDeck("Chinese")
            val preferences = DeckPickerWidgetPreferences(targetContext)
            preferences.saveSelectedDecks(widgetId, listOf(first.toString(), second.toString()))
            val activity = openSetup()
            advanceUntilIdle()

            compose.onNodeWithText("Japanese").assertIsDisplayed()
            compose.onNodeWithText("Chinese").assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.widget_config_remove, "Japanese")).performClick()
            compose.onNodeWithText(activity.getString(CommonString.save)).performClick()

            assertEquals(listOf(second), preferences.getSelectedDeckIdsFromPreferences(widgetId).toList())
        }

    private fun openSetup() =
        startActivityNormallyOpenCollectionWithIntent(
            DeckPickerWidgetConfig::class.java,
            Intent(targetContext, DeckPickerWidgetConfig::class.java).updateWidget(widgetId),
        )

    companion object {
        private val widgetId = AppWidgetId(1)
    }
}
