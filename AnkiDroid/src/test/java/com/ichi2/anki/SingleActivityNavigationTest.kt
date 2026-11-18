// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.mediacheck.MediaCheckFragment
import com.ichi2.anki.ui.internationalization.trCase
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
            val mediaCheck = composeRule.onNodeWithText(trCase().checkMediaTitle())
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
    fun `first run opens Welcome and continues to Decks in the same activity`() {
        setIntroductionSlidesShown(false)
        val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent())
        assertNull(shadowOf(deckPicker).nextStartedActivity)
        composeRule.onNodeWithText(deckPicker.getString(CommonString.paper_welcome_start_empty)).performClick()
        advanceRobolectricLooper()

        composeRule.onNodeWithText("Decks").assertIsDisplayed()
        assertFalse(deckPicker.isFinishing)
        assertNull(shadowOf(deckPicker).nextStartedActivity)
    }
}
