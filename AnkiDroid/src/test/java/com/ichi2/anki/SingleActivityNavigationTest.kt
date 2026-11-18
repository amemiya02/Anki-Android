// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.mediacheck.MediaCheckFragment
import com.ichi2.anki.ui.internationalization.sentenceCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SingleActivityNavigationTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `media check opens in the existing activity and Back returns to Decks`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            val deck = composeRule.onNodeWithText("Test Deck 0")
            val mediaCheck = composeRule.onNodeWithText(with(deckPicker) { TR.sentenceCase.checkMediaTitle })
            deck.assertIsDisplayed()

            deckPicker.startActivity(MediaCheckFragment.getIntent(deckPicker))
            advanceRobolectricLooper()

            assertNull(shadowOf(deckPicker).nextStartedActivity)
            mediaCheck.assertIsDisplayed()
            deck.assertDoesNotExist()

            deckPicker.onBackPressedDispatcher.onBackPressed()
            advanceRobolectricLooper()

            deck.assertIsDisplayed()
            mediaCheck.assertDoesNotExist()
            assertFalse(deckPicker.isFinishing)
        }

    @Test
    fun `shared text waits for Welcome then opens the Note Editor once`() {
        setIntroductionSlidesShown(false)
        val shared = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "Text from another app")
        val editor = Intent(targetContext, NoteEditorActivity::class.java).putExtras(shared).setAction(shared.action)
        val incoming = DeckPicker.getEntryIntent(targetContext, editor, shared)
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, incoming)
        controller.recreate()
        advanceRobolectricLooper()

        val host = controller.get()
        composeRule.onNodeWithText(host.getString(CommonString.paper_welcome_start_empty)).performClick()
        advanceRobolectricLooperUntil { host.currentScreenFragment is NoteEditorPaneFragment }

        assertEquals(
            "Text from another app",
            (host.currentScreenFragment as NoteEditorPaneFragment).launchIntent.getStringExtra(Intent.EXTRA_TEXT),
        )
        assertNull(shadowOf(host).nextStartedActivity)
    }

    @Test
    fun `first run opens Welcome and continues to Decks in the same activity`() {
        setIntroductionSlidesShown(false)
        val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent())
        assertNull(shadowOf(deckPicker).nextStartedActivity)
        composeRule.onNodeWithText(deckPicker.getString(CommonString.paper_welcome_start_empty)).performClick()
        advanceRobolectricLooper()

        composeRule.onAllNodesWithText("Decks").onFirst().assertIsDisplayed()
        assertFalse(deckPicker.isFinishing)
        assertNull(shadowOf(deckPicker).nextStartedActivity)
    }
}
