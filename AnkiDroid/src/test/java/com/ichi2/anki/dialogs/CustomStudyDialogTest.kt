// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.os.Bundle
import android.os.Looper
import android.widget.AdapterView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.scheduler.CustomStudyDefaultsResponse
import anki.scheduler.CustomStudyRequest.Cram.CramKind
import anki.scheduler.customStudyDefaultsResponse
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.CustomStudyAction
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.CustomStudyDefaults.Companion.toDomainModel
import com.ichi2.anki.dialogs.customstudy.CustomStudyViewModel
import com.ichi2.anki.dialogs.tags.TagsDialogListener.Companion.REQUEST_SELECT_TAGS
import com.ichi2.anki.dialogs.tags.TagsDialogListener.Companion.RESULT_SELECTED_TAGS
import com.ichi2.anki.ioDispatcher
import com.ichi2.anki.libanki.CardType
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.QueueType
import com.ichi2.anki.libanki.sched.Scheduler
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.testutils.AnkiFragmentScenario
import com.ichi2.testutils.EmptyAnkiActivity
import com.ichi2.testutils.ext.snackbarText
import com.ichi2.testutils.isJsonHolderEqual
import com.ichi2.testutils.launchFragment
import com.ichi2.testutils.uninitializeField
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.nullValue
import org.intellij.lang.annotations.Language
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class CustomStudyDialogTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    override fun setUp() {
        super.setUp()
        uninitializeField<CustomStudyDialog>("deferredInitialization")
    }

    @Test
    fun `cancelling a defaults read prevents late deck selection`() =
        runTest {
            val response = customStudyDefaultsResponse { availableNew = 1 }
            val collection = mockCollectionWithSchedulerReturning(response)
            val decks = collection.decks
            val initialization = Job()
            every { collection.sched.customStudyDefaults(Consts.DEFAULT_DECK_ID) } answers {
                initialization.cancel()
                response
            }
            CollectionManager.setColForTests(collection)
            val viewModel = CustomStudyViewModel(SavedStateHandle(mapOf(CustomStudyViewModel.ARG_DECK_ID to Consts.DEFAULT_DECK_ID)))

            assertFailsWith<CancellationException> {
                withContext(initialization) { viewModel.initialize() }
            }

            verify(exactly = 0) { decks.select(Consts.DEFAULT_DECK_ID) }
        }

    @Test
    fun `empty deck initialization does not block the dialog`() =
        runTest {
            val other = addDeck("Other")
            col.setDeck(listOf(addBasicNote().firstCard().id), other)
            val undoStatus = col.undoStatus()
            val previousDispatcher = ioDispatcher
            ioDispatcher = StandardTestDispatcher(testScheduler)
            try {
                createHostActivity().use { controller ->
                    val activity = controller.get()
                    val fragment = CustomStudyDialog.createInstance(Consts.DEFAULT_DECK_ID)
                    activity.showDialogFragment(fragment)
                    progress().assertExists()

                    runCurrent()

                    assertUnavailableMenu(fragment)
                    assertEquals(Consts.DEFAULT_DECK_ID, col.decks.selected())
                    assertEquals(undoStatus, col.undoStatus())
                }
            } finally {
                ioDispatcher = previousDispatcher
            }
        }

    @Test
    fun `populated deck selection waits for initialization`() =
        runTest {
            val target = addDeck("Target")
            col.setDeck(listOf(addBasicNote().firstCard().id), target)
            val previousDispatcher = ioDispatcher
            ioDispatcher = StandardTestDispatcher(testScheduler)
            try {
                AnkiFragmentScenario.launch(CustomStudyDialog::class.java, argumentsDisplayingMainScreen(target)).use { scenario ->
                    assertEquals(Consts.DEFAULT_DECK_ID, col.decks.selected())

                    runCurrent()

                    assertEquals(target, col.decks.selected())
                    scenario.onFragment {
                        progress().assertDoesNotExist()
                        compose.onNodeWithText(TR.customStudyStudyByCardStateOrTag()).assertIsEnabled()
                    }
                }
            } finally {
                ioDispatcher = previousDispatcher
            }
        }

    @Test
    fun `cancelling initialization does not select the deck`() =
        runTest {
            val target = addDeck("Target")
            col.setDeck(listOf(addBasicNote().firstCard().id), target)
            val undoStatus = col.undoStatus()
            val previousDispatcher = ioDispatcher
            ioDispatcher = StandardTestDispatcher(testScheduler)
            try {
                AnkiFragmentScenario.launch(CustomStudyDialog::class.java, argumentsDisplayingMainScreen(target)).use { scenario ->
                    scenario.moveToState(Lifecycle.State.DESTROYED)

                    runCurrent()

                    assertEquals(Consts.DEFAULT_DECK_ID, col.decks.selected())
                    assertEquals(undoStatus, col.undoStatus())
                }
            } finally {
                ioDispatcher = previousDispatcher
            }
        }

    @Test
    fun `empty initialization waits for resume before showing feedback`() =
        runTest {
            val previousDispatcher = ioDispatcher
            ioDispatcher = StandardTestDispatcher(testScheduler)
            try {
                createHostActivity().use { controller ->
                    val activity = controller.get()
                    val fragment = CustomStudyDialog.createInstance(Consts.DEFAULT_DECK_ID)
                    activity.showDialogFragment(fragment)
                    controller.pause().saveInstanceState(Bundle()).stop()

                    runCurrent()

                    assertTrue(fragment.isAdded)
                    assertTrue(activity.supportFragmentManager.isStateSaved)
                    assertNull(activity.snackbarText)

                    controller.start().resume().visible()
                    runCurrent()

                    assertUnavailableMenu(fragment)
                }
            } finally {
                ioDispatcher = previousDispatcher
            }
        }

    @Test
    fun `cancelled loading dialog does not show empty-deck feedback`() =
        runTest {
            val previousDispatcher = ioDispatcher
            ioDispatcher = StandardTestDispatcher(testScheduler)
            try {
                createHostActivity().use { controller ->
                    val activity = controller.get()
                    val fragment = CustomStudyDialog.createInstance(Consts.DEFAULT_DECK_ID)
                    activity.showDialogFragment(fragment)
                    fragment.requireDialog().cancel()

                    runCurrent()
                    shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
                    activity.supportFragmentManager.executePendingTransactions()

                    assertFalse(fragment.isAdded)
                    assertNull(activity.snackbarText)
                }
            } finally {
                ioDispatcher = previousDispatcher
            }
        }

    @Test
    fun `stale custom study menu explains that the deck is now empty`() =
        runTest {
            val card = addBasicNote().firstCard()
            withCustomStudyFragment(argumentsDisplayingMainScreen()) { fragment ->
                val fragmentManager = fragment.parentFragmentManager
                col.removeNotes(cardIds = listOf(card.id))

                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                advanceUntilIdle()

                assertUnavailableMenu(fragment)
                assertEquals(
                    0,
                    fragmentManager.fragments
                        .filterIsInstance<CustomStudyDialog>()
                        .size,
                )
            }
        }

    @Test
    fun `stale menu becomes unavailable when its deck is deleted`() =
        runTest {
            val target = addDeck("Target")
            col.setDeck(listOf(addBasicNote().firstCard().id), target)
            withCustomStudyFragment(argumentsDisplayingMainScreen(target)) { fragment ->
                val dialog = fragment.requireDialog()
                col.decks.remove(listOf(target))
                val selectedDeck = col.decks.selected()
                val undoStatus = col.undoStatus()

                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                advanceUntilIdle()

                assertUnavailableMenu(fragment)
                assertFalse(dialog.isShowing)
                assertEquals(selectedDeck, col.decks.selected())
                assertEquals(undoStatus, col.undoStatus())
            }
        }

    @Test
    fun `deleted deck initialization explains why custom study is unavailable`() {
        val target = addDeck("Target")
        col.setDeck(listOf(addBasicNote().firstCard().id), target)
        col.decks.remove(listOf(target))
        val selectedDeck = col.decks.selected()
        val undoStatus = col.undoStatus()

        withCustomStudyFragment(argumentsDisplayingMainScreen(target)) { fragment ->
            assertUnavailableMenu(fragment)
        }

        assertEquals(selectedDeck, col.decks.selected())
        assertEquals(undoStatus, col.undoStatus())
    }

    @Test
    fun `menu check does not navigate after state saving and can be retried on resume`() =
        runTest {
            addBasicNote()
            val controller = Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup()
            saveControllerForCleanup(controller)
            advanceUntilIdle()
            advanceRobolectricLooper()
            val activity = controller.get()
            activity.showDialogFragment(CustomStudyDialog.createInstance(Consts.DEFAULT_DECK_ID))
            val fragment = activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as CustomStudyDialog
            compose.onNodeWithText(TR.customStudyReviewAhead()).assertExists()

            // Exercise the production queue's suspension instead of Robolectric's synchronous lock.
            val previousIoDispatcher = ioDispatcher
            ioDispatcher = StandardTestDispatcher(testScheduler)
            val collectionScheduler = TestCoroutineScheduler()
            val previousDispatcher =
                CollectionManager.setTestDispatcher(StandardTestDispatcher(collectionScheduler), useReentrantLock = false)
            try {
                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()
                assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
                controller.pause().saveInstanceState(Bundle()).stop()

                collectionScheduler.runCurrent()
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()

                assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
                controller.start().resume().visible()
                advanceUntilIdle()
                advanceRobolectricLooper()
                assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))

                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()
                collectionScheduler.runCurrent()
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()
                collectionScheduler.runCurrent()
                runCurrent()

                input().assertIsDisplayed()
                assertEquals(
                    1,
                    activity.supportFragmentManager.fragments
                        .filterIsInstance<CustomStudyDialog>()
                        .size,
                )
            } finally {
                CollectionManager.setTestDispatcher(previousDispatcher)
                ioDispatcher = previousIoDispatcher
            }
        }

    @Test
    fun `hidden empty Default explains why custom study is unavailable and preserves redo`() {
        val other = addDeck("Other")
        col.setDeck(listOf(addBasicNote().firstCard().id), other)
        addBasicNote()
        col.undo()
        assertNull(col.sched.deckDueTree().find(Consts.DEFAULT_DECK_ID))
        val undoStatus = col.undoStatus()
        assertNotNull(undoStatus.redo)

        withCustomStudyFragment(argumentsDisplayingMainScreen()) { fragment ->
            assertUnavailableMenu(fragment)
        }

        assertEquals(Consts.DEFAULT_DECK_ID, col.decks.selected())
        assertEquals(undoStatus, col.undoStatus())
        col.redo()
        withCustomStudyFragment(argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyStudyByCardStateOrTag()).assertIsEnabled()
        }
    }

    @Test
    fun `hidden Default with borrowed cards avoids loading defaults and preserves selection`() {
        addBasicNote()
        val filtered = addDynamicDeck("Filtered", "")
        assertNull(col.sched.deckDueTree().find(Consts.DEFAULT_DECK_ID))
        assertEquals(1, col.decks.cardCount(Consts.DEFAULT_DECK_ID, includeSubdecks = true))
        val undoStatus = col.undoStatus()

        withCustomStudyFragment(argumentsDisplayingMainScreen()) { fragment ->
            assertUnavailableMenu(fragment)
        }

        assertEquals(filtered, col.decks.selected())
        assertEquals(undoStatus, col.undoStatus())

        col.sched.emptyFilteredDeck(filtered)
        withCustomStudyFragment(argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyStudyByCardStateOrTag()).assertIsEnabled()
        }
    }

    @Test
    fun `stale menu becomes unavailable when a filtered deck borrows all Default cards`() =
        runTest {
            addBasicNote()
            withCustomStudyFragment(argumentsDisplayingMainScreen()) { fragment ->
                val fragmentManager = fragment.parentFragmentManager
                val filtered = addDynamicDeck("Filtered", "")
                assertNull(col.sched.deckDueTree().find(Consts.DEFAULT_DECK_ID))
                assertEquals(1, col.decks.cardCount(Consts.DEFAULT_DECK_ID, includeSubdecks = true))
                val undoStatus = col.undoStatus()

                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                advanceUntilIdle()

                assertUnavailableMenu(fragment)
                assertEquals(
                    0,
                    fragmentManager.fragments
                        .filterIsInstance<CustomStudyDialog>()
                        .size,
                )
                assertEquals(filtered, col.decks.selected())
                assertEquals(undoStatus, col.undoStatus())
            }
        }

    @Test
    fun `empty deck explains why custom study is unavailable without selecting it`() {
        addBasicNote()
        val emptyDeck = addDeck("Empty")
        val undoStatus = col.undoStatus()

        withCustomStudyFragment(argumentsDisplayingMainScreen(emptyDeck)) { fragment ->
            assertUnavailableMenu(fragment)
        }

        assertEquals(Consts.DEFAULT_DECK_ID, col.decks.selected())
        assertEquals(undoStatus, col.undoStatus())
    }

    @Test
    fun `custom study remains available when only a child deck has cards`() {
        val child = addDeck("Default::Child")
        col.setDeck(listOf(addBasicNote().firstCard().id), child)

        withCustomStudyFragment(argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyStudyByCardStateOrTag()).assertIsEnabled()
        }
    }

    @Test
    fun `new custom study decks have expected structure - issue 6289`() =
        runTest {
            val studyType = ContextMenuOption.STUDY_PREVIEW
            // we need a non-empty deck to custom study
            addBasicNote()

            withCustomStudyFragment(
                args = argumentsDisplayingSubscreen(studyType),
            ) { dialogFragment: CustomStudyDialog ->
                dialogFragment.submitSubscreenData()
            }

            val customStudy = col.decks.current()
            assertThat("Custom Study should be filtered", customStudy.isFiltered)

            // remove timestamps to allow us to compare JSON
            customStudy.jsonObject.remove("id")
            customStudy.jsonObject.remove("mod")
            customStudy.jsonObject.remove("name")

            // compare JSON
            @Language("json")
            val expected =
                """
                {
                    "browserCollapsed": true,
                    "collapsed": true,
                    "delays": null,
                    "desc": "",
                    "dyn": 1,
                    "lrnToday": [0, 0],
                    "newToday": [0, 0],
                    "previewDelay": 10,
                    "previewAgainSecs": 60,
                    "previewHardSecs": 600,
                    "previewGoodSecs": 0,
                    "resched": false,
                    "revToday": [0, 0],
                    "separate": true,
                    "terms": [
                        ["is:new added:1 deck:Default", 99999, 5]
                    ],
                    "timeToday": [0, 0],
                    "usn": -1
                }
                """.trimIndent()
            assertThat(customStudy, isJsonHolderEqual(expected))
        }

    /** Issue 22801: the activity owns the operation, so it can finish after the dialog is removed. */
    @Test
    fun `custom study completes after its dialog is dismissed`() =
        runTest {
            addBasicNote()
            withCustomStudyFragment(argumentsDisplayingSubscreen(ContextMenuOption.STUDY_PREVIEW)) { fragment ->
                val activity = fragment.requireActivity()
                val manager = fragment.parentFragmentManager
                val preferences = activity.sharedPrefs()
                val collection = col
                var result: CustomStudyAction? = null
                manager.setFragmentResultListener(CustomStudyAction.REQUEST_KEY, activity) { _, bundle ->
                    result = CustomStudyAction.fromBundle(bundle)
                }
                val collectionScheduler = TestCoroutineScheduler()
                val previousDispatcher =
                    CollectionManager.setTestDispatcher(StandardTestDispatcher(collectionScheduler), useReentrantLock = false)
                try {
                    fragment.submitSubscreenData()
                    runCurrent()
                    assertFalse(collection.decks.current().isFiltered)

                    fragment.dismissNow()
                    assertNull(fragment.activity)
                    collectionScheduler.runCurrent()
                    runCurrent()
                    advanceRobolectricLooper()

                    assertTrue(collection.decks.current().isFiltered)
                    assertEquals(CustomStudyAction.CUSTOM_STUDY_SESSION, result)
                    assertEquals(1, preferences.getInt("previewDays", -1))
                } finally {
                    CollectionManager.setTestDispatcher(previousDispatcher)
                }
            }
        }

    @Test
    fun `previous value for 'increase new card limit' is suggested`() =
        runTest {
            // add cards to be sure we can extend successfully. Needs to be > 20
            repeat(23) {
                addBasicNote()
            }
            val newExtendByValue = 1

            assertThat("'new' default value", defaultsOfDefaultDeck.extendNew.initialValue, equalTo(0))

            // extend limits with a value of '1'
            withCustomStudyFragment(
                args = argumentsDisplayingSubscreen(ContextMenuOption.EXTEND_NEW),
            ) { dialogFragment: CustomStudyDialog ->

                input().performTextReplacement(newExtendByValue.toString())

                dialogFragment.submitSubscreenData()
            }

            // ensure backend is updated
            assertThat(
                "'new' updated value",
                defaultsOfDefaultDeck.extendNew.initialValue,
                equalTo(newExtendByValue),
            )

            // ensure 'newExtendByValue' is used by our UI
            withCustomStudyFragment(
                args = argumentsDisplayingSubscreen(ContextMenuOption.EXTEND_NEW),
            ) {
                input().assertTextContains(newExtendByValue.toString())
            }
        }

    @Test
    fun `previous value for 'increase review card limit' is suggested`() =
        runTest {
            // Reduce review limit to 0, so we can successfully extend with just 1 review card.
            updateDeckConfig(Consts.DEFAULT_DECK_ID) { rev.perDay = 0 }
            addRevBasicNoteDueToday("Review", "Today")

            val reviewExtendByValue = 1
            assertThat("'review' default value", defaultsOfDefaultDeck.extendReview.initialValue, equalTo(0))

            // Extend reviews by 'reviewExtendByValue'.
            withCustomStudyFragment(
                args = argumentsDisplayingSubscreen(ContextMenuOption.EXTEND_REV),
            ) { dialogFragment: CustomStudyDialog ->
                input().performTextReplacement(reviewExtendByValue.toString())
                dialogFragment.submitSubscreenData()
            }

            // Ensure backend is updated.
            assertThat(
                "'review' updated value",
                defaultsOfDefaultDeck.extendReview.initialValue,
                equalTo(reviewExtendByValue),
            )

            // Ensure 'reviewExtendByValue' is used in our UI.
            withCustomStudyFragment(
                args = argumentsDisplayingSubscreen(ContextMenuOption.EXTEND_REV),
            ) {
                input().assertTextContains(reviewExtendByValue.toString())
            }
        }

    @Test
    @Config(qualifiers = "en")
    fun `'extend limits' dialogs are titled 'Extend daily limits' and confirm with 'Increase'`() {
        listOf(ContextMenuOption.EXTEND_NEW, ContextMenuOption.EXTEND_REV).forEach { option ->
            withExtendLimitsDialog(option) { dialog ->
                compose.onNodeWithText("Extend daily limits").assertIsDisplayed()
                dialog.assertTextContains("Increase")
            }
        }
    }

    @Test
    @Config(qualifiers = "en")
    fun `'extend limits' descriptions do not mention decreasing the limit`() {
        listOf(ContextMenuOption.EXTEND_NEW, ContextMenuOption.EXTEND_REV).forEach { option ->
            withExtendLimitsDialog(option) {
                assertThat(
                    "$option description",
                    input().fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(),
                    not(containsString("decrease")),
                )
            }
        }
    }

    @Test
    fun `'extend limits' dialogs show the available cards below the input`() {
        mapOf(
            ContextMenuOption.EXTEND_NEW to { defaultsOfDefaultDeck.labelForNewQueueAvailable() },
            ContextMenuOption.EXTEND_REV to { defaultsOfDefaultDeck.labelForReviewQueueAvailable() },
        ).forEach { (option, expectedLabel) ->
            withExtendLimitsDialog(option) {
                val label = compose.onNodeWithText(expectedLabel()).fetchSemanticsNode().boundsInRoot
                assertTrue(
                    label.top >= input().fetchSemanticsNode().boundsInRoot.bottom,
                    "$option available cards label is below the input",
                )
            }
        }
    }

    @Test
    fun `'extend limits' steppers change the value by 1`() {
        listOf(ContextMenuOption.EXTEND_NEW, ContextMenuOption.EXTEND_REV).forEach { option ->
            withExtendLimitsDialog(option) {
                val initial = inputText().toInt()

                compose.onNodeWithText("+").performClick()
                assertThat("$option after +", inputText(), equalTo("${initial + 1}"))

                compose.onNodeWithText("−").performClick()
                compose.onNodeWithText("−").performClick()
                assertThat("$option after -", inputText(), equalTo("${initial - 1}"))
            }
        }
    }

    @Test
    fun `'extend limits' stepper treats an empty value as 0`() {
        withExtendLimitsDialog(ContextMenuOption.EXTEND_NEW) {
            input().performTextReplacement("")
            compose.onNodeWithText("+").performClick()
            assertThat(inputText(), equalTo("1"))
        }
    }

    @Test
    fun `steppers are not shown outside of 'extend limits'`() {
        withCustomStudyFragment(args = argumentsDisplayingSubscreen(ContextMenuOption.STUDY_FORGOT)) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onNodeWithText("+").assertDoesNotExist()
            compose.onNodeWithText("−").assertDoesNotExist()
        }
    }

    @Test
    fun `boundary values validate correctly across study modes`() {
        // STUDY_FORGOT boundary values: 0 (invalid), 1 (valid), 30 (valid), 31 (invalid)
        withCustomStudyFragment(args = argumentsDisplayingSubscreen(ContextMenuOption.STUDY_FORGOT)) {
            val dialog = confirmAction()

            input().performTextReplacement("0")
            dialog.assertIsNotEnabled()
            assertThat(
                "0 error in STUDY_FORGOT",
                inputError(),
                containsString("1"),
            )

            input().performTextReplacement("1")
            dialog.assertIsEnabled()
            assertThat("1 error in STUDY_FORGOT is null", inputError(), nullValue())

            input().performTextReplacement("30")
            dialog.assertIsEnabled()
            assertThat("30 error in STUDY_FORGOT is null", inputError(), nullValue())

            input().performTextReplacement("31")
            dialog.assertIsNotEnabled()
            assertThat(
                "31 error in STUDY_FORGOT",
                inputError(),
                containsString("30"),
            )
        }

        // EXTEND_NEW boundary values: -99999 (valid), 0 (invalid), 99999 (valid), 100000 (invalid)
        withExtendLimitsDialog(ContextMenuOption.EXTEND_NEW) { dialog ->
            input().performTextReplacement("-99999")
            dialog.assertIsEnabled()
            assertThat("-99999 error is null", inputError(), nullValue())

            input().performTextReplacement("0")
            dialog.assertIsNotEnabled()

            input().performTextReplacement("99999")
            dialog.assertIsEnabled()
            assertThat("99999 error is null", inputError(), nullValue())

            input().performTextReplacement("100000")
            dialog.assertIsNotEnabled()
            assertThat("100000 error in EXTEND_NEW", inputError(), containsString("99999"))
        }
    }

    @Test
    fun `creating a tags custom session uses selected card state`() =
        runTest {
            val testDeckId = addDeck("A")
            val n1 = addNoteToDeckA { addTag("testTag") }
            val n2 = addNoteToDeckA { addTag("anotherTag") }
            val n3 = addNoteToDeckA { addTag("testTag") }
            // target this specific card for custom studying
            // TODO use Card.moveToReviewQueue when it's refactored
            n3.firstCard().update {
                due = col.sched.today
                queue = QueueType.Rev
                type = CardType.Rev
            }
            col.updateCard(n3.firstCard())
            val dueNow = col.findCards("is:due")
            assertThat(dueNow.size, equalTo(1))
            assertThat(dueNow[0], equalTo(n3.firstCard().id))
            // make sure there isn't a 'Custom Study Session' already present
            assertNull(col.decks.customStudySession)
            val args = argumentsDisplayingSubscreen(ContextMenuOption.STUDY_TAGS, deckId = testDeckId)
            withCustomStudyFragment(args = args) { studyDialog ->
                val d = studyDialog.dialog
                assertNotNull(d)
                // the first item is automatically selected at start
                assertThat(studyDialog.viewModel.selectedCardStateIndex, equalTo(0))

                studyDialog.setCardStateSelectionTo(CustomStudyDialog.CustomStudyCardState.DueCardsOnly)

                // create list of selected tags
                // Note: using an ArrayList because that is how it's stored in the passed Bundle
                val selectedTags = ArrayList<String>(1).apply { add("testTag") }
                // simulate tag selection
                studyDialog.parentFragmentManager.setFragmentResult(
                    REQUEST_SELECT_TAGS,
                    Bundle().apply { putStringArrayList(RESULT_SELECTED_TAGS, selectedTags) },
                )
                val customStudyDeck = col.decks.customStudySession
                assertNotNull(customStudyDeck)
                assertThat(col.decks.cardCount(customStudyDeck.id), equalTo(1))
                assertThat(n1.firstCard().did, equalTo(testDeckId))
                assertThat(n2.firstCard().did, equalTo(testDeckId))
                assertThat(n3.firstCard().did, equalTo(customStudyDeck.id))
                assertThat(n3.firstCard().oDid, equalTo(testDeckId))
            }
        }

    private fun addNoteToDeckA(setup: Note.() -> Unit): Note =
        addBasicNote().update {
            moveToDeck("A", false)
            setup()
        }

    @Test
    @Ignore("disabled while we confirm/diagnose issues")
    @Config(qualifiers = "en")
    fun `'increase new limit' is shown when there are new cards`() {
        val studyDefaults = customStudyDefaultsResponse { availableNew = 1 }
        CollectionManager.setColForTests(mockCollectionWithSchedulerReturning(studyDefaults))

        withCustomStudyFragment(args = argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyIncreaseTodaysNewCardLimit()).assertIsEnabled()
        }
    }

    @Test
    @Ignore("disabled while we confirm/diagnose issues")
    @Config(qualifiers = "en")
    fun `'increase new limit' is not shown when there are no new cards`() {
        val studyDefaults = customStudyDefaultsResponse { availableNew = 0 }
        CollectionManager.setColForTests(mockCollectionWithSchedulerReturning(studyDefaults))

        withCustomStudyFragment(args = argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyIncreaseTodaysNewCardLimit()).assertIsNotEnabled()
        }
    }

    @Test
    @Ignore("disabled while we confirm/diagnose issues")
    @Config(qualifiers = "en")
    fun `'increase review limit' is shown when there are new cards`() {
        val studyDefaults = customStudyDefaultsResponse { availableReview = 1 }
        CollectionManager.setColForTests(mockCollectionWithSchedulerReturning(studyDefaults))

        withCustomStudyFragment(args = argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyIncreaseTodaysReviewCardLimit()).assertIsEnabled()
        }
    }

    @Test
    @Ignore("disabled while we confirm/diagnose issues")
    @Config(qualifiers = "en")
    fun `'increase review limit' is not shown when there are no new cards`() {
        val studyDefaults = customStudyDefaultsResponse { availableReview = 0 }
        CollectionManager.setColForTests(mockCollectionWithSchedulerReturning(studyDefaults))

        withCustomStudyFragment(args = argumentsDisplayingMainScreen()) {
            compose.onNodeWithText(TR.customStudyIncreaseTodaysReviewCardLimit()).assertIsNotEnabled()
        }
    }

    @Test
    fun `subscreens are ignored when restoring from process death`() {
        addBasicNote()
        withCustomStudyFragment(args = argumentsDisplayingSubscreen(ContextMenuOption.EXTEND_REV, restoreFromProcessDeath = true)) {
            // ensure we're on the main screen
            compose.onNodeWithText(TR.customStudyIncreaseTodaysReviewCardLimit()).assertIsDisplayed()
        }
    }

    @Test
    fun `tags chosen after recreation start a custom study session`() =
        runTest {
            addBasicNote().update { addTag("testTag") }

            launchFragment<Fragment>().use { scenario ->
                scenario.onFragment {
                    it.requireActivity().showDialogFragment(CustomStudyDialog.createInstance(Consts.DEFAULT_DECK_ID))
                }
                compose.onNodeWithText(TR.customStudyStudyByCardStateOrTag()).performScrollTo().performClick()

                compose.waitForIdle()
                scenario.recreate()

                scenario.onFragment {
                    it.parentFragmentManager.setFragmentResult(
                        REQUEST_SELECT_TAGS,
                        Bundle().apply { putStringArrayList(RESULT_SELECTED_TAGS, arrayListOf("testTag")) },
                    )
                }
                advanceUntilIdle()
                assertNotNull(col.decks.customStudySession)
            }
        }

    @Test
    fun `selectedKind maps selected card state index to cram kind`() {
        val viewModel = CustomStudyViewModel(SavedStateHandle())

        CustomStudyDialog.CustomStudyCardState.entries.forEachIndexed { index, cardState ->
            viewModel.selectedCardStateIndex = index
            assertThat(viewModel.selectedKind, equalTo(cardState.kind))
        }
    }

    @Test
    fun `selectedKind defaults to new cards when no card state is selected`() {
        val viewModel = CustomStudyViewModel(SavedStateHandle())

        viewModel.selectedCardStateIndex = AdapterView.INVALID_POSITION

        assertThat(viewModel.selectedKind, equalTo(CramKind.CRAM_KIND_NEW))
    }

    @Test
    @Config(qualifiers = "en")
    fun `'review ahead' shows a day or days suffix`() {
        addBasicNote()
        withStudyAheadDialog {
            compose.onNodeWithText("day", useUnmergedTree = true).assertIsDisplayed()

            input().performTextReplacement("2")
            compose.onNodeWithText("days", useUnmergedTree = true).assertIsDisplayed()

            input().performTextReplacement("1")
            compose.onNodeWithText("day", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `'review ahead' rejects leading zeros and more than 5 digits`() {
        addBasicNote()
        withStudyAheadDialog {
            input().performTextReplacement("0")
            input().performTextInput("5")
            assertThat("a digit typed after '0' is rejected", inputText(), equalTo("0"))

            input().performTextReplacement("123456")
            assertThat(inputText(), equalTo("12345"))
        }
    }

    @Test
    fun `'review ahead' disables Create when no cards are due in the period`() =
        runTest {
            // a card due tomorrow matches 'review ahead by 1 day'
            val card = addBasicNote().firstCard()
            card.update {
                queue = QueueType.Rev
                type = CardType.Rev
                due = col.sched.today + 1
            }

            withStudyAheadDialog { dialog ->

                input().performTextReplacement("1")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsEnabled()
                assertNull(inputError())

                input().performTextReplacement("0")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsNotEnabled()

                card.update { due = col.sched.today + 30 }

                input().performTextReplacement("1")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsNotEnabled()
                assertThat(inputError(), equalTo(TR.customStudyNoCardsMatchedTheCriteriaYou()))
            }
        }

    @Test
    fun `'review ahead' search does not treat the deck name as a pattern`() =
        runTest {
            // '_' is a single-character wildcard in a search: "A_B" must not match "AXB"
            val deckId = addDeck("A_B")
            addBasicNote().update { moveToDeck("A_B", false) }.firstCard().update {
                queue = QueueType.Rev
                type = CardType.Rev
                due = col.sched.today + 30
            }
            addDeck("AXB")
            addNoteDueTomorrow(deckName = "AXB")

            withStudyAheadDialog(deckId = deckId) { dialog ->
                input().performTextReplacement("1")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsNotEnabled()
            }
        }

    @Test
    fun `'review ahead' search escapes the deck name`() =
        runTest {
            // '\' starts an escape sequence in a search
            val deckId = addDeck("""A\B""")
            addNoteDueTomorrow(deckName = """A\B""")

            withStudyAheadDialog(deckId = deckId) { dialog ->
                input().performTextReplacement("1")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsEnabled()
            }
        }

    @Test
    @Config(qualifiers = "en")
    fun `'review ahead' warns when 0 days is entered`() =
        runTest {
            addBasicNote()
            withStudyAheadDialog {
                input().performTextReplacement("0")
                shadowOf(Looper.getMainLooper()).idle()
                assertThat(inputError(), equalTo("Minimum value is 1"))

                input().performTextReplacement("")
                shadowOf(Looper.getMainLooper()).idle()
                assertNull(inputError(), "an empty field is not an error yet")
            }
        }

    @Test
    fun `'review ahead' validates the default value on open`() =
        runTest {
            // the default is 1 day; the only card is due in 30 days
            addBasicNote().firstCard().update {
                queue = QueueType.Rev
                type = CardType.Rev
                due = col.sched.today + 30
            }

            withStudyAheadDialog { dialog ->
                dialog.assertIsNotEnabled()
                assertThat(inputError(), equalTo(TR.customStudyNoCardsMatchedTheCriteriaYou()))
            }
        }

    @Test
    fun `typed value is kept after a dialog on top closes - issue 22731`() =
        withTypedInputCoveredByAnotherDialog(ContextMenuOption.EXTEND_NEW, typed = "7") {
            input().assertTextContains("7")
        }

    @Test
    fun `'preview' positive button is disabled on open when no cards were added recently`() =
        runTest {
            addBasicNote()
            col.db.execute(
                "UPDATE cards SET id = id - ${40L * 24 * 60 * 60 * 1000} WHERE id > 0",
            )

            withStudyPreviewDialog { dialog ->
                dialog.assertIsNotEnabled()
                assertThat(
                    inputError(),
                    equalTo(TR.customStudyNoCardsMatchedTheCriteriaYou()),
                )
            }
        }

    @Test
    @Config(qualifiers = "en")
    fun `'review ahead' suffix matches the typed value after a dialog on top closes`() =
        withTypedInputCoveredByAnotherDialog(ContextMenuOption.STUDY_AHEAD, typed = "2") {
            compose.onNodeWithText("days", useUnmergedTree = true).assertIsDisplayed()
        }

    @Test
    @Config(qualifiers = "en")
    fun `'preview' shows a day or days suffix`() {
        withStudyPreviewDialog {
            compose.onNodeWithText("day", useUnmergedTree = true).assertIsDisplayed()

            input().performTextReplacement("2")
            compose.onNodeWithText("days", useUnmergedTree = true).assertIsDisplayed()

            input().performTextReplacement("1")
            compose.onNodeWithText("day", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "en")
    fun `'preview' positive button text is 'Create'`() {
        withStudyPreviewDialog { dialog ->
            dialog.assertTextContains("Create")
        }
    }

    @Test
    fun `'preview' and 'review ahead' hide the empty first label`() {
        withStudyPreviewDialog {
            assertThat(
                "preview",
                compose.onAllNodes(hasText(""), useUnmergedTree = true).fetchSemanticsNodes().any {
                    SemanticsProperties.EditableText !in
                        it.config
                },
                equalTo(false),
            )
        }
        withStudyAheadDialog {
            assertThat(
                "review ahead",
                compose.onAllNodes(hasText(""), useUnmergedTree = true).fetchSemanticsNodes().any {
                    SemanticsProperties.EditableText !in
                        it.config
                },
                equalTo(false),
            )
        }
    }

    @Test
    fun `'preview' disables Create when no cards match the criteria`() =
        runTest {
            addBasicNote()

            withStudyPreviewDialog { dialog ->
                input().performTextReplacement("1")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsEnabled()
                assertThat(inputError(), nullValue())

                input().performTextReplacement("0")
                shadowOf(Looper.getMainLooper()).idle()
                dialog.assertIsNotEnabled()
            }
        }

    @Test
    fun `'review ahead' validates the default value on open - shared path guard`() =
        runTest {
            addBasicNote().firstCard().update {
                queue = QueueType.Rev
                type = CardType.Rev
                due = col.sched.today + 30
            }

            withStudyAheadDialog { dialog ->
                dialog.assertIsNotEnabled()
            }
        }

    /** Adds a note to [deckName] whose card is due tomorrow */
    private fun addNoteDueTomorrow(deckName: String) {
        addBasicNote().update { moveToDeck(deckName, false) }.firstCard().update {
            queue = QueueType.Rev
            type = CardType.Rev
            due = col.sched.today + 1
        }
    }

    /**
     * Runs [block] on a [CustomStudyDialog]
     */
    private fun withCustomStudyFragment(
        args: Bundle,
        block: (CustomStudyDialog) -> Unit,
    ) {
        createHostActivity().use { controller ->
            val fragment = CustomStudyDialog().apply { arguments = args }
            controller.get().showDialogFragment(fragment)
            compose.waitForIdle()
            val start = System.nanoTime()
            while (fragment.dialog?.isShowing == true &&
                compose
                    .onAllNodes(
                        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                    ).fetchSemanticsNodes()
                    .isNotEmpty()
            ) {
                check(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(10)) { "Custom Study did not finish loading" }
                Thread.sleep(10)
                shadowOf(Looper.getMainLooper()).idleFor(16, TimeUnit.MILLISECONDS)
            }
            shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
            block(fragment)
        }
    }

    /** Opens the 'review ahead' subscreen for [deckId] and runs [block] once the dialog is shown and its buttons exist */
    private fun withStudyAheadDialog(
        deckId: DeckId = Consts.DEFAULT_DECK_ID,
        block: CustomStudyDialog.(dialog: androidx.compose.ui.test.SemanticsNodeInteraction) -> Unit,
    ) = withCustomStudyFragment(args = argumentsDisplayingSubscreen(ContextMenuOption.STUDY_AHEAD, deckId = deckId)) { fragment ->
        shadowOf(Looper.getMainLooper()).idle()
        fragment.block(confirmAction())
    }

    private fun withTypedInputCoveredByAnotherDialog(
        option: ContextMenuOption,
        typed: String,
        block: CustomStudyDialog.() -> Unit,
    ) {
        addBasicNote()
        val customStudy = CustomStudyDialog().apply { arguments = argumentsDisplayingSubscreen(option) }
        Robolectric.buildActivity(FragmentActivity::class.java).use { controller ->
            controller.get().setTheme(CommonR.style.Base_Theme_Light)
            val activity = controller.setup().get()
            activity.showDialogFragment(customStudy)
            input().performTextReplacement(typed)
            input().assertTextContains(typed)

            val dialogOnTop = DialogFragment()
            activity.showDialogFragment(dialogOnTop)
            shadowOf(Looper.getMainLooper()).idle()
            dialogOnTop.dismiss()
            shadowOf(Looper.getMainLooper()).idle()

            customStudy.block()
        }
    }

    /** Opens the 'preview' subscreen for [deckId] and runs [block] once the dialog is shown */
    private fun withStudyPreviewDialog(
        deckId: DeckId = Consts.DEFAULT_DECK_ID,
        block: CustomStudyDialog.(dialog: androidx.compose.ui.test.SemanticsNodeInteraction) -> Unit,
    ) = withCustomStudyFragment(args = argumentsDisplayingSubscreen(ContextMenuOption.STUDY_PREVIEW, deckId = deckId)) { fragment ->
        shadowOf(Looper.getMainLooper()).idle()
        fragment.block(confirmAction())
    }

    private fun withExtendLimitsDialog(
        option: ContextMenuOption,
        block: CustomStudyDialog.(dialog: androidx.compose.ui.test.SemanticsNodeInteraction) -> Unit,
    ) {
        // Main dismisses custom study when the deck is empty. These dialogs need a card so the
        // subscreen is shown instead of the empty-deck snackbar.
        addBasicNote()
        withCustomStudyFragment(args = argumentsDisplayingSubscreen(option)) { fragment ->
            shadowOf(Looper.getMainLooper()).idle()
            fragment.block(confirmAction())
        }
    }

    private fun mockCollectionWithSchedulerReturning(response: CustomStudyDefaultsResponse): Collection {
        val tree = col.sched.deckTree(includeCounts = false)
        return mockk<Collection>(relaxed = true) {
            every { decks.cardCount(Consts.DEFAULT_DECK_ID, includeSubdecks = true) } returns 1
            every { sched } returns
                mockk<Scheduler> {
                    every { deckTree(includeCounts = false) } returns tree
                    every { customStudyDefaults(Consts.DEFAULT_DECK_ID) } returns response
                }
        }
    }

    private fun argumentsDisplayingSubscreen(
        subscreen: ContextMenuOption,
        deckId: DeckId = Consts.DEFAULT_DECK_ID,
        restoreFromProcessDeath: Boolean = false,
    ): Bundle {
        @Suppress("RedundantValueArgument")
        fun setupDefaultValuesSingleton() {
            withCustomStudyFragment(argumentsDisplayingMainScreen(deckId = deckId)) { }
        }

        if (!restoreFromProcessDeath) {
            setupDefaultValuesSingleton()
        }

        return requireNotNull(
            CustomStudyDialog
                .createSubDialog(
                    deckId = deckId,
                    contextMenuAttribute = subscreen,
                ).arguments,
        )
    }

    private fun argumentsDisplayingMainScreen(deckId: DeckId = Consts.DEFAULT_DECK_ID) =
        requireNotNull(
            CustomStudyDialog
                .createInstance(
                    deckId = deckId,
                ).arguments,
        )

    private fun input() = compose.onNode(hasSetTextAction())

    private fun inputText() = input().fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun inputError() = input().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)

    private fun confirmAction() =
        compose.onNode(
            hasText(targetContext.getString(CommonString.dialog_positive_create)) or
                hasText(targetContext.getString(CommonString.custom_study_increase)) or
                hasText(targetContext.getString(CommonString.dialog_ok)),
        )

    private fun progress() = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

    private fun createHostActivity() =
        Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup().apply {
            val activity = get()
            activity.setContentView(CoordinatorLayout(activity).apply { id = R.id.root_layout })
        }

    private fun assertUnavailableMenu(fragment: CustomStudyDialog) {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        assertFalse(fragment.dialog?.isShowing == true)
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(allOf(isDisplayed(), withText(CommonString.empty_deck))))
    }

    private fun CustomStudyDialog.submitSubscreenData() = confirmAction().performClick()

    /** Set the card state to [state] and also verify the selection in the fragment's ViewModel */
    private fun CustomStudyDialog.setCardStateSelectionTo(state: CustomStudyDialog.CustomStudyCardState) {
        assertThat(
            state.ordinal,
            allOf(
                greaterThanOrEqualTo(0),
                lessThan(CustomStudyDialog.CustomStudyCardState.entries.size),
            ),
        )
        compose
            .onNodeWithText(
                CustomStudyDialog.CustomStudyCardState.entries[viewModel.selectedCardStateIndex].labelProducer(),
            ).performClick()
        compose.onNodeWithText(state.labelProducer()).performClick()
        assertThat(viewModel.selectedCardStateIndex, equalTo(state.ordinal))
    }

    /**
     * The current backend value of [CustomStudyDialog.CustomStudyDefaults] for the default deck
     * */
    private val defaultsOfDefaultDeck
        get() = col.sched.customStudyDefaults(Consts.DEFAULT_DECK_ID).toDomainModel()
}
