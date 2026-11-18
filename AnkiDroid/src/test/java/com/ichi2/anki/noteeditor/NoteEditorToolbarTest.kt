// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.noteeditor

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.NoteEditorActivity
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.common.preferences.sharedPrefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NoteEditorToolbarTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val customButton get() = composeRule.onNodeWithContentDescription("BR")

    private fun editCustomButton() {
        customButton.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
    }

    @Test
    fun `clearing the text before the cursor saves a button that inserts after the cursor`() {
        checkEditedButton(prefix = "", suffix = "<br>", expectedCursor = 0)
    }

    @Test
    fun `clearing the text after the cursor saves a button that inserts before the cursor`() {
        checkEditedButton(prefix = "<br>", suffix = "", expectedCursor = 4)
    }

    @Test
    fun `editing requires text on at least one side of the cursor`() {
        openEditor()
        editCustomButton()
        val before = toolbarField(CommonString.before_text)
        val after = toolbarField(CommonString.after_text)
        val save = toolbarAction(CommonString.save)

        save.assertIsEnabled()
        before.performTextReplacement("")
        save.assertIsEnabled()
        after.performTextReplacement("")
        save.assertIsNotEnabled()

        after.performTextReplacement("<br>")
        save.assertIsEnabled()
        after.performTextReplacement(" ")
        save.assertIsNotEnabled()
        before.performTextReplacement("<br>")
        save.assertIsEnabled()
        toolbarAction(CommonString.dialog_cancel).performClick()
    }

    private fun checkEditedButton(
        prefix: String,
        suffix: String,
        expectedCursor: Int,
    ) {
        val editor = openEditor()
        editCustomButton()
        toolbarField(CommonString.before_text).performTextReplacement(prefix)
        toolbarField(CommonString.after_text).performTextReplacement(suffix)
        toolbarAction(CommonString.save).performClick()

        val field = editor.getFieldForTest(0)
        composeRule.runOnIdle { field.requestFocus() }
        customButton.performClick()
        assertEquals("<br>", field.text.toString())
        assertEquals(expectedCursor, field.selectionStart)
        assertEquals(expectedCursor, field.selectionEnd)

        // Reopening reads the saved button from preferences.
        editCustomButton()
        assertEquals(prefix, fieldValue(CommonString.before_text))
        assertEquals(suffix, fieldValue(CommonString.after_text))
        toolbarAction(CommonString.dialog_cancel).performClick()
    }

    private fun toolbarField(label: Int) = composeRule.onNode(hasSetTextAction() and hasText(targetContext.getString(label)))

    private fun toolbarAction(label: Int) = composeRule.onNodeWithText(targetContext.getString(label))

    private fun fieldValue(label: Int) = toolbarField(label).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun openEditor() =
        run {
            // Start with both fields populated so saving must clear an existing value.
            targetContext.sharedPrefs().edit {
                putStringSet(
                    "note_editor_custom_buttons",
                    CustomToolbarButton.toStringSet(arrayListOf(CustomToolbarButton(0, "BR", "<b>", "</b>"))),
                )
            }
            startRegularActivity<NoteEditorActivity>(NoteEditorDestination.AddNote().toIntent()).getNoteEditorFragment()
        }
}
