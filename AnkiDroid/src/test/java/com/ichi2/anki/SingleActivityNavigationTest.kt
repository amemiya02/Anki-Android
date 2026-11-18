// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.browser.CardBrowserPaneFragment
import com.ichi2.anki.browser.toIntent
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.mediacheck.MediaCheckFragment
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `a hosted browser search retains a visible parent for feedback`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            val search = BrowserDestination.Search(query = "is:new", allDecks = false).toIntent(deckPicker)
            deckPicker.startActivity(search)
            composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is CardBrowserPaneFragment }

            composeRule.onNodeWithText("Browse").assertIsDisplayed()
            assertTrue(deckPicker.findViewById<View>(R.id.root_layout).isShown)
            assertNull(shadowOf(deckPicker).nextStartedActivity)
        }

    @Test
    fun `shared text waits for Welcome then opens the Note Editor once`() {
        ensureCollectionLoadIsSynchronous()
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        setIntroductionSlidesShown(false)
        val shared = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "Text from another app")
        val editor = Intent(targetContext, NoteEditorActivity::class.java).putExtras(shared).setAction(shared.action)
        val incoming = DeckPicker.getEntryIntent(targetContext, editor, shared)
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, incoming)
        val startEmpty = composeRule.onNodeWithText(controller.get().getString(CommonString.paper_welcome_start_empty))
        startEmpty.assertIsDisplayed()
        controller.recreate()
        advanceRobolectricLooper()

        val host = controller.get()
        startEmpty.performClick()
        composeRule.waitUntil(10_000) { host.currentScreenFragment is NoteEditorPaneFragment }

        assertEquals(
            "Text from another app",
            (host.currentScreenFragment as NoteEditorPaneFragment).launchIntent.getStringExtra(Intent.EXTRA_TEXT),
        )
        host.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(host.getString(CommonString.discard)).performClick()
        composeRule.onAllNodesWithText("Decks").onFirst().assertIsDisplayed()
        assertNull(shadowOf(host).nextStartedActivity)
    }

    @Test
    fun `a second share returns to the existing host and preserves its previous screen`() {
        ensureCollectionLoadIsSynchronous()
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        setIntroductionSlidesShown(true)
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent())
        val deckPicker = controller.get()
        deckPicker.viewModel.awaitStartup()
        deckPicker.startActivity(MediaCheckFragment.getIntent(deckPicker))
        composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is MediaCheckFragment }

        val shared = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "A second share")
        val editor = Intent(targetContext, NoteEditorActivity::class.java).putExtras(shared).setAction(shared.action)
        val incoming = DeckPicker.getEntryIntent(targetContext, editor, shared)
        val host = deckPicker.packageManager.getActivityInfo(requireNotNull(incoming.component), 0)
        // A share starts in the sending app's task. Android must find the same DeckPicker across tasks.
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, host.launchMode)
        controller.pause().newIntent(incoming).resume()
        composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is NoteEditorPaneFragment }

        val noteEditor = deckPicker.currentScreenFragment as NoteEditorPaneFragment
        assertEquals("A second share", noteEditor.launchIntent.getStringExtra(Intent.EXTRA_TEXT))
        deckPicker.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(deckPicker.getString(CommonString.discard)).performClick()
        composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is MediaCheckFragment }
        assertNull(shadowOf(deckPicker).nextStartedActivity)
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
