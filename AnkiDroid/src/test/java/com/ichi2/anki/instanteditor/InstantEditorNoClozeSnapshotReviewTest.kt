// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.instanteditor

import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.instantnoteeditor.InstantEditorViewModel
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorFragment
import com.ichi2.testutils.withCollectionAccessOn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
class InstantEditorNoClozeSnapshotReviewTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `confirmation saves the fields that produced the no-cloze warning`() =
        runTest {
            val originalText = "Hello world"
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, originalText)
                }
            val activity = openSharedInstantNote(intent, compose).get()
            compose.onNodeWithContentDescription(getResourceString(CommonString.change_editor_mode)).performClick()
            val editor = requireNotNull(ShadowDialog.getLatestDialog())
            val viewModel = ViewModelProvider(checkNotNull(activity.currentScreenFragment))[InstantEditorViewModel::class.java]
            val notesBefore = col.noteCount()

            withCollectionAccessOn(StandardTestDispatcher(testScheduler)) {
                compose.onNodeWithText(getResourceString(CommonString.menu_add)).performClick()
                // This window remains interactive while validation waits in the collection queue.
                compose.onNode(hasSetTextAction() and hasText(originalText)).performTextReplacement("")
            }
            advanceUntilIdle()
            val warning = requireNotNull(ShadowDialog.getLatestDialog())
            assertTrue("no-cloze warning must be shown", warning !== editor && warning.isShowing)
            compose.onNodeWithText(CollectionManager.TR.addingYouHaveAClozeDeletionNote()).assertIsDisplayed()
            compose.onNodeWithText(CollectionManager.TR.actionsSave()).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            advanceUntilIdle()

            compose.waitUntil(10_000) { activity.currentScreenFragment !is InstantNoteEditorFragment }
            assertEquals("one note added", notesBefore + 1, col.noteCount())
            assertEquals(
                "confirmation must not insert unvalidated replacement fields",
                originalText,
                col.getNote(viewModel.editorNote.id).fields[0],
            )
        }
}
