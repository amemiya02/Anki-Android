// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.browser.CardBrowserPaneFragment
import com.ichi2.anki.browser.toIntent
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.ui.TransitionDirection
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity
import com.ichi2.anki.mediacheck.MediaCheckFragment
import com.ichi2.anki.noteeditor.toIntent
import com.ichi2.anki.pages.PageFragment
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
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
    fun `the launcher Browse shortcut enters the hosted browser`() {
        ensureCollectionLoadIsSynchronous()
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        setIntroductionSlidesShown(true)
        NavigationDrawerActivity.enablePostShortcut(targetContext)
        val shortcut = ShortcutManagerCompat.getDynamicShortcuts(targetContext).single { it.id == "cardBrowserShortcutId" }
        val entry = shortcut.intents.single()
        assertEquals(IntentHandler.LaunchType.OPEN_BROWSER, IntentHandler.getLaunchType(entry))

        val launch =
            Robolectric.buildActivity(IntentHandler::class.java, entry).use { controller ->
                val handler = controller.create().get()
                shadowOf(handler).nextStartedActivity
            }
        assertEquals(DeckPicker::class.java.name, launch.component?.className)
        val host = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, launch)
        composeRule.waitUntil(10_000) {
            advanceRobolectricLooper()
            host.currentScreenFragment is CardBrowserPaneFragment
        }
        composeRule.onNodeWithText("Browse").assertIsDisplayed()
        assertNull(shadowOf(host).nextStartedActivity)
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
        composeRule.waitUntil(10_000) {
            advanceRobolectricLooper()
            host.currentScreenFragment is NoteEditorPaneFragment
        }

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
    fun `editing an existing note opens the bundled change note type page`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            val note = addBasicNote("Example prompt", "Example answer")
            val editor = NoteEditorDestination.EditSelection(note.cardIds(col), TransitionDirection.DEFAULT)
            deckPicker.startActivity(editor.toIntent(deckPicker))
            composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is NoteEditorPaneFragment }

            composeRule.onNodeWithText(note.notetype.name).performClick()
            advanceRobolectricLooper()

            val page = deckPicker.currentScreenFragment as? PageFragment
            assertEquals(
                "https://appassets.androidplatform.net/change-notetype/${note.noteTypeId}",
                (page?.webViewLayout?.getChildAt(0) as? android.webkit.WebView)?.url?.substringBefore('#'),
            )
        }

    @Test
    fun `instant share dispatches to the existing host and opens the full editor there`() =
        withDeckPicker(deckCount = 1) {
            val shared =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Shared instant note")
                }
            val entry = Robolectric.buildActivity(InstantNoteEditorActivity::class.java, shared).use { controller ->
                val handler = controller.create().get()
                assertTrue(handler.isFinishing)
                shadowOf(handler).nextStartedActivity
            }
            assertEquals(DeckPicker::class.java.name, entry.component?.className)
            val controller = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, entry)
            val host = controller.get()
            val openEditor = composeRule.onNodeWithContentDescription(getResourceString(CommonString.open_note_editor))
            openEditor.assertIsDisplayed().performClick()
            composeRule.waitUntil(10_000) { host.currentScreenFragment is NoteEditorPaneFragment }
            assertEquals("Shared instant note", (host.currentScreenFragment as NoteEditorPaneFragment).launchIntent.getStringExtra(Intent.EXTRA_TEXT))
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
