// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.backend.backendError
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.backend.DatabaseCorruption
import com.ichi2.anki.dialogs.DatabaseErrorDialog
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.addNotetypeLegacy
import com.ichi2.anki.progress.ViewModelProgress
import com.ichi2.anki.throwOnShowError
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbCorruptException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ManageNoteTypesFailureRecreationReviewTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `invalid clone without recreation shows validation`() = checkFailure(corruption = false, recreate = false)

    @Test
    fun `invalid clone after recreation shows validation`() = checkFailure(corruption = false, recreate = true)

    @Test
    fun `corruption without recreation opens recovery`() = checkFailure(corruption = true, recreate = false)

    @Test
    fun `corruption after recreation opens recovery`() = checkFailure(corruption = true, recreate = true)

    private fun checkFailure(
        corruption: Boolean,
        recreate: Boolean,
    ) = runTest {
        throwOnShowError = false
        val collection = col
        val sourceName = "Invalid legacy note type"
        val newName = "Creation that must fail"
        if (!corruption) addStandardNoteType(sourceName, arrayOf("front", "back"), "", "")
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(ManageNotetypes::class.java, Intent())
        val activity = controller.get()
        val viewModel = activity.viewModel
        compose.openAddType()
        if (!corruption) compose.chooseNoteType(sourceName)
        compose.fillName(newName)
        val queue = StepDispatcher()
        val previousQueue = CollectionManager.setTestDispatcher(queue, useReentrantLock = false)
        val previousCorruption = DatabaseCorruption.isDetected
        DatabaseCorruption.isDetected = false
        var attemptedAdd = false
        if (corruption) {
            mockkStatic(Collection::addNotetypeLegacy)
            every { any<Collection>().addNotetypeLegacy(any()) } answers {
                attemptedAdd = true
                throw BackendDbCorruptException(backendError {})
            }
        }
        try {
            compose.confirmAdd()
            advanceRobolectricLooper()
            assertTrue(queue.hasTasks, "creation is still waiting for collection access")
            assertIs<ViewModelProgress.Active<Nothing>>(viewModel.progressManager.progress.value)

            if (recreate) {
                // Permit synchronous collection reads during activity startup, while the submitted
                // operation remains on the queue, as in the PR's success recreation tests.
                CollectionManager.setTestDispatcher(queue)
                val configuration =
                    Configuration(activity.resources.configuration).apply {
                        val night =
                            if (uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                                Configuration.UI_MODE_NIGHT_NO
                            } else {
                                Configuration.UI_MODE_NIGHT_YES
                            }
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    }
                controller.configurationChange(configuration)
                advanceRobolectricLooper()
                assertNotSame(activity, controller.get())
                assertSame(viewModel, controller.get().viewModel)
                assertTrue(activity.isDestroyed)
            }

            queue.runAll()
            advanceRobolectricLooper()
            assertIs<ViewModelProgress.Idle>(viewModel.progressManager.progress.value)
            assertNull(collection.notetypes.byName(newName), "the failed operation must not create a note type")
            if (corruption) {
                assertTrue(attemptedAdd, "the retained operation must actually run and fail")
                assertTrue(DatabaseCorruption.isDetected, "corruption must still be marked for recovery after recreation")
                assertTrue(
                    controller
                        .get()
                        .supportFragmentManager.fragments
                        .any { it is DatabaseErrorDialog },
                    "the current activity must receive database recovery",
                )
            } else {
                compose.onNodeWithText("problem", substring = true).assertIsDisplayed()
            }
        } finally {
            CollectionManager.setTestDispatcher(previousQueue)
            queue.runAll()
            if (corruption) unmockkStatic(Collection::addNotetypeLegacy)
            DatabaseCorruption.isDetected = previousCorruption
        }
    }

    private class StepDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        val hasTasks: Boolean get() = tasks.isNotEmpty()

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            tasks.addLast(block)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }
}
