// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.instanteditor

import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentDialog
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.instantnoteeditor.InstantEditorViewModel
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorFragment
import com.ichi2.anki.progress.ViewModelProgress
import com.ichi2.testutils.withCollectionAccessOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import kotlin.coroutines.CoroutineContext

@RunWith(AndroidJUnit4::class)
class InstantNoteEditorActivityTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `ignored Save does not overwrite the note being saved`() =
        runTest {
            val originalText = "{{c1::Hello}} world"
            val activity = startInstantNoteEditor(originalText)
            compose.onNodeWithContentDescription(getResourceString(CommonString.change_editor_mode)).performClick()
            val viewModel = ViewModelProvider(checkNotNull(activity.currentScreenFragment))[InstantEditorViewModel::class.java]
            val notesBefore = col.noteCount()
            val pendingOperations = ArrayDeque<Runnable>()
            val dispatcher =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        pendingOperations.addLast(block)
                    }
                }

            withCollectionAccessOn(dispatcher) {
                compose.onNodeWithText(getResourceString(CommonString.menu_add)).performClick()
                assertTrue(viewModel.progressManager.progress.value is ViewModelProgress.Active)

                // Complete field validation, but leave insertion queued while the dialog is interactive.
                pendingOperations.removeFirst().run()
                assertEquals("note insertion is pending", 1, pendingOperations.size)
                compose.onNode(hasSetTextAction() and hasText(originalText)).performTextReplacement("")
                compose.onNodeWithText(getResourceString(CommonString.menu_add)).performClick()

                while (pendingOperations.isNotEmpty()) {
                    pendingOperations.removeFirst().run()
                }
            }
            advanceUntilIdle()

            assertTrue("editor closes after saving", activity.currentScreenFragment !is InstantNoteEditorFragment)
            assertEquals("notes added", notesBefore + 1, col.noteCount())
            assertEquals("saved fields must match the validated fields", originalText, col.getNote(viewModel.editorNote.id).fields[0])
        }

    @Test
    fun `tapping Save twice in the no-cloze confirmation adds one note`() =
        runTest {
            startInstantNoteEditor("Hello world")
            val notesBefore = col.noteCount()
            compose.onNodeWithText(getResourceString(CommonString.menu_add)).performClick()
            advanceUntilIdle()
            val save = compose.onNodeWithText(TR.actionsSave()).assertIsDisplayed()

            withCollectionAccessOn(StandardTestDispatcher(testScheduler)) {
                save.performTouchInput { doubleClick() }
                shadowOf(Looper.getMainLooper()).idle()
            }
            advanceUntilIdle()
            assertEquals("notes added", notesBefore + 1, col.noteCount())
        }

    @Test
    fun `tapping Save twice while the note is saving adds one note`() =
        runTest {
            startInstantNoteEditor("{{c1::Hello}} world")
            val notesBefore = col.noteCount()

            withCollectionAccessOn(StandardTestDispatcher(testScheduler)) {
                repeat(2) {
                    compose.onNodeWithText(getResourceString(CommonString.menu_add)).performClick()
                }
            }
            advanceUntilIdle()

            assertEquals("notes added", notesBefore + 1, col.noteCount())
        }

    @Test
    fun `tapping Save again after the note is saved adds one note`() =
        runTest {
            val activity = startInstantNoteEditor("{{c1::Hello}} world")
            val notesBefore = col.noteCount()

            val save = compose.onNodeWithText(getResourceString(CommonString.menu_add))
            val submit = checkNotNull(save.fetchSemanticsNode().config[SemanticsActions.OnClick].action)
            save.performClick()
            advanceUntilIdle()
            assertTrue("editor is closing", activity.currentScreenFragment !is InstantNoteEditorFragment)

            submit()
            advanceUntilIdle()

            assertEquals("notes added", notesBefore + 1, col.noteCount())
        }

    @Test
    fun `back while the note is saving does not close the editor`() =
        runTest {
            val activity = startInstantNoteEditor("{{c1::Hello}} world")

            withCollectionAccessOn(StandardTestDispatcher(testScheduler)) {
                compose.onNodeWithText(getResourceString(CommonString.menu_add)).performClick()
                activity.onBackPressedDispatcher.onBackPressed()

                assertTrue("editor closed while saving", activity.currentScreenFragment is InstantNoteEditorFragment)
            }
            advanceUntilIdle()

            assertTrue("editor closes once the note is saved", activity.currentScreenFragment !is InstantNoteEditorFragment)
        }

    @Test
    fun `Back from the dialog returns to the sharing app`() {
        val activity = startInstantNoteEditor("Shared text")
        compose.onNodeWithText("Shared").assertIsDisplayed()
        val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
        dialog.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(10_000) { shadowOf(activity).isTaskMovedToBack }
        assertTrue(activity.currentScreenFragment !is InstantNoteEditorFragment)
    }

    private fun startInstantNoteEditor(text: String): DeckPicker {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        return openSharedInstantNote(intent)
    }
}
