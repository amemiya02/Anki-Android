// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Chaitanya Medidar <2023.chaitanya.medidar@ves.ac.in>

package com.ichi2.anki.notetype

import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.LoadingDialogFragment
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageNotetypesTest : RobolectricTest() {
    @Test
    fun `deleting a note type blocks touches until it finishes`() =
        runTest {
            ensureCollectionLoadIsSynchronous()
            addStandardNoteType(TEST_NOTE_TYPE_NAME, arrayOf("front", "back"), "", "")
            val activity = startRegularActivity<ManageNotetypes>()
            val noteType =
                activity.viewModel.state.value.noteTypes
                    .first { it.name == TEST_NOTE_TYPE_NAME }

            withQueuedCollectionAccess {
                activity.viewModel.delete(noteType.id)
                assertTrue("touches blocked while deleting", activity.touchesBlocked())
            }
            advanceUntilIdle()

            assertFalse("touches restored after deleting", activity.touchesBlocked())
            assertTrue(
                "note type deleted",
                activity.viewModel.state.value.noteTypes
                    .none { it.id == noteType.id },
            )
        }

    @Test
    fun `back during the progress delay does not finish an activity deleting note types`() =
        runTest {
            ensureCollectionLoadIsSynchronous()
            addStandardNoteType(TEST_NOTE_TYPE_NAME, arrayOf("front", "back"), "", "")
            val activity = startRegularActivity<ManageNotetypes>()
            val noteType =
                activity.viewModel.state.value.noteTypes
                    .first { it.name == TEST_NOTE_TYPE_NAME }
            activity.viewModel.onItemLongClick(noteType)

            try {
                withQueuedCollectionAccess {
                    activity.viewModel.deleteSelectedNoteTypes()
                    assertTrue("deletion is still in progress", activity.touchesBlocked())
                    assertFalse(
                        "the progress dialog has not appeared yet",
                        activity.supportFragmentManager.fragments.any { it is LoadingDialogFragment },
                    )

                    activity.onBackPressedDispatcher.onBackPressed()

                    assertFalse("Back must not finish the activity during deletion", activity.isFinishing)
                }
            } finally {
                advanceUntilIdle()
            }
        }

    private suspend fun <T> TestScope.withQueuedCollectionAccess(block: suspend () -> T): T {
        val previousQueue = CollectionManager.setTestDispatcher(StandardTestDispatcher(testScheduler), useReentrantLock = false)
        try {
            return block()
        } finally {
            CollectionManager.setTestDispatcher(previousQueue)
        }
    }

    private fun ManageNotetypes.touchesBlocked() = window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0

    companion object {
        private const val TEST_NOTE_TYPE_NAME = "BasicTestNoteType1"
    }
}
