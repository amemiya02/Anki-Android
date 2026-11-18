// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.descendants
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.IntentHandler
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.Consts
import com.ichi2.widget.AppWidgetId.Companion.getAppWidgetId
import com.ichi2.widget.cardanalysis.CardAnalysisWidget
import com.ichi2.widget.cardanalysis.CardAnalysisWidgetConfig
import com.ichi2.widget.cardanalysis.CardAnalysisWidgetPreferences
import com.ichi2.widget.deckpicker.DeckPickerWidget
import com.ichi2.widget.deckpicker.DeckPickerWidgetPreferences
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DeckWidgetUpdateTest : RobolectricTest() {
    @Test
    fun `the deck list fits the first selected Deck at minimum size and more Decks after resize`() =
        runTest {
            val first = addDeck("Japanese").also { addNoteToDeck(it, count = 2) }
            val second = addDeck("Chinese").also { addNoteToDeck(it, count = 1) }
            DeckPickerWidgetPreferences(targetContext).saveSelectedDecks(WIDGET_ID, listOf(first.toString(), second.toString()))

            val compact = targetContext.renderWidget(DeckPickerWidget(), width = 120, height = 50, id = WIDGET_ID)
            assertEquals(listOf("Japanese"), compact.labels().filter { it == "Japanese" || it == "Chinese" })
            assertTrue("3 due today" in compact.labels())

            val expanded = targetContext.renderWidget(DeckPickerWidget(), width = 320, height = 180, id = WIDGET_ID)
            assertEquals(listOf("Japanese", "Chinese"), expanded.labels().filter { it == "Japanese" || it == "Chinese" })
            expanded.tapWidgetText("Chinese")

            val opened = assertNotNull(shadowOf(targetContext as Application).nextStartedActivity)
            assertEquals(IntentHandler::class.java.name, opened.component?.className)
            assertEquals(second, opened.getLongExtra(IntentHandler.EXTRA_DECK_ID, 0))
            assertEquals(IntentHandler.LaunchType.REVIEW, IntentHandler.getLaunchType(opened))
        }

    @Test
    fun `a deleted selected Deck opens its own widget setup`() =
        runTest {
            CardAnalysisWidgetPreferences(targetContext).saveSelectedDeck(WIDGET_ID, 987654321)

            val widget = targetContext.renderWidget(CardAnalysisWidget(), width = 166, height = 166, id = WIDGET_ID)
            widget.tapWidgetText("This Deck is gone")

            val opened = assertNotNull(shadowOf(targetContext as Application).nextStartedActivity)
            assertEquals(CardAnalysisWidgetConfig::class.java.name, opened.component?.className)
            assertEquals(WIDGET_ID, opened.getAppWidgetId())
        }

    @Test
    fun `deck picker widget has a recovery state when the collection cannot be opened`() =
        runTest {
            DeckPickerWidgetPreferences(targetContext).saveSelectedDecks(WIDGET_ID, listOf(Consts.DEFAULT_DECK_ID.toString()))
            enableNullCollection()

            val widget = targetContext.renderWidget(DeckPickerWidget(), width = 320, height = 166, id = WIDGET_ID)
            assertTrue("Collection unavailable" in widget.labels())
        }

    @Test
    fun `one Deck widget has a recovery state when the collection cannot be opened`() =
        runTest {
            CardAnalysisWidgetPreferences(targetContext).saveSelectedDeck(WIDGET_ID, Consts.DEFAULT_DECK_ID)
            enableNullCollection()

            val widget = targetContext.renderWidget(CardAnalysisWidget(), width = 166, height = 166, id = WIDGET_ID)
            assertTrue("Collection unavailable" in widget.labels())
        }

    private fun View.labels() =
        (this as ViewGroup)
            .descendants
            .filterIsInstance<TextView>()
            .map { it.text.toString() }
            .toList()

    private fun View.tapWidgetText(text: String) {
        val label = (this as ViewGroup).descendants.filterIsInstance<TextView>().first { it.text.toString() == text }
        val button = generateSequence<View>(label) { it.parent as? View }.first { it.hasOnClickListeners() }
        assertTrue(button.performClick())
    }

    companion object {
        private val WIDGET_ID = AppWidgetId(1)
    }
}
