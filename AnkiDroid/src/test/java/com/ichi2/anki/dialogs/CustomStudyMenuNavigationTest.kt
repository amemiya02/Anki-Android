// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.anki.ioDispatcher
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.testutils.EmptyAnkiActivity
import com.ichi2.testutils.uninitializeField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CustomStudyMenuNavigationTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    override fun setUp() {
        super.setUp()
        uninitializeField<CustomStudyDialog>("deferredInitialization")
    }

    @Test
    fun `recreated menu rejects a selection from its previous dialog`() =
        runTest {
            val (activity, fragment) = openMenu()
            val originalDialog = fragment.requireDialog()
            withQueuedChecks(this) { collectionScheduler ->
                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()

                activity.showDialogFragment(DialogFragment())
                assertFalse(originalDialog.isShowing)
                assertFalse(fragment.isResumed)
                assertTrue(activity.supportFragmentManager.popBackStackImmediate())
                assertTrue(fragment.isResumed)
                val recreatedDialog = fragment.requireDialog()
                assertTrue(recreatedDialog !== originalDialog)

                // The old dialog's result arrives before the recreated menu finishes loading.
                collectionScheduler.runCurrent()
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()
                collectionScheduler.runCurrent()
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()

                assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
                assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
                assertTrue(recreatedDialog.isShowing)
                compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertDoesNotExist()

                // Ignoring the old result must still allow a new selection from the recreated menu.
                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()
                collectionScheduler.runCurrent()
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()

                val input = activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as CustomStudyDialog
                assertTrue(input.requireDialog().isShowing)
                compose.onNode(hasSetTextAction()).assertIsDisplayed()
                assertEquals(2, activity.supportFragmentManager.backStackEntryCount)
            }
        }

    @Test
    fun `native cancellation must not reopen the menu as an input dialog`() =
        runTest {
            val (activity, fragment) = openMenu()
            val originalDialog = fragment.requireDialog()
            withQueuedChecks(this) { collectionScheduler ->
                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()
                collectionScheduler.runCurrent()

                originalDialog.cancel()
                assertFalse(originalDialog.isShowing)
                assertTrue(fragment.isResumed, "The native dismissal callback is still queued")

                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()
                activity.supportFragmentManager.executePendingTransactions()

                assertNull(activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
            }
        }

    @Test
    fun `a queued menu result must not reopen a dismissed dialog`() =
        runTest {
            val (activity, fragment) = openMenu()
            val originalDialog = fragment.requireDialog()
            withQueuedChecks(this) { collectionScheduler ->
                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()
                assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))

                // Finish the backend read, leaving its UI continuation queued.
                collectionScheduler.runCurrent()
                fragment.dismiss()
                assertFalse(originalDialog.isShowing)
                assertTrue(fragment.isResumed, "The dismissal transaction has not executed yet")

                // The read result arrives before FragmentManager processes the pending dismissal.
                runCurrent()
                activity.supportFragmentManager.executePendingTransactions()

                assertNull(activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
            }
        }

    @Test
    fun `a queued menu result must not open an input dialog when the activity is finishing`() =
        runTest {
            val (activity, fragment) = openMenu()
            withQueuedChecks(this) { collectionScheduler ->
                compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                runCurrent()
                collectionScheduler.runCurrent()

                activity.finish()
                assertTrue(activity.isFinishing)
                assertTrue(fragment.isResumed, "The activity has not paused yet")
                assertTrue(fragment.requireDialog().isShowing)
                assertFalse(activity.supportFragmentManager.isStateSaved)

                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()
                activity.supportFragmentManager.executePendingTransactions()

                assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
                assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
            }
        }

    @Test
    fun `two queued menu taps open exactly one input dialog`() = assertTwoQueuedTapsOpenOneInput(childHosted = false)

    @Test
    fun `two queued taps in a child-hosted menu open exactly one input dialog`() = assertTwoQueuedTapsOpenOneInput(childHosted = true)

    private fun assertTwoQueuedTapsOpenOneInput(childHosted: Boolean) =
        runTest {
            val (activity, fragment) = openMenu(childHosted)
            withQueuedChecks(this) { collectionScheduler ->
                repeat(2) {
                    compose.onNodeWithText(TR.customStudyReviewAhead()).performClick()
                    runCurrent()
                    assertTrue(fragment.requireDialog().isShowing)
                }
                collectionScheduler.runCurrent()
                runCurrent()
                collectionScheduler.runCurrent()
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()

                val dialogs = activity.supportFragmentManager.fragments.filterIsInstance<CustomStudyDialog>()
                assertEquals(1, dialogs.size)
                assertEquals(if (childHosted) 1 else 2, activity.supportFragmentManager.backStackEntryCount)
                assertTrue(dialogs.single().requireDialog().isShowing)
                compose.onNode(hasSetTextAction()).assertIsDisplayed()
            }
        }

    private fun openMenu(childHosted: Boolean = false): Pair<EmptyAnkiActivity, CustomStudyDialog> {
        addBasicNote()
        val controller = Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup()
        saveControllerForCleanup(controller)
        advanceRobolectricLooper()
        val activity = controller.get()
        val fragment = CustomStudyDialog.createInstance(Consts.DEFAULT_DECK_ID)
        if (childHosted) {
            // A fragment may host Custom Study in its child fragment manager.
            val parent = Fragment()
            activity.supportFragmentManager
                .beginTransaction()
                .add(parent, "host")
                .commitNow()
            fragment.show(parent.childFragmentManager, null)
            parent.childFragmentManager.executePendingTransactions()
        } else {
            activity.showDialogFragment(fragment)
        }
        compose.onNodeWithText(TR.customStudyReviewAhead()).assertIsDisplayed()
        return activity to fragment
    }

    private fun withQueuedChecks(
        scope: TestScope,
        block: (TestCoroutineScheduler) -> Unit,
    ) {
        val previousIo = ioDispatcher
        val mainDispatcher = StandardTestDispatcher(scope.testScheduler)
        val collectionScheduler = TestCoroutineScheduler()
        val previousCollection = CollectionManager.setTestDispatcher(StandardTestDispatcher(collectionScheduler), useReentrantLock = false)
        Dispatchers.setMain(mainDispatcher)
        ioDispatcher = mainDispatcher
        try {
            block(collectionScheduler)
        } finally {
            try {
                repeat(3) {
                    collectionScheduler.runCurrent()
                    scope.runCurrent()
                    shadowOf(Looper.getMainLooper()).idle()
                }
            } finally {
                CollectionManager.setTestDispatcher(previousCollection)
                ioDispatcher = previousIo
                Dispatchers.setMain(previousIo)
            }
        }
    }
}
