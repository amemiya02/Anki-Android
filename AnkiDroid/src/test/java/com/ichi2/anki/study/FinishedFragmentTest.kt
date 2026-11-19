// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki.study

import android.app.Activity
import android.os.Bundle
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.StudyOptionsActivity
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.CustomStudyAction
import com.ichi2.anki.ioDispatcher
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.pages.DeckOptions
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.AnkiFragmentScenario
import com.ichi2.testutils.EmptyAnkiActivity
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.launchFragmentInContainer
import com.ichi2.testutils.saveState
import com.ichi2.testutils.uninitializeField
import com.ichi2.testutils.withFragment
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.compose.ui.test.isDialog as isPaperDialog
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class FinishedFragmentTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `finished screen back button clears the status bar and side cutout`() =
        withDeckPicker(deckCount = 1) { host ->
            host.startActivity(FinishedFragment.getIntent(host))
            composeRule.waitUntil(10_000) { host.currentScreenFragment is FinishedFragment }
            val back = composeRule.onNodeWithContentDescription(host.getString(androidx.appcompat.R.string.abc_action_bar_up_description))
            back.assertIsDisplayed()
            host.dispatchInsets(cutoutLeft = 32.dp, cutoutTop = 36.dp)
            advanceRobolectricLooper()
            val bounds = back.fetchSemanticsNode().boundsInWindow
            assertTrue(bounds.top >= 36.dp.toPx(host), "Finished toolbar overlaps the status bar or top cutout: $bounds")
            assertTrue(bounds.left >= 32.dp.toPx(host), "Finished back button overlaps the side cutout: $bounds")
        }

    @Test
    fun `unburying returns to studying the Deck`() =
        runTest {
            val note = addBasicNote()
            col.sched.buryCards(listOf(note.firstCard().id))

            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                val activity = scenario.withFragment { requireActivity() }
                advanceUntilIdle()

                clickText(targetContext.getString(CommonR.string.finished_unbury))
                advanceUntilIdle()
                shadowOf(Looper.getMainLooper()).idle()

                assertEquals(CardViewerActivity::class.java.name, shadowOf(activity).nextStartedActivity?.component?.className)
                assertTrue(activity.isFinishing)
                assertNotNull(col.sched.card, "the Card is no longer buried")
            }
        }

    @Test
    fun `back to decks closes the screen`() =
        runTest {
            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                val activity = scenario.withFragment { requireActivity() }
                advanceUntilIdle()

                clickText(targetContext.getString(CommonR.string.finished_back_to_decks))

                assertTrue(activity.isFinishing)
                assertNull(shadowOf(activity).nextStartedActivity)
            }
        }

    @Test
    fun `custom study is reachable`() =
        runTest {
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { new.perDay = 0 }
            addBasicNote()

            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                advanceUntilIdle()

                clickText("Custom study")
                shadowOf(Looper.getMainLooper()).idle()
                advanceUntilIdle()

                composeRule.onNodeWithText(TR.customStudyIncreaseTodaysNewCardLimit()).assertIsDisplayed()
            }
        }

    @Test
    fun `deck options are reachable`() =
        runTest {
            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                val activity = scenario.withFragment { requireActivity() }
                advanceUntilIdle()

                clickText("Deck options")
                advanceUntilIdle()
                shadowOf(Looper.getMainLooper()).idle()

                val next = assertNotNull(shadowOf(activity).nextStartedActivity)
                assertEquals(DeckOptions::class.java.name, next.getStringExtra(com.ichi2.anki.SingleFragmentActivity.EXTRA_FRAGMENT_NAME))
            }
        }

    @Test
    fun `a filtered Deck offers no custom study`() =
        runTest {
            addBasicNote()
            val filtered = addDynamicDeck("Filtered", "")
            withCol { sched.emptyFilteredDeck(filtered) }

            launchFragmentInContainer<FinishedFragment>().use {
                advanceUntilIdle()
                composeRule.waitForIdle()

                composeRule.onNodeWithText("Deck options").assertExists()
                composeRule.onNodeWithText("Custom study").assertDoesNotExist()
                composeRule.onNodeWithText(TR.studyingThisIsASpecialDeckFor()).assertExists()
            }
        }

    // https://github.com/ankidroid/Anki-Android/pull/21409#pullrequestreview-3568785419
    @Test
    fun `resuming after a background rebuild refills the deck - navigates to study options`() =
        runTest {
            addBasicNote("Front", "Back")
            val deckId = addDynamicDeck("Filtered", "")
            withCol { sched.emptyFilteredDeck(deckId) }

            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                scenario.moveToState(Lifecycle.State.CREATED)

                // rebuild happens while the screen isn't visible - nobody is collecting congratsRefreshState
                undoableOp { sched.rebuildFilteredDeck(deckId) }

                scenario.moveToState(Lifecycle.State.RESUMED)
                advanceUntilIdle()

                scenario.onFragment { fragment ->
                    val next = shadowOf(fragment.requireActivity()).nextStartedActivity
                    assertEquals(StudyOptionsActivity::class.java.name, next?.component?.className)
                }
            }
        }

    @Test
    fun `custom study input dialog survives activity recreation`() =
        runTest {
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { new.perDay = 0 }
            addBasicNote()
            ioDispatcher = StandardTestDispatcher(testScheduler)

            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                scenario.onFragment { it.onCustomStudy() }
                shadowOf(Looper.getMainLooper()).idle()
                advanceUntilIdle()
                clickText(TR.customStudyIncreaseTodaysNewCardLimit())
                advanceUntilIdle()

                scenario.recreate()

                composeRule
                    .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText) and hasAnyAncestor(isPaperDialog()))
                    .assertIsDisplayed()
            }
        }

    @Test
    fun `custom study results open study options and finish the screen`() =
        runTest {
            // Keep the deck finished so the finished screen stays open.
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { new.perDay = 0 }
            addBasicNote()

            for (action in CustomStudyAction.entries) {
                // hosted by EmptyAnkiActivity, not SingleFragmentActivity: the screen handles the result itself
                launchFragmentInContainer<FinishedFragment>().use { scenario ->
                    val activity = scenario.withFragment { requireActivity() }
                    advanceUntilIdle()
                    assertNull(shadowOf(activity).nextStartedActivity, "$action: screen opened study options early")

                    scenario.postCustomStudyResult(action)

                    assertOpenedStudyOptionsAndFinished(activity)
                }
            }
        }

    @Test
    fun `custom study result is handled after the screen is recreated`() =
        runTest {
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { new.perDay = 0 }
            addBasicNote()

            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                scenario.recreate()
                val activity = scenario.withFragment { requireActivity() }
                advanceUntilIdle()
                assertNull(shadowOf(activity).nextStartedActivity)

                scenario.postCustomStudyResult(CustomStudyAction.EXTEND_STUDY_LIMITS)

                assertOpenedStudyOptionsAndFinished(activity)
            }
        }

    /** Issue 22801: the activity owns the custom study operation, so its result can arrive while the screen is stopped */
    @Test
    fun `custom study result received while stopped is handled when the screen starts`() =
        runTest {
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { new.perDay = 0 }
            addBasicNote()

            launchFragmentInContainer<FinishedFragment>().use { scenario ->
                val activity = scenario.withFragment { requireActivity() }
                scenario.moveToState(Lifecycle.State.CREATED)

                scenario.postCustomStudyResult(CustomStudyAction.CUSTOM_STUDY_SESSION)
                assertNull(shadowOf(activity).nextStartedActivity, "a stopped screen should not navigate")
                assertTrue(!activity.isFinishing)

                scenario.moveToState(Lifecycle.State.RESUMED)
                advanceUntilIdle()

                assertOpenedStudyOptionsAndFinished(activity)
            }
        }

    @Test
    fun `custom study tag selection survives process death`() =
        runTest {
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { new.perDay = 0 }
            val selectedNote = addBasicNote().update { addTag("selected") }
            val otherNote = addBasicNote().update { addTag("other") }

            val savedState =
                Robolectric.buildActivity(EmptyAnkiActivity::class.java).use { controller ->
                    val activity = controller.setup().get()
                    val screen = FinishedFragment()
                    activity.supportFragmentManager.commitNow { add(android.R.id.content, screen) }
                    screen.onCustomStudy()
                    shadowOf(Looper.getMainLooper()).idle()
                    clickText(TR.customStudyStudyByCardStateOrTag())
                    clickText(with(screen) { TR.sentenceCase.chooseTags })
                    controller.saveState()
                }

            // recreate() retains ViewModels and process-local defaults. Restore a fresh activity
            // from saved state and clear the defaults to model process death with the picker open.
            uninitializeField<CustomStudyDialog>("deferredInitialization")
            Robolectric.buildActivity(EmptyAnkiActivity::class.java).use { controller ->
                controller.setup(savedState)
                shadowOf(Looper.getMainLooper()).idle()
                clickText("selected")
                clickText(getResourceString(CommonString.select))
            }

            assertEquals(assertNotNull(col.decks.customStudySession).id, selectedNote.firstCard().did)
            assertEquals(Consts.DEFAULT_DECK_ID, otherNote.firstCard().did)
        }

    private fun clickText(text: String) {
        composeRule.waitForIdle()
        val node = composeRule.onNodeWithText(text)
        // "Back to decks" sits below the scrolling page, the rest inside it
        runCatching { node.performScrollTo() }
        node.performClick()
        composeRule.waitForIdle()
    }

    /** Posts [action] where [CustomStudyDialog] posts it: the activity's fragment manager */
    private fun AnkiFragmentScenario<FinishedFragment>.postCustomStudyResult(action: CustomStudyAction) {
        onFragment { screen ->
            screen.requireActivity().supportFragmentManager.setFragmentResult(
                CustomStudyAction.REQUEST_KEY,
                Bundle().apply { putInt(CustomStudyAction.RESULT_CUSTOM_STUDY_ACTION, action.ordinal) },
            )
        }
    }

    private fun assertOpenedStudyOptionsAndFinished(activity: Activity) {
        assertEquals(StudyOptionsActivity::class.java.name, shadowOf(activity).nextStartedActivity?.component?.className)
        assertNull(shadowOf(activity).nextStartedActivity, "study options should open once")
        assertTrue(activity.isFinishing)
    }
}
