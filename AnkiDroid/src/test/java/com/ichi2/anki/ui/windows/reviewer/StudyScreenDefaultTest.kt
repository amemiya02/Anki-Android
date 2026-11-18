// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.content.Intent
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.IntentHandler
import com.ichi2.anki.NavigationDrawerActivity
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.destinations.ReviewDeckDestination
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.toIntent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StudyScreenDefaultTest : RobolectricTest() {
    @Test
    fun `studying opens the study screen when the old screen was selected`() {
        disableFormerReviewerPreference()

        assertStudyScreen(ReviewDeckDestination.CurrentDeck.toIntent(targetContext))
    }

    @Test
    fun `external deck launch selects the requested deck and opens the study screen`() {
        disableFormerReviewerPreference()
        val deckId = addDeck("Requested deck")
        val intent = ReviewDeckDestination.ExternalLaunch(deckId).toIntent(targetContext)

        val handler = startActivityNormallyOpenCollectionWithIntent(IntentHandler::class.java, intent)

        assertEquals(deckId, col.decks.selected())
        assertReviewStack(handler)
    }

    @Test
    fun `saved launcher shortcut opens the current deck`() {
        val deckId = addDeck("Current deck")
        col.decks.select(deckId)
        val intent = formerReviewerIntent().putExtra(NavigationDrawerActivity.EXTRA_STARTED_WITH_SHORTCUT, true)

        val handler = startActivityNormallyOpenCollectionWithIntent(IntentHandler::class.java, intent)

        assertEquals(deckId, col.decks.selected())
        val study = assertReviewStack(handler)
        assertTrue(study.getBooleanExtra(NavigationDrawerActivity.EXTRA_STARTED_WITH_SHORTCUT, false))
    }

    @Test
    fun `former external reviewer link still opens a requested deck`() {
        val deckId = addDeck("Requested deck")
        val intent = formerReviewerIntent().putExtra("deckId", deckId)

        val handler = startActivityNormallyOpenCollectionWithIntent(IntentHandler::class.java, intent)

        assertEquals(deckId, col.decks.selected())
        assertReviewStack(handler)
    }

    @Test
    fun `former external reviewer link falls back to the current deck when its deck has been deleted`() {
        val deckId = addDeck("Current deck")
        col.decks.select(deckId)
        val intent = formerReviewerIntent().putExtra("deckId", Long.MAX_VALUE)

        val handler = startActivityNormallyOpenCollectionWithIntent(IntentHandler::class.java, intent)

        assertEquals(deckId, col.decks.selected())
        assertReviewStack(handler)
    }

    @Test
    fun `published review shortcut keeps its launcher and opens the study screen`() {
        disableFormerReviewerPreference()
        NavigationDrawerActivity.enablePostShortcut(targetContext)

        val shortcut =
            ShortcutManagerCompat.getDynamicShortcuts(targetContext).single { it.id == "reviewCardsShortcutId" }
        val intent = shortcut.intents.single()
        assertEquals(IntentHandler.LaunchType.REVIEW, IntentHandler.getLaunchType(intent))

        val handler = startActivityNormallyOpenCollectionWithIntent(IntentHandler::class.java, intent)

        val study = assertReviewStack(handler)
        assertTrue(study.getBooleanExtra(NavigationDrawerActivity.EXTRA_STARTED_WITH_SHORTCUT, false))
    }

    private fun formerReviewerIntent(): Intent = Intent(Intent.ACTION_VIEW).setClassName(targetContext, "com.ichi2.anki.Reviewer")

    private fun disableFormerReviewerPreference() {
        getPreferences().edit { putBoolean("newReviewerOptions", false) }
    }

    private fun assertReviewStack(handler: IntentHandler): Intent {
        val shadow = shadowOf(handler.application)
        val study = assertHostedStudyScreen(assertNotNull(shadow.nextStartedActivity))
        assertNull(shadow.nextStartedActivity)
        return study
    }

    private fun assertHostedStudyScreen(intent: Intent): Intent {
        assertEquals(DeckPicker::class.java.name, intent.component?.className)
        val study = assertNotNull(IntentCompat.getParcelableExtra(intent, DeckPicker.EXTRA_ENTRY_SCREEN, Intent::class.java))
        assertStudyScreen(study)
        return study
    }

    private fun assertStudyScreen(intent: Intent) {
        assertEquals(CardViewerActivity::class.java.name, intent.component?.className)
        assertEquals(ReviewerFragment::class.java.name, intent.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME))
    }
}
