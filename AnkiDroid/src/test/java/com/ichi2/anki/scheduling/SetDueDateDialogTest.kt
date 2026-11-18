// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.scheduling

import android.os.Bundle
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.libanki.CardId
import com.ichi2.anki.libanki.sched.SetDueDateDays
import com.ichi2.anki.scheduling.SetDueDateViewModel.Tab
import com.ichi2.anki.servicelayer.getFSRSStatus
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.anki.utils.ext.requireParcelable
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowToast
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class SetDueDateDialogTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val confirm get() = composeRule.onNodeWithText(getResourceString(CommonString.dialog_ok))
    private val changeInterval get() = composeRule.onNodeWithText(getResourceString(CommonString.set_due_date_match_interval))
    private val singleDay get() = composeRule.onNode(hasSetTextAction())
    private val rangeStart get() =
        composeRule.onNode(hasText(getResourceString(CommonString.set_due_date_range_start)) and hasSetTextAction())
    private val rangeEnd get() = composeRule.onNode(hasText(getResourceString(CommonString.set_due_date_range_end)) and hasSetTextAction())

    private fun select(tab: Tab) = composeRule.onNodeWithText(getResourceString(tab.text)).performClick().assertIsSelected()

    @Test
    fun `switch tabs`() =
        testDialog {
            select(Tab.SINGLE_DAY)
            assertThat(viewModel.currentTab, equalTo(Tab.SINGLE_DAY))
            select(Tab.DATE_RANGE)
            assertThat(viewModel.currentTab, equalTo(Tab.DATE_RANGE))
        }

    @Test
    fun `initial suffix is set`() =
        testDialog {
            select(Tab.SINGLE_DAY)
            composeRule.onAllNodesWithText("days", useUnmergedTree = true).assertCountEquals(1)
            select(Tab.DATE_RANGE)
            composeRule.onAllNodesWithText("days", useUnmergedTree = true).assertCountEquals(2)
        }

    @Test
    fun `set single day`() =
        testDialog {
            confirm.assertIsNotEnabled()
            singleDay.performTextReplacement("1")
            confirm.assertIsEnabled()
        }

    @Test
    fun `set date range`() =
        testDialog {
            select(Tab.DATE_RANGE)
            confirm.assertIsNotEnabled()
            rangeStart.performTextReplacement("1")
            rangeEnd.performTextReplacement("5")
            confirm.assertIsEnabled()
        }

    @Test
    fun `set update interval`() =
        testDialog {
            assertThat(viewModel.updateIntervalToMatchDueDate, equalTo(false))
            changeInterval.performScrollTo().performClick()
            assertThat(viewModel.updateIntervalToMatchDueDate, equalTo(true))
        }

    @Test
    fun `singular text`() =
        testDialog(cardCount = 1) {
            singleDay.assertTextContains("Show card in")
            select(Tab.DATE_RANGE)
            composeRule.onNodeWithText("Show card in range").assertIsDisplayed()
        }

    @Test
    fun `plural text`() =
        testDialog(cardCount = 2) {
            singleDay.assertTextContains("Show cards in")
            select(Tab.DATE_RANGE)
            composeRule.onNodeWithText("Show cards in range").assertIsDisplayed()
        }

    @Test
    fun `integration test`() =
        testDialog {
            assertThat(viewModel.updateIntervalToMatchDueDate, equalTo(false))
            select(Tab.DATE_RANGE)
            rangeStart.performTextReplacement("1")
            rangeEnd.performTextReplacement("2")
            changeInterval.performScrollTo().performClick()
            assertThat(viewModel.calculateDaysParameter(), equalTo(SetDueDateDays("1-2!")))
        }

    @Test
    fun `single day input limited to 5 digits`() =
        testDialog {
            singleDay.performTextReplacement("123456").assertTextContains("12345")
        }

    @Test
    fun `range start input limited to 5 digits`() =
        testDialog {
            select(Tab.DATE_RANGE)
            rangeStart.performTextReplacement("123456").assertTextContains("12345")
        }

    @Test
    fun `range end input limited to 5 digits`() =
        testDialog {
            select(Tab.DATE_RANGE)
            rangeEnd.performTextReplacement("123456").assertTextContains("12345")
        }

    @Test
    fun `card ids are readable after recreation`() =
        runTest {
            val cardIds = List(2) { addBasicNote().firstCard().id }
            launchFragment<SetDueDateDialog>(
                themeResId = CommonR.style.Base_Theme_Light,
                fragmentArgs = setDueDateArgs(cardIds),
            ).use { scenario ->
                advanceRobolectricLooper()
                scenario.recreate()
                advanceRobolectricLooper()
                scenario.onFragment { fragment ->
                    assertThat(fragment.cardIds, equalTo(cardIds))
                }
            }
        }

    @Test
    fun `ids file is removed after the dialog is dismissed`() =
        runTest {
            val cardIds = List(2) { addBasicNote().firstCard().id }
            val args = setDueDateArgs(cardIds)
            val idsFile = args.requireParcelable<IdsFile>(SetDueDateDialog.ARG_IDS_FILE)

            launchFragment<SetDueDateDialog>(
                themeResId = CommonR.style.Base_Theme_Light,
                fragmentArgs = args,
            ).use { scenario ->
                advanceRobolectricLooper()
                assertThat("kept while the dialog is open", idsFile.exists(), equalTo(true))
                scenario.onFragment { it.dismiss() }
                advanceRobolectricLooper()
            }

            assertThat("removed once dismissed", idsFile.exists(), equalTo(false))
        }

    @Test
    fun `missing selection dismisses due date dialog`() =
        withActivity { activity ->
            val card = addBasicNote().firstCard()
            val originalDue = card.due
            val args = setDueDateArgs(listOf(card.id))
            assertTrue(args.requireParcelable<IdsFile>(SetDueDateDialog.ARG_IDS_FILE).delete())
            val fragment = SetDueDateDialog().apply { arguments = args }
            fragment.show(activity.supportFragmentManager, "due-date")
            advanceRobolectricLooper()
            assertNull(activity.supportFragmentManager.findFragmentByTag("due-date"))
            assertThat(col.getCard(card.id).due, equalTo(originalDue))
            assertThat(
                ShadowToast.getTextOfLatestToast(),
                equalTo(targetContext.getString(CommonString.something_wrong)),
            )
        }

    @Test
    fun `unreadable ids file does not crash`() =
        runTest {
            val cardIds = List(2) { addBasicNote().firstCard().id }
            val args = setDueDateArgs(cardIds)
            args.requireParcelable<IdsFile>(SetDueDateDialog.ARG_IDS_FILE).writeBytes(ByteArray(0))

            launchFragment<SetDueDateDialog>(
                themeResId = CommonR.style.Base_Theme_Light,
                fragmentArgs = args,
            ).use {
                advanceRobolectricLooper()
            }
        }

    @Test
    fun `loading FSRS forces the interval to match the due date`() = assertSchedulerLoading(fsrsEnabled = true)

    @Test
    fun `loading SM-2 allows changing the interval`() = assertSchedulerLoading(fsrsEnabled = false)

    @Test
    fun `unavailable scheduler setting falls back to SM-2`() = assertSchedulerLoading(fsrsEnabled = null)

    @Test
    fun `show and duplicate requests return without waiting for loading`() =
        withActivity { activity ->
            val firstCardIds = listOf(addBasicNote().firstCard().id)
            val secondCardIds = listOf(addBasicNote().firstCard().id)
            withPausedLoading { finishLoading ->
                SetDueDateDialog.show(activity, firstCardIds)
                SetDueDateDialog.show(activity, secondCardIds)
                advanceRobolectricLooper()

                assertEquals(firstCardIds, assertNotNull(activity.currentDialog).cardIds)
                assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
                assertEquals(1, activity.dueDateFiles.size)
                finishLoading.complete(Unit)
            }
        }

    @Test
    fun `dialog can reopen after dismissal`() =
        withActivity { activity ->
            val firstCardIds = listOf(addBasicNote().firstCard().id)
            val secondCardIds = listOf(addBasicNote().firstCard().id)
            SetDueDateDialog.show(activity, firstCardIds)
            advanceRobolectricLooper()
            val dialog = assertNotNull(activity.currentDialog)

            dialog.dismiss()
            advanceRobolectricLooper()
            assertNull(activity.currentDialog)

            SetDueDateDialog.show(activity, secondCardIds)
            advanceRobolectricLooper()
            val reopenedDialog = assertNotNull(activity.currentDialog)
            assertNotSame(dialog, reopenedDialog)
            assertEquals(secondCardIds, reopenedDialog.cardIds)
            assertTrue(reopenedDialog.requireDialog().isShowing)
        }

    @Test
    fun `dialog can be dismissed and reopened while loading`() =
        withActivity { activity ->
            val firstCardIds = listOf(addBasicNote().firstCard().id)
            val secondCardIds = listOf(addBasicNote().firstCard().id)
            withPausedLoading(fsrsEnabled = true) { finishLoading ->
                SetDueDateDialog.show(activity, firstCardIds)
                advanceRobolectricLooper()
                assertNotNull(activity.currentDialog).dismiss()
                advanceRobolectricLooper()
                assertNull(activity.currentDialog)
                assertTrue(activity.dueDateFiles.isEmpty())

                coEvery { getFSRSStatus() } returns false
                SetDueDateDialog.show(activity, secondCardIds)
                finishLoading.complete(Unit)
                advanceUntilIdle()
                advanceRobolectricLooper()

                val dialog = assertNotNull(activity.currentDialog)
                assertEquals(secondCardIds, dialog.cardIds)
                assertTrue(dialog.requireDialog().isShowing)
                changeInterval.performScrollTo().assertIsDisplayed()
                assertFalse(dialog.viewModel.fsrsEnabled.value!!)
            }
        }

    private fun assertSchedulerLoading(fsrsEnabled: Boolean?) =
        withActivity { activity ->
            val cardIds = listOf(addBasicNote().firstCard().id)
            withPausedLoading(fsrsEnabled) { finishLoading ->
                SetDueDateDialog.show(activity, cardIds)
                advanceRobolectricLooper()
                val dialog = assertNotNull(activity.currentDialog)
                singleDay.performTextReplacement("3")
                confirm.assertIsNotEnabled()
                changeInterval.assertDoesNotExist()
                singleDay.performImeAction()
                assertEquals(dialog, activity.currentDialog, "keyboard submission cannot save while loading")

                finishLoading.complete(Unit)
                advanceUntilIdle()
                advanceRobolectricLooper()

                confirm.assertIsEnabled()
                if (fsrsEnabled == true) changeInterval.assertDoesNotExist() else changeInterval.performScrollTo().assertIsDisplayed()
                assertEquals(fsrsEnabled == true, dialog.viewModel.updateIntervalToMatchDueDate)
                assertEquals(SetDueDateDays(if (fsrsEnabled == true) "3!" else "3"), dialog.viewModel.calculateDaysParameter())
            }
        }

    private val FragmentActivity.currentDialog: SetDueDateDialog?
        get() = supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as SetDueDateDialog?

    private val FragmentActivity.dueDateFiles: List<File>
        get() =
            IdsFile
                .getDirectory()
                .listFiles { _, name -> name.startsWith("set-due-date") }
                .orEmpty()
                .toList()

    private fun withActivity(block: suspend TestScope.(FragmentActivity) -> Unit) =
        runTest {
            Robolectric.buildActivity(FragmentActivity::class.java).use { controller ->
                controller.get().setTheme(CommonR.style.Base_Theme_Light)
                block(controller.setup().get())
            }
        }

    private suspend fun withPausedLoading(
        fsrsEnabled: Boolean? = false,
        block: suspend (CompletableDeferred<Unit>) -> Unit,
    ) {
        val finishLoading = CompletableDeferred<Unit>()
        mockkStatic(::getFSRSStatus)
        try {
            coEvery { getFSRSStatus() } coAnswers {
                finishLoading.await()
                fsrsEnabled
            }
            block(finishLoading)
        } finally {
            unmockkStatic(::getFSRSStatus)
        }
    }

    private fun setDueDateArgs(cardIds: List<CardId>): Bundle =
        SetDueDateDialog
            .newInstance(targetContext.externalCacheDir ?: targetContext.cacheDir, cardIds)
            .requireArguments()

    private fun testDialog(
        cardCount: Int = 1,
        action: SetDueDateDialog.() -> Unit,
    ) = runTest {
        val cardIds = List(cardCount) { addBasicNote().firstCard().id }
        val dialog = SetDueDateDialog.newInstance(targetContext.externalCacheDir ?: targetContext.cacheDir, cardIds)
        launchFragment(
            themeResId = CommonR.style.Base_Theme_Light,
            fragmentArgs = dialog.arguments,
        ) {
            return@launchFragment dialog
        }.use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            advanceRobolectricLooper()
            action(dialog)
        }
    }
}
