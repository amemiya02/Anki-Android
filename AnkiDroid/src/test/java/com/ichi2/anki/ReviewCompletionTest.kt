// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.content.edit
import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.NavigationDrawerActivity.Companion.EXTRA_STARTED_WITH_SHORTCUT
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.study.FinishedFragment
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment.Companion.RESULT_NO_MORE_CARDS
import com.ichi2.testutils.BackupManagerTestUtilities
import kotlinx.coroutines.test.advanceUntilIdle
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(ParameterizedRobolectricTestRunner::class)
class ReviewCompletionTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @ParameterizedRobolectricTestRunner.Parameter(0)
    @JvmField
    var fromShortcut = false

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun `finished parent with cards only in its child still shows completion`() {
        val child = addDeck("Parent::Child")
        col.setDeck(listOf(addBasicNote().firstCard().id), child)
        col.decks.select(col.decks.id("Parent"))

        completeReview {
            col.sched.answerCard(assertNotNull(col.sched.card), Rating.EASY)
            assertEquals(0, col.sched.totalCount())
            assertEquals(1, col.decks.cardCount(child, includeSubdecks = true))
        }
    }

    @Test
    fun `filtered deck emptied by the final review still shows completion`() {
        addBasicNote()
        val filtered = addDynamicDeck("Filtered", "")

        completeReview {
            col.sched.answerCard(assertNotNull(col.sched.card), Rating.EASY)
            assertEquals(0, col.decks.cardCount(filtered, includeSubdecks = true))
        }
    }

    @Test
    fun `learning later still shows completion`() {
        col.config.set("collapseTime", 0)
        val config = col.decks.configDictForDeckId(DEFAULT_DECK_ID)
        config.new.delays = JSONArray(listOf(1))
        col.decks.save(config)
        addBasicNote()

        completeReview {
            col.sched.answerCard(assertNotNull(col.sched.card), Rating.AGAIN)
            assertNull(col.sched.card)
        }
    }

    private fun completeReview(finishCards: () -> Unit) {
        if (fromShortcut) {
            runTest {
                finishCards()
                val selected = col.decks.selected()
                val undoStatus = col.undoStatus()
                val intent = ReviewerFragment.getIntent(targetContext).putExtra(EXTRA_STARTED_WITH_SHORTCUT, true)
                val reviewer = startActivityNormallyOpenCollectionWithIntent(CardViewerActivity::class.java, intent)
                val fragment = reviewer.fragment as ReviewerFragment
                // Robolectric does not run the WebView page load callback.
                fragment.viewModel.onPageFinished(false)
                advanceUntilIdle()
                advanceRobolectricLooper()

                assertTrue(reviewer.isFinishing)
                assertEquals(RESULT_NO_MORE_CARDS, shadowOf(reviewer).resultCode)
                assertFinishedScreenIntent(shadowOf(reviewer).nextStartedActivity)
                assertEquals(selected, col.decks.selected())
                assertEquals(undoStatus, col.undoStatus())
            }
        } else {
            BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
            getPreferences().edit { putBoolean("backupPromptDisabled", true) }
            deckPicker {
                advanceRobolectricLooper()
                val shadow = shadowOf(this)
                shadow.clearNextStartedActivities()
                assertTrue(onKeyUp(KeyEvent.KEYCODE_S, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_S)))
                compose.waitUntil(10_000) {
                    advanceRobolectricLooper()
                    currentScreenFragment is ReviewerFragment
                }
                val reviewer = assertNotNull(currentScreenFragment)
                finishCards()
                val selected = col.decks.selected()
                val undoStatus = col.undoStatus()

                reviewer.finishScreen(RESULT_NO_MORE_CARDS)

                compose.waitUntil(10_000) {
                    advanceRobolectricLooper()
                    currentScreenFragment is FinishedFragment
                }
                assertNull(shadow.nextStartedActivity)
                assertEquals(selected, col.decks.selected())
                assertEquals(undoStatus, col.undoStatus())
            }
        }
    }

    private fun assertFinishedScreenIntent(intent: Intent?) {
        assertEquals(
            FinishedFragment::class.java.name,
            assertNotNull(intent).getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME),
        )
    }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "shortcut = {0}")
        @JvmStatic
        fun configurations() = listOf(false, true).map { shortcut -> arrayOf(shortcut) }
    }
}
