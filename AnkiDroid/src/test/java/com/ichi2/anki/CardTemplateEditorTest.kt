// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2020 Mike Hardy <mike@mikehardy.net>

package com.ichi2.anki

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardTemplateEditor.CardTemplate
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.common.utils.ext.deepClonedInto
import com.ichi2.anki.dialogs.InsertFieldDialog
import com.ichi2.anki.dialogs.InsertFieldDialogViewModel
import com.ichi2.anki.libanki.CardOrdinal
import com.ichi2.anki.libanki.NotetypeJson
import com.ichi2.anki.libanki.Notetypes
import com.ichi2.anki.libanki.testutils.ext.addNote
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.notetype.ManageNoteTypesState.CardEditor
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.previewer.TemplatePreviewerFragment
import com.ichi2.testutils.assertFalse
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.withSplitPaneUi
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import timber.log.Timber
import kotlin.test.junit5.JUnit5Asserter.assertEquals
import kotlin.test.junit5.JUnit5Asserter.assertNotEquals
import kotlin.test.junit5.JUnit5Asserter.assertNotNull
import kotlin.test.junit5.JUnit5Asserter.assertNull
import kotlin.test.junit5.JUnit5Asserter.assertTrue
import kotlin.time.Duration

@RunWith(AndroidJUnit4::class)
class CardTemplateEditorTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun CardTemplateEditor.selectTab(index: Int) = compose.selectTemplate(this, index)

    private val CardTemplateEditor.selectedTabPosition: Int get() = compose.selectedTemplate(this)

    private fun renderedDialogMessage(): String =
        compose
            .onAllNodes(hasAnyAncestor(isDialog()) and !hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
            .last()
            .config[SemanticsProperties.Text]
            .single()
            .text

    private fun clickDialogAction(label: String) {
        compose.onNode(hasText(label) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        advanceRobolectricLooper()
    }

    @Test
    fun `field insertion previews the original card after an unsaved deletion`() {
        val note = addBasicAndReversedNote()
        val reverseCardId = note.cards().single { it.ord == 1 }.id
        val intent = CardTemplateEditor.getIntent(targetContext, col.notetypes.basicAndReversed.id, note.id)
        val editor = startActivityNormallyOpenCollectionWithIntent(CardTemplateEditor::class.java, intent)

        with(editor) {
            deleteSelectedCardType()
            compose.clickTemplateEditorAction(EditorControl.INSERT_FIELD)
            advanceRobolectricLooper()
            val dialog = supportFragmentManager.fragments.filterIsInstance<InsertFieldDialog>().single()
            val metadata = ViewModelProvider(dialog)[InsertFieldDialogViewModel::class.java].metadata

            assertEquals("The preview should use the surviving card", reverseCardId, metadata.cardId)
        }
    }

    @Test
    fun `deletion confirmation counts the original card type after an unsaved deletion`() {
        val noteType = col.notetypes.basicAndReversed
        val spare =
            Notetypes.newTemplate("Spare").apply {
                qfmt = "{{Front}} spare"
                afmt = "{{Back}}"
            }
        col.notetypes.addTemplate(noteType, spare)
        addBasicAndReversedNote("one", "back")
        addBasicAndReversedNote("two", "")

        withCardTemplateEditor(noteType) {
            deleteSelectedCardType()
            selectTab(0)
            advanceRobolectricLooper()
            assertTrue("Unable to delete the card type", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()

            assertEquals(
                "The remaining reverse card type has one card in the database",
                getQuantityString(CommonPlurals.card_template_editor_confirm_delete, 1, 1, "Card 2"),
                renderedDialogMessage(),
            )
        }
    }

    @Test
    fun `change detection does not serialize the note type - Issue 21912`() {
        val original = col.notetypes.basic
        val edited =
            NotetypeJson(
                original.jsonObject.deepClonedInto(
                    object : JSONObject() {
                        override fun toString(): String = error("Change detection must not serialize the whole note type")
                    },
                ),
            )
        val intent = CardEditor(ntid = original.id).toIntent(targetContext)
        val controller = Robolectric.buildActivity(CardTemplateEditor::class.java, intent)
        saveControllerForCleanup(controller)
        controller.setup()
        // Install the guarded model before the editor fragment's initial Back callback update.
        val guardedEditor =
            CardTemplateEditorFragment().apply {
                arguments = intent.extras
                tempNoteType = CardTemplateNotetype(edited)
            }
        controller.get().supportFragmentManager.commitNow {
            replace(R.id.fragment_container, guardedEditor, SingleFragmentActivity.FRAGMENT_TAG)
        }

        with(controller.get()) {
            assertFalse("Opening the editor should not mark the note type as changed", noteTypeHasChanged())
            edited.templates[0].qfmt += "edited"
            assertTrue("A nested template edit should be detected", noteTypeHasChanged())
            edited.templates[0].qfmt = original.templates[0].qfmt
            assertFalse("Reverting the edit should clear the change", noteTypeHasChanged())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEditTemplateContents() {
        val noteTypeName = "Basic"

        // Start the CardTemplateEditor with a specific note type, and make sure the note type starts unchanged
        val collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
        var templateEditorController =
            Robolectric
                .buildActivity(CardTemplateEditor::class.java, intent)
                .create()
                .start()
                .resume()
                .visible()
        saveControllerForCleanup(templateEditorController)
        var testEditor = templateEditorController.get()
        assertFalse("Note type should not have changed yet", testEditor.noteTypeHasChanged())

        // Change the note type and make sure it registers as changed, but the database is unchanged
        var templateFront = testEditor.editText
        val testNoteTypeQfmtEdit = "!@#$%^&*TEST*&^%$#@!"
        templateFront.text.append(testNoteTypeQfmtEdit)
        advanceRobolectricLooper()
        assertTrue("Note type did not change after edit?", testEditor.noteTypeHasChanged())
        assertEquals(
            "Change already in database?",
            collectionBasicNoteTypeOriginal.toString().trim(),
            getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
        )

        // Kill and restart the Activity, make sure note type edit is preserved
        val outBundle = Bundle()
        templateEditorController.saveInstanceState(outBundle)
        templateEditorController.pause().stop().destroy()
        templateEditorController =
            Robolectric
                .buildActivity(CardTemplateEditor::class.java)
                .create(outBundle)
                .start()
                .resume()
                .visible()
        saveControllerForCleanup(templateEditorController)
        testEditor = templateEditorController.get()
        assertTrue("note type change not preserved across activity lifecycle?", testEditor.noteTypeHasChanged())
        assertEquals(
            "Change already in database?",
            collectionBasicNoteTypeOriginal.toString().trim(),
            getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
        )

        // Make sure we get a confirmation dialog if we hit the back button
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.BACK))
        advanceRobolectricLooper()
        assertEquals("Wrong dialog shown?", renderedDialogMessage(), "Discard changes?")
        clickDialogAction("Keep editing")
        advanceRobolectricLooper()
        assertTrue("note type change not preserved despite canceling back button?", testEditor.noteTypeHasChanged())

        // Make sure we things are cleared out after a cancel
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.BACK))
        assertEquals("Wrong dialog shown?", renderedDialogMessage(), "Discard changes?")
        clickDialogAction(getResourceString(CommonString.discard))
        advanceRobolectricLooper()
        assertFalse("note type change not cleared despite discarding changes?", testEditor.noteTypeHasChanged())

        // Get going for content edit assertions again...
        templateEditorController =
            Robolectric
                .buildActivity(CardTemplateEditor::class.java, intent)
                .create()
                .start()
                .resume()
                .visible()
        saveControllerForCleanup(templateEditorController)
        testEditor = templateEditorController.get()
        templateFront = testEditor.editText
        templateFront.text.append(testNoteTypeQfmtEdit)
        advanceRobolectricLooper()
        assertTrue("Note type did not change after edit?", testEditor.noteTypeHasChanged())

        // Make sure we pass the edit to the Previewer
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.PREVIEW))
        advanceRobolectricLooper()
        val startedIntent = shadowOf(testEditor).nextStartedActivity
        val shadowIntent = shadowOf(startedIntent)
        advanceRobolectricLooper()
        assertEquals("Previewer not started?", CardViewerActivity::class.java.name, shadowIntent.intentClass.name)
        assertEquals(
            "Change already in database?",
            collectionBasicNoteTypeOriginal.toString().trim(),
            getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
        )

        // Save the template then fetch it from the collection to see if it was saved correctly
        var testEditorNoteTypeEdited = testEditor.tempNoteType?.notetype
        advanceRobolectricLooper()
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.SAVE))
        advanceRobolectricLooper()
        val collectionBasicNoteTypeCopyEdited = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        testEditorNoteTypeEdited = col.notetypes.get(testEditorNoteTypeEdited!!.id)
        assertNotEquals("Note type is unchanged?", collectionBasicNoteTypeOriginal, collectionBasicNoteTypeCopyEdited)
        assertEquals(
            "note type did not save?",
            testEditorNoteTypeEdited.toString().trim(),
            collectionBasicNoteTypeCopyEdited.toString().trim(),
        )
        assertTrue("Note type does not have our change?", collectionBasicNoteTypeCopyEdited.toString().contains(testNoteTypeQfmtEdit))
    }

    @Test
    fun testDeleteTemplate() {
        val noteTypeName = "Basic (and reversed card)"

        // Start the CardTemplateEditor with a specific note type, and make sure the note type starts unchanged
        val collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
        val templateEditorController =
            Robolectric
                .buildActivity(CardTemplateEditor::class.java, intent)
                .create()
                .start()
                .resume()
                .visible()
        saveControllerForCleanup(templateEditorController)
        val testEditor = templateEditorController.get()
        assertFalse("Note type should not have changed yet", testEditor.noteTypeHasChanged())
        assertEquals("Note type should have 2 templates now", 2, testEditor.tempNoteType?.templateCount)

        // Try to delete the template - click delete, click confirm for card delete, click confirm again for full sync
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
        advanceRobolectricLooper()
        assertEquals("Wrong dialog shown?", "Delete the “Card 1” card type, and its 0 cards?", renderedDialogMessage())
        clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
        advanceRobolectricLooper()
        assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
        assertEquals("Note type should have 1 template now", 1, testEditor.tempNoteType?.templateCount)

        // With one remaining template the delete action is unavailable.
        compose.openTemplateActions(testEditor)
        compose.onNodeWithText(getResourceString(CommonString.delete_card_type)).assertDoesNotExist()
        pressBack()
        assertEquals(
            "Change already in database?",
            collectionBasicNoteTypeOriginal.toString().trim(),
            getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
        )

        // Save the change to the database and make sure there's only one template after
        var testEditorNoteTypeEdited = testEditor.tempNoteType?.notetype
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.SAVE))
        advanceRobolectricLooper()
        val collectionBasicNoteTypeCopyEdited = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        testEditorNoteTypeEdited = col.notetypes.get(testEditorNoteTypeEdited!!.id)
        assertNotEquals("Note type is unchanged?", collectionBasicNoteTypeOriginal, collectionBasicNoteTypeCopyEdited)
        assertEquals(
            "Note type did not save?",
            testEditorNoteTypeEdited.toString().trim(),
            collectionBasicNoteTypeCopyEdited.toString().trim(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testTemplateAdd() {
        // Make sure we test previewing a new card template - not working for real yet
        val noteTypeName = "Basic"
        val collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
        val templateEditorController =
            Robolectric
                .buildActivity(CardTemplateEditor::class.java, intent)
                .create()
                .start()
                .resume()
                .visible()
        saveControllerForCleanup(templateEditorController)
        val testEditor = templateEditorController.get()
        assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))

        // Try to add a template - click add, click confirm for card add, click confirm again for full sync
        addCardType(testEditor)
        // if AnkiDroid moves to match AnkiDesktop it will pop a dialog to confirm card create
        // Assert.assertEquals("Wrong dialog shown?", "This will create NN cards. Proceed?", getDialogText());
        // clickDialogButton(WhichButton.POSITIVE);
        assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
        assertNull("New templates have no database ordinal", testEditor.tempNoteType!!.getOriginalOrdinal(1))
        assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
        assertTrue("Ordinal not pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))
        assertEquals("Note type should have 2 templates now", 2, testEditor.tempNoteType!!.templateCount)

        // Make sure we pass the new template to the Previewer
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.PREVIEW))
        val startedIntent = shadowOf(testEditor).nextStartedActivity
        val shadowIntent = shadowOf(startedIntent)
        assertEquals("Previewer not started?", CardViewerActivity::class.java.name, shadowIntent.intentClass.name)
        assertEquals(
            "Change already in database?",
            collectionBasicNoteTypeOriginal.toString().trim(),
            getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
        )

        // Save the change to the database and make sure there are two templates after
        var testEditorNoteTypeEdited = col.notetypes.get(testEditor.tempNoteType!!.notetype.id)
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.SAVE))
        advanceRobolectricLooper()
        val collectionBasicNoteTypeCopyEdited = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        assertNotEquals("Note type is unchanged?", collectionBasicNoteTypeOriginal, collectionBasicNoteTypeCopyEdited)
        testEditorNoteTypeEdited = col.notetypes.get(testEditorNoteTypeEdited!!.id)

        assertEquals(
            "Note type did not save?",
            testEditorNoteTypeEdited.toString().trim(),
            collectionBasicNoteTypeCopyEdited.toString().trim(),
        )
    }

    /**
     * In a note type with two card templates using different fields, some notes may only use card 1,
     * and some may only use card 2. If you delete the 2nd template,
     * it will cause the notes that only use card 2 to disappear.
     *
     * So the unit test would then be to make a note type like the "basic (optional reverse card)"
     * with two fields Enable1 and Enable2, and two templates "card 1" and "card 2".
     * Both cards use selective generation, so they're empty unless the corresponding field is set.
     *
     * So then in the unit test you make the note type, add the two templates, then you add two notes,
     * with Enable1 and Enable2 respectively set to "y".
     * Then you try to delete one of the templates and it should fail
     *
     * (question: but I thought deleting one should work - still one card left to maintain the note,
     * and second template delete should fail since we finally get to a place where no cards are left?
     * I am having trouble creating selectively generated cards though - I can do one optional field but not 2 ugh)
     *
     */
    @Test
    fun testDeleteTemplateWithSelectivelyGeneratedCards() =
        runTest {
            val noteTypeName = "Basic (optional reversed card)"
            val collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)

            // Start the CardTemplateEditor with a specific note type, and make sure the note type starts unchanged
            val intent = Intent(Intent.ACTION_VIEW)
            intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
            val templateEditorController =
                Robolectric
                    .buildActivity(
                        CardTemplateEditor::class.java,
                        intent,
                    ).create()
                    .start()
                    .resume()
                    .visible()
            saveControllerForCleanup(templateEditorController)
            val testEditor = templateEditorController.get()
            assertFalse("Note type should not have changed yet", testEditor.noteTypeHasChanged())
            assertEquals("Note type should have 2 templates now", 2, testEditor.tempNoteType?.templateCount)
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))

            // Try to delete Card 1 template - click delete, check confirm for card delete popup indicating it was possible, then dismiss it
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals("Wrong dialog shown?", "Delete the “Card 1” card type, and its 0 cards?", renderedDialogMessage())
            clickDialogAction(getResourceString(CommonString.dialog_cancel))
            advanceRobolectricLooper()
            assertFalse("Note type should not have changed", testEditor.noteTypeHasChanged())

            // Create note with forward and back info, Add Reverse is empty, so should only be one card
            val selectiveGeneratedNote = col.newNote(collectionBasicNoteTypeOriginal)
            selectiveGeneratedNote.setField(0, "TestFront")
            selectiveGeneratedNote.setField(1, "TestBack")
            val fields = selectiveGeneratedNote.fields
            for (field in fields) {
                Timber.d("Got a field: %s", field)
            }
            col.addNote(selectiveGeneratedNote)
            assertEquals("selective generation should result in one card", 1, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))

            // Try to delete the template again, but there's selective generation means it would orphan the note
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals(
                "Did not show dialog about deleting only card?",
                getResourceString(CommonString.orphan_note_message),
                renderedDialogMessage(),
            )
            clickDialogAction(getResourceString(android.R.string.ok))
            advanceRobolectricLooper()
            assertNull(
                "Can delete used template?",
                collectionBasicNoteTypeOriginal.getCardIds(0),
            )
            assertEquals(
                "Change already in database?",
                collectionBasicNoteTypeOriginal.toString().trim(),
                getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
            )
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("A blocked deletion must not change the note type", testEditor.noteTypeHasChanged())

            // Assert can delete 'Card 2'
            assertNotNull("Cannot delete unused template?", collectionBasicNoteTypeOriginal.getCardIds(1))

            // Edit note to have Add Reverse set to 'y' so we get a second card
            selectiveGeneratedNote.setField(2, "y")
            selectiveGeneratedNote.flush()

            // - assert two cards
            assertEquals("should be two cards now", 2, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))

            // - assert can delete either Card template but not both
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(0))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(1))
            assertNull("Can delete both templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1))

            // A couple more notes to make sure things are okay
            val secondNote = col.newNote(collectionBasicNoteTypeOriginal)
            secondNote.setField(0, "TestFront2")
            secondNote.setField(1, "TestBack2")
            secondNote.setField(2, "y")
            col.addNote(secondNote)

            // - assert can delete either Card template but not both
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(0))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(1))
            assertNull("Can delete both templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1))
        }

    /**
     * Normal template deletion - with no selective generation should of course work
     */
    @Test
    fun testDeleteTemplateWithGeneratedCards() =
        runTest {
            val noteTypeName = "Basic (and reversed card)"
            var collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)

            // Start the CardTemplateEditor with a specific note type, and make sure the note type starts unchanged
            var intent = Intent(Intent.ACTION_VIEW)
            intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
            var templateEditorController =
                Robolectric
                    .buildActivity(
                        CardTemplateEditor::class.java,
                        intent,
                    ).create()
                    .start()
                    .resume()
                    .visible()
            saveControllerForCleanup(templateEditorController)
            var testEditor = templateEditorController.get()
            assertFalse("Note type should not have changed yet", testEditor.noteTypeHasChanged())
            assertEquals("Note type should have 2 templates now", 2, testEditor.tempNoteType?.templateCount)
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))

            // Create note with forward and back info
            val selectiveGeneratedNote = col.newNote(collectionBasicNoteTypeOriginal)
            selectiveGeneratedNote.setField(0, "TestFront")
            selectiveGeneratedNote.setField(1, "TestBack")
            col.addNote(selectiveGeneratedNote)
            assertEquals("card generation should result in two cards", 2, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))

            // Test if we can delete the template - should be possible - but cancel the delete
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals(
                "Did not show dialog about deleting template and it's card?",
                getQuantityString(CommonPlurals.card_template_editor_confirm_delete, 1, 1, "Card 1"),
                renderedDialogMessage(),
            )
            clickDialogAction(getResourceString(CommonString.dialog_cancel))
            advanceRobolectricLooper()
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(0))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(1))
            assertNull("Can delete both templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1))
            assertEquals(
                "Change in database despite no change?",
                collectionBasicNoteTypeOriginal.toString().trim(),
                getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
            )
            assertEquals("Note type should have 2 templates still", 2, testEditor.tempNoteType?.templateCount)

            // Add a template - click add, click confirm for card add, click confirm again for full sync
            addCardType(testEditor)
            // the templates must be different
            testEditor.tempNoteType!!.getTemplate(2).qfmt += "different_template"
            assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
            assertNull("New templates have no database ordinal", testEditor.tempNoteType!!.getOriginalOrdinal(2))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))
            assertTrue("Ordinal not pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(2))
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.SAVE))
            advanceRobolectricLooper()
            assertFalse("Note type should now be unchanged", testEditor.noteTypeHasChanged())
            assertEquals("card generation should result in three cards", 3, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))
            // reload the note type for future comparison after saving the edit
            collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)

            // Start the CardTemplateEditor back up after saving (which closes the thing...)
            intent = Intent(Intent.ACTION_VIEW)
            intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
            templateEditorController =
                Robolectric
                    .buildActivity(CardTemplateEditor::class.java, intent)
                    .create()
                    .start()
                    .resume()
                    .visible()
            saveControllerForCleanup(templateEditorController)
            testEditor = templateEditorController.get()
            assertFalse("Note type should not have changed yet", testEditor.noteTypeHasChanged())
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(2))
            assertEquals("Note type should have 3 templates now", 3, testEditor.tempNoteType?.templateCount)

            // Add another template - but we work in memory for a while before saving
            addCardType(testEditor)
            assertNull("New templates have no database ordinal", testEditor.tempNoteType!!.getOriginalOrdinal(3))
            assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
            assertEquals("Note type should have 4 templates now", 4, testEditor.tempNoteType?.templateCount)
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(2))
            assertTrue("Ordinal not pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(3))
            assertNull("New templates have no database ordinal", testEditor.tempNoteType!!.getOriginalOrdinal(3))

            // Delete two pre-existing templates for real now - but still without saving it out, should work fine
            advanceRobolectricLooper()
            testEditor.selectTab(0)
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals(
                "Did not show dialog about deleting template and it's card?",
                getQuantityString(CommonPlurals.card_template_editor_confirm_delete, 1, 1, "Card 1"),
                renderedDialogMessage(),
            )
            clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
            advanceRobolectricLooper()
            advanceRobolectricLooper()
            testEditor.selectTab(0)
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals(
                "Did not show dialog about deleting template and it's card?",
                getQuantityString(CommonPlurals.card_template_editor_confirm_delete, 1, 1, "Card 2"),
                renderedDialogMessage(),
            )
            clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
            advanceRobolectricLooper()

            // - assert can delete any 1 or 2 Card templates but not all
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(0))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(1))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(2))
            assertNotNull("Cannot delete two templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1))
            assertNotNull("Cannot delete two templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 2))
            assertNotNull("Cannot delete two templates?", collectionBasicNoteTypeOriginal.getCardIds(1, 2))
            assertNull("Can delete all templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1, 2))
            assertEquals(
                "Change already in database?",
                collectionBasicNoteTypeOriginal.toString().trim(),
                getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
            )
            assertNull("New templates have no database ordinal", testEditor.tempNoteType!!.getOriginalOrdinal(1))

            // Now confirm everything to persist it to the database
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.SAVE))
            advanceRobolectricLooper()
            advanceRobolectricLooper()
            assertNotEquals(
                "Change not in database?",
                collectionBasicNoteTypeOriginal.toString().trim(),
                getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
            )
            assertEquals("Note type should have 2 templates now", 2, getCurrentDatabaseNoteTypeCopy(noteTypeName).templates.length())
            assertEquals("should be two cards", 2, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))
        }

    /**
     * Deleting a template you just added - but in the same ordinal as a previous pending delete - should get it's card count correct
     */
    @Test
    fun testDeletePendingAddExistingCardCount() =
        runTest {
            val noteTypeName = "Basic (optional reversed card)"
            val collectionBasicNoteTypeOriginal = getCurrentDatabaseNoteTypeCopy(noteTypeName)

            // Start the CardTemplateEditor with a specific note type, and make sure the note type starts unchanged
            val intent = Intent(Intent.ACTION_VIEW)
            intent.putExtra("noteTypeId", collectionBasicNoteTypeOriginal.id)
            val templateEditorController =
                Robolectric
                    .buildActivity(
                        CardTemplateEditor::class.java,
                        intent,
                    ).create()
                    .start()
                    .resume()
                    .visible()
            saveControllerForCleanup(templateEditorController)
            val testEditor = templateEditorController.get()
            assertFalse("Note type should not have changed yet", testEditor.noteTypeHasChanged())
            assertEquals("Note type should have 2 templates now", 2, testEditor.tempNoteType?.templateCount)
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))

            // Create note with forward and back info
            val selectiveGeneratedNote = col.newNote(collectionBasicNoteTypeOriginal)
            selectiveGeneratedNote.setField(0, "TestFront")
            selectiveGeneratedNote.setField(1, "TestBack")
            selectiveGeneratedNote.setField(2, "y")
            col.addNote(selectiveGeneratedNote)
            assertEquals("card generation should result in two cards", 2, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))

            // Delete ord 1 / 'Card 2' and check the message
            testEditor.selectTab(1)
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals(
                "Did not show dialog about deleting template and it's card?",
                getQuantityString(CommonPlurals.card_template_editor_confirm_delete, 1, 1, "Card 2"),
                renderedDialogMessage(),
            )
            clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
            advanceRobolectricLooper()
            assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(0))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(1))
            assertNull("Can delete both templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1))
            assertEquals(
                "Change in database despite no save?",
                collectionBasicNoteTypeOriginal.toString().trim(),
                getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
            )
            assertEquals("Note type should have 1 template", 1, testEditor.tempNoteType?.templateCount)

            // Add a template - click add, click confirm for card add, click confirm again for full sync
            addCardType(testEditor)
            assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
            assertNull("New templates have no database ordinal", testEditor.tempNoteType!!.getOriginalOrdinal(1))
            assertFalse("Ordinal pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(0))
            assertTrue("Ordinal not pending add?", testEditor.tempNoteType.isOrdinalPendingAdd(1))
            assertEquals("Note type should have 2 templates", 2, testEditor.tempNoteType?.templateCount)

            // Delete ord 1 / 'Card 2' again and check the message - it's in the same spot as the pre-existing template but there are no cards actually associated
            testEditor.selectTab(1)
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            assertEquals(
                "Did not show dialog about deleting template and it's card?",
                getQuantityString(CommonPlurals.card_template_editor_confirm_delete, 0, 0, CollectionManager.TR.cardTemplatesCard(2)),
                renderedDialogMessage(),
            )
            clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
            advanceRobolectricLooper()
            assertTrue("Note type should have changed", testEditor.noteTypeHasChanged())
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(0))
            assertNotNull("Cannot delete template?", collectionBasicNoteTypeOriginal.getCardIds(1))
            assertNull("Can delete both templates?", collectionBasicNoteTypeOriginal.getCardIds(0, 1))
            assertEquals(
                "Change in database despite no save?",
                collectionBasicNoteTypeOriginal.toString().trim(),
                getCurrentDatabaseNoteTypeCopy(noteTypeName).toString().trim(),
            )
            assertEquals("Note type should have 1 template", 1, testEditor.tempNoteType?.templateCount)

            // Save it out and make some assertions
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.SAVE))
            advanceRobolectricLooper()
            assertFalse("Note type should now be unchanged", testEditor.noteTypeHasChanged())
            assertEquals("card generation should result in 1 card", 1, getNoteTypeCardCount(collectionBasicNoteTypeOriginal))
        }

    @Test
    fun testDeckOverride() {
        val noteTypeName = "Basic (optional reversed card)"
        val noteType = getCurrentDatabaseNoteTypeCopy(noteTypeName)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra("noteTypeId", noteType.id)
        val editor = super.startActivityNormallyOpenCollectionWithIntent(CardTemplateEditor::class.java, intent)
        val template = editor.tempNoteType?.getTemplate(0)
        MatcherAssert.assertThat("Deck ID element should exist", template?.jsonObject?.has("did"), Matchers.equalTo(true))
        MatcherAssert.assertThat("Deck ID element should be null", template?.jsonObject?.get("did"), Matchers.equalTo(JSONObject.NULL))
        editor.onDeckSelected(SelectableDeck.Deck(1, "hello"))
        MatcherAssert.assertThat("Deck ID element should be changed", template?.jsonObject?.get("did"), Matchers.equalTo(1L))
        editor.onDeckSelected(null)
        MatcherAssert.assertThat("Deck ID element should exist", template!!.jsonObject.has("did"), Matchers.equalTo(true))
        MatcherAssert.assertThat("Deck ID element should be null", template.jsonObject["did"], Matchers.equalTo(JSONObject.NULL))
    }

    @Test
    fun `editing with the keyboard open leaves Back to the IME - Issue 21807`() {
        withCardTemplateEditor {
            dispatchInsets(imeBottom = 240.dp)
            editText.text.append("edited")

            assertTrue("The template has unsaved changes", noteTypeHasChanged())
            assertFalse("Back should dismiss the keyboard", onBackPressedDispatcher.hasEnabledCallbacks())

            dispatchInsets()
            onBackPressedDispatcher.onBackPressed()
            assertEquals("Back should now confirm discarding edits", "Discard changes?", renderedDialogMessage())
            assertFalse("Edits must not be discarded without confirmation", isFinishing)
        }
    }

    @Test
    fun `opening the keyboard after editing leaves Back to the IME - Issue 21807`() {
        withCardTemplateEditor {
            editText.text.append("edited")
            assertTrue("Back should confirm discarding edits", onBackPressedDispatcher.hasEnabledCallbacks())

            dispatchInsets(imeBottom = 240.dp)

            assertFalse("Back should dismiss the keyboard", onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    /**
     * Guards the fix for 21807: a physical keyboard reports a visible IME, but does not hide the
     * tabs and Back opens the discard confirmation.
     *
     */
    @Test
    fun `back confirms discarding edits with a physical keyboard`() {
        fun CardTemplateEditor.assertBackConfirmsDiscardingEdits() {
            editText.text.append("edited")

            assertTrue("Back should confirm discarding edits", onBackPressedDispatcher.hasEnabledCallbacks())
            onBackPressedDispatcher.onBackPressed()

            assertEquals("Back should confirm discarding edits", "Discard changes?", renderedDialogMessage())
            assertFalse("Edits must not be discarded without confirmation", isFinishing)
            // dismiss: the next case would otherwise assert against this dialog if it showed none
            clickDialogAction("Keep editing")
        }

        // 3-button navigation reports a visible IME with no height
        withCardTemplateEditor {
            dispatchInsets(navBarRight = 48.dp, imeVisible = true)
            assertBackConfirmsDiscardingEdits()
        }

        // gesture navigation shows only a 48dp strip
        withCardTemplateEditor {
            dispatchInsets(navBarBottom = 24.dp, imeBottom = 48.dp)
            assertBackConfirmsDiscardingEdits()
        }
    }

    /**
     * Back follows the keyboard when the selected Card Template changes.
     */
    @Test
    fun `Back follows the keyboard after selecting another card - Issue 21807`() {
        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            editText.text.append("edited")
            selectTab(1)
            advanceRobolectricLooper()

            dispatchInsets(imeBottom = 240.dp)
            assertFalse("Back should dismiss the keyboard", onBackPressedDispatcher.hasEnabledCallbacks())

            dispatchInsets()
            assertTrue("Back should confirm discarding edits", onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    @Test
    fun `Back follows the keyboard after deleting a card type - Issue 21807`() {
        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            assertTrue("Unable to delete the card type", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
            advanceRobolectricLooper()
            assertEquals("One card type should remain", 1, tempNoteType!!.templateCount)
            assertTrue("The deletion is unsaved", noteTypeHasChanged())

            dispatchInsets(imeBottom = 240.dp)
            compose.onNodeWithText(templateSectionLabel(this, EditorViewType.FRONT)).assertIsDisplayed()
            assertFalse("Back should dismiss the keyboard", onBackPressedDispatcher.hasEnabledCallbacks())

            dispatchInsets()
            onBackPressedDispatcher.onBackPressed()
            assertEquals("Back should now confirm discarding the deletion", "Discard changes?", renderedDialogMessage())
            assertFalse("The deletion must not be discarded without confirmation", isFinishing)
        }
    }

    @Test
    fun `Ctrl+2 switches to the back template after deleting a card type`() {
        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            assertTrue("Unable to delete the card type", compose.clickTemplateEditorAction(EditorControl.DELETE))
            advanceRobolectricLooper()
            clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
            advanceRobolectricLooper()
            assertEquals("One card type should remain", 1, tempNoteType!!.templateCount)

            val template = tempNoteType!!.getTemplate(0)
            assertEquals("The front template should be selected initially", template.qfmt, editText.text.toString())

            dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_2, 0, KeyEvent.META_CTRL_ON))

            assertEquals("Ctrl+2 should show the back template", template.afmt, editText.text.toString())
        }
    }

    @Test
    fun `editing with the keyboard open leaves Back to the IME - tablet ui - Issue 21807`() =
        withSplitPaneUi {
            withCardTemplateEditor(col.notetypes.basicAndReversed) {
                editText.text.append("edited")
                assertTrue("Back should confirm discarding edits", onBackPressedDispatcher.hasEnabledCallbacks())

                dispatchInsets(imeBottom = 240.dp)

                assertFalse("Back should dismiss the keyboard", onBackPressedDispatcher.hasEnabledCallbacks())
            }
        }

    @Test
    fun `toolbar navigation still confirms discarding edits with the keyboard open`() {
        withCardTemplateEditor {
            dispatchInsets(imeBottom = 240.dp)
            editText.text.append("edited")

            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.BACK))

            assertEquals("The toolbar should confirm discarding edits", "Discard changes?", renderedDialogMessage())
            assertFalse("Edits must not be discarded without confirmation", isFinishing)
        }
    }

    @Test
    fun `closing the keyboard after reverting edits allows Back to exit`() {
        withCardTemplateEditor {
            val original = editText.text.toString()
            dispatchInsets(imeBottom = 240.dp)
            editText.text.append("edited")
            editText.setText(original)

            dispatchInsets()
            assertFalse("No discard confirmation is needed", onBackPressedDispatcher.hasEnabledCallbacks())
            onBackPressedDispatcher.onBackPressed()

            assertTrue("Back should close the unchanged editor", isFinishing)
        }
    }

    @Test
    fun `ensure 'Discard changes' dialog is enabled after note type changes - Issue 18518`() {
        fun assertDiscardChangesDialogShown() {
            advanceRobolectricLooper()
            assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.BACK))
            advanceRobolectricLooper()
            assertEquals("Wrong dialog shown?", "Discard changes?", renderedDialogMessage())
            clickDialogAction(getResourceString(CommonString.discard))
        }

        // Case 1: Show dialog after deck override
        withCardTemplateEditor {
            onDeckSelected(SelectableDeck.Deck(1, "hello"))
            assertDiscardChangesDialogShown()
        }

        // Case 2: Show dialog after changing card browser appearance
        withCardTemplateEditor {
            val shadowEditor = shadowOf(this)
            assertTrue(
                "Unable to click?",
                compose.clickTemplateEditorAction(EditorControl.BROWSER_APPEARANCE),
            )
            advanceRobolectricLooper()
            shadowEditor.receiveResult(
                shadowEditor.nextStartedActivity,
                Activity.RESULT_OK,
                Intent()
                    .putExtra(CardTemplateBrowserAppearanceEditor.EXTRA_QUESTION_FORMAT, "q")
                    .putExtra(CardTemplateBrowserAppearanceEditor.EXTRA_ANSWER_FORMAT, "a"),
            )
            assertDiscardChangesDialogShown()
        }

        // Case 3: Show dialog after adding a card type
        withCardTemplateEditor {
            addCardType(this)
            assertDiscardChangesDialogShown()
        }
    }

    @Test
    fun `edits survive switching template sections`() =
        withCardTemplateEditor {
            val front = editText.text.toString() + " edited"
            editText.setText(front)
            compose.selectSection(this, EditorViewType.STYLING)
            assertEquals("Styling should be displayed", tempNoteType!!.css, editText.text.toString())
            compose.selectSection(this, EditorViewType.FRONT)
            assertEquals("The front edit is preserved", front, editText.text.toString())
        }

    @Test
    fun `save remains available after rejected template`() =
        withCardTemplateEditor {
            editText.setText("{{UnknownField}}")
            throwOnShowError = false
            saveChanges()
            compose.onNodeWithContentDescription(getString(CommonString.save)).assertIsEnabled()
        }

    @Test
    fun `field insertion changes only the selected Card Template`() =
        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            val first = editText.text.toString()
            selectTab(1)
            val second = editText.text.toString()
            selectTab(0)
            editText.setSelection(0)

            compose.clickTemplateEditorAction(EditorControl.INSERT_FIELD)
            compose.onAllNodes(hasText("Back") and hasClickAction()).onLast().performClick()
            advanceRobolectricLooper()

            assertEquals("The field is inserted at the caret", "{{Back}}$first", editText.text.toString())
            selectTab(1)
            assertEquals("The other template is unchanged", second, editText.text.toString())
        }

    @Test
    fun `tab changes succeed with tablet UI - Issue 19589`() =
        withSplitPaneUi {
            withCardTemplateEditor(col.notetypes.basicAndReversed) {
                selectTab(1)
                selectTab(0)

                assertThat(selectedTabPosition, equalTo(0))
            }
        }

    @Test
    fun `correct ord is previewed after tab change - tablet ui - issue 20097`() =
        withSplitPaneUi {
            withCardTemplateEditor(col.notetypes.basicAndReversed) {
                selectTab(1)

                assertThat(previewer.ord, equalTo(1))
            }
        }

    @Test
    fun `the current card is focused`() {
        withCardTemplateEditor(noteType = getCurrentDatabaseNoteTypeCopy("Basic (and reversed card)")) {
            advanceRobolectricLooper()
            assertThat("card 1 is focused on open", window.decorView.findFocus(), equalTo(editText))

            selectTab(1)
            advanceRobolectricLooper()
            assertThat("card 2 is focused once selected", window.decorView.findFocus(), equalTo(editText))

            selectTab(0)
            advanceRobolectricLooper()
            assertThat("card 1 is focused again", window.decorView.findFocus(), equalTo(editText))
        }
    }

    @Test
    fun `adding a second card type still counts the notes`() {
        addBasicNote()
        val intent = Intent(Intent.ACTION_VIEW).putExtra("noteTypeId", col.notetypes.byName("Basic")!!.id)
        val controller =
            Robolectric
                .buildActivity(CardTemplateEditor::class.java, intent)
                .create()
                .start()
                .resume()
                .visible()
        saveControllerForCleanup(controller)
        val editor = controller.get()

        addCardType(editor)
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.ADD))
        advanceRobolectricLooper()
        assertEquals(
            "Adding from a new card type should count the notes",
            getQuantityString(CommonPlurals.card_template_editor_confirm_add, 1, 1),
            renderedDialogMessage(),
        )
    }

    @Test
    fun `adding an optional reverse template with an empty condition creates no cards`() {
        val noteType = col.notetypes.byName("Basic (optional reversed card)")!!
        val note = col.newNote(noteType)
        note.setField(0, "Front")
        note.setField(1, "Back")
        note.setField(2, "")
        col.addNote(note)
        assertEquals("Only the forward card should exist", 1, getNoteTypeCardCount(noteType))

        withCardTemplateEditor(noteType) {
            selectTab(1)
            advanceRobolectricLooper()
            assertEquals("The optional reverse template should be selected", 1, selectedTabPosition)

            assertTrue("Unable to add a card type", compose.clickTemplateEditorAction(EditorControl.ADD))
            advanceRobolectricLooper()
            val confirmation = renderedDialogMessage()
            clickDialogAction(getResourceString(CommonString.menu_add))
            assertEquals("The duplicate template should be added", 3, tempNoteType!!.templateCount)

            // Saving rejects identical fronts. Keep the empty condition while making the new front distinct.
            advanceRobolectricLooper()
            editText.setText("{{#Add Reverse}}{{Front}} {{Back}}{{/Add Reverse}}")

            assertTrue("Unable to save the templates", compose.clickTemplateEditorAction(EditorControl.SAVE))
            advanceRobolectricLooper()
            val savedNoteType = col.notetypes.get(noteType.id)!!
            assertEquals("The duplicate template should be saved", 3, savedNoteType.templates.length())
            assertEquals("An empty Add Reverse field should prevent generating another card", 1, getNoteTypeCardCount(noteType))
            assertEquals(
                "The confirmation must not promise a card that will not be generated",
                getQuantityString(CommonPlurals.card_template_editor_confirm_add, 0, 0),
                confirmation,
            )
        }
    }

    @Test
    fun `adding from an unsaved optional reverse template creates no cards`() {
        val noteType = col.notetypes.byName("Basic (optional reversed card)")!!
        val note = col.newNote(noteType)
        note.setField(0, "Front")
        note.setField(1, "Back")
        note.setField(2, "")
        col.addNote(note)
        assertEquals("Only the forward card should exist", 1, getNoteTypeCardCount(noteType))

        withCardTemplateEditor(noteType) {
            selectTab(1)
            advanceRobolectricLooper()
            addCardType(this)
            advanceRobolectricLooper()
            assertEquals("The first duplicate should be selected", 2, selectedTabPosition)
            assertTrue(
                "The selected template must be unsaved",
                CardTemplateNotetype.isOrdinalPendingAdd(tempNoteType!!, selectedTabPosition),
            )

            // Keep the empty condition, but make each front distinct so saving succeeds.
            editText.setText("{{#Add Reverse}}{{Front}} {{Back}}{{/Add Reverse}}")
            assertTrue("Unable to add a card type", compose.clickTemplateEditorAction(EditorControl.ADD))
            advanceRobolectricLooper()
            val confirmation = renderedDialogMessage()
            clickDialogAction(getResourceString(CommonString.menu_add))
            advanceRobolectricLooper()
            assertEquals("The second duplicate should be selected", 3, selectedTabPosition)
            editText.setText("{{#Add Reverse}}{{Back}} {{Front}}{{/Add Reverse}}")

            assertTrue("Unable to save the templates", compose.clickTemplateEditorAction(EditorControl.SAVE))
            advanceRobolectricLooper()
            assertEquals(
                "Both new templates should be saved",
                4,
                col.notetypes
                    .get(noteType.id)!!
                    .templates
                    .length(),
            )
            assertEquals("Neither conditional template should generate cards", 1, getNoteTypeCardCount(noteType))
            assertEquals(
                "Adding from an unsaved conditional template must not promise a card that will not be generated",
                getQuantityString(CommonPlurals.card_template_editor_confirm_add, 0, 0),
                confirmation,
            )
        }
    }

    @Test
    fun `adding from a broken template does not crash`() {
        addBasicAndReversedNote()
        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            editText.setText("{{#Front}}")
            assertTrue("Unable to add a card type", compose.clickTemplateEditorAction(EditorControl.ADD))
            advanceRobolectricLooper()
            assertEquals(
                "A broken template should not create cards",
                getQuantityString(CommonPlurals.card_template_editor_confirm_add, 0, 0),
                renderedDialogMessage(),
            )
        }
    }

    @Test
    fun `adding a template with a card number condition counts generated cards`() {
        addBasicAndReversedNote()
        val noteType = col.notetypes.basicAndReversed
        val initialCardCount = getNoteTypeCardCount(noteType)
        assertEquals("Both original cards should exist", 2, initialCardCount)

        withCardTemplateEditor(noteType) {
            // Preview supplies c3 for the new template, but card generation does not.
            editText.setText("{{#c3}}{{Front}}{{/c3}}")
            assertTrue("Unable to add a card type", compose.clickTemplateEditorAction(EditorControl.ADD))
            advanceRobolectricLooper()
            val confirmation = renderedDialogMessage()
            clickDialogAction(getResourceString(CommonString.menu_add))
            advanceRobolectricLooper()
            assertEquals("The new template should be selected", 2, selectedTabPosition)

            // Avoid identical fronts without changing the condition or field requirements.
            editText.setText("{{#c3}}{{Front}} extra{{/c3}}")
            assertTrue("Unable to save the templates", compose.clickTemplateEditorAction(EditorControl.SAVE))
            advanceRobolectricLooper()
            assertEquals(
                "The new template should be saved",
                3,
                col.notetypes
                    .get(noteType.id)!!
                    .templates
                    .length(),
            )
            val createdCards = getNoteTypeCardCount(noteType) - initialCardCount
            assertEquals("The card number condition should prevent generation", 0, createdCards)
            assertEquals(
                "The confirmation must count generated cards, not nonempty previews",
                getQuantityString(CommonPlurals.card_template_editor_confirm_add, createdCards, createdCards),
                confirmation,
            )
        }
    }

    @Test
    fun `counting falls back to the note count when it times out`() {
        val noteType = col.notetypes.byName("Basic (optional reversed card)")!!
        val note = col.newNote(noteType)
        note.setField(0, "Front")
        note.setField(1, "Back")
        col.addNote(note)
        val reverseTemplate = noteType.templates[1]

        assertEquals("No reverse card is created", 0, col.countCardsCreatedBy(noteType, reverseTemplate))
        assertEquals("A timeout counts every note", 1, col.countCardsCreatedBy(noteType, reverseTemplate, timeout = Duration.ZERO))
    }

    @Test
    fun `a failed save does not add card types - Issue 19953`() {
        addBasicNote()
        val noteType = col.notetypes.basic
        val savedNames = noteType.templatesNames

        withCardTemplateEditor(noteType) {
            repeat(2) {
                assertTrue("Unable to add a card type", compose.clickTemplateEditorAction(EditorControl.ADD))
                advanceRobolectricLooper()
                clickDialogAction(getResourceString(CommonString.menu_add))
                advanceRobolectricLooper()
            }
            assertEquals("The new card types should share a front", tempNoteType!!.getTemplate(1).qfmt, tempNoteType!!.getTemplate(2).qfmt)

            throwOnShowError = false
            saveChanges()
            assertEquals("A failed save must not add card types", savedNames, col.notetypes.get(noteType.id)!!.templatesNames)

            editText.setText("{{Back}} {{Front}}")
            val editedNames = tempNoteType!!.notetype.templatesNames
            saveChanges()
            assertEquals("Saving again should add each card type once", editedNames, col.notetypes.get(noteType.id)!!.templatesNames)
        }
    }

    @Test
    fun `deleting the first card type keeps the cards of the second`() {
        val note = addBasicAndReversedNote()
        val reverseCardId = note.cards().single { it.ord == 1 }.id

        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            deleteSelectedCardType()
            saveChanges()
        }

        assertEquals("Only the deleted card type's card should be removed", listOf(reverseCardId), note.cids())
    }

    @Test
    fun `deleting the first card type keeps the cards of the second after recreation`() {
        val note = addBasicAndReversedNote()
        val reverseCardId = note.cards().single { it.ord == 1 }.id
        val intent = CardEditor(ntid = col.notetypes.basicAndReversed.id).toIntent(targetContext)
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(CardTemplateEditor::class.java, intent)

        controller.get().deleteSelectedCardType()
        controller.recreate()
        advanceRobolectricLooper()
        controller.get().saveChanges()

        assertEquals("Only the deleted card type's card should be removed", listOf(reverseCardId), note.cids())
    }

    private fun CardTemplateEditor.deleteSelectedCardType() {
        assertTrue("Unable to delete the card type", compose.clickTemplateEditorAction(EditorControl.DELETE))
        advanceRobolectricLooper()
        clickDialogAction(getResourceString(CommonString.dialog_positive_delete))
        advanceRobolectricLooper()
    }

    private fun CardTemplateEditor.saveChanges() {
        assertTrue("Unable to save", compose.clickTemplateEditorAction(EditorControl.SAVE))
        advanceRobolectricLooper()
    }

    private fun addCardType(testEditor: CardTemplateEditor) {
        assertTrue("Unable to click?", compose.clickTemplateEditorAction(EditorControl.ADD))
        advanceRobolectricLooper()
        val ordinal = testEditor.selectedTabPosition
        val numAffectedCards =
            if (!testEditor.tempNoteType.isOrdinalPendingAdd(ordinal)) {
                col.notetypes.tmplUseCount(testEditor.tempNoteType!!.notetype, ordinal)
            } else {
                0
            }
        assertEquals(
            "Did not show dialog about adding template and it's card?",
            getQuantityString(CommonPlurals.card_template_editor_confirm_add, numAffectedCards, numAffectedCards),
            renderedDialogMessage(),
        )
        clickDialogAction(getResourceString(CommonString.menu_add))
    }

    @Test
    fun `template to markdown`() {
        val template =
            CardTemplate(
                front = "Hello World{{Front}}\n{{Extra}}",
                back = "{{FrontSide}}\n",
                style = ".card { }",
            )

        assertEquals(
            "markdown formatted template",
"""
**Front template**

```html
Hello World{{Front}}
{{Extra}}
```

**Back template**

```html
{{FrontSide}}

```

**Styling**

```css
.card { }
```
""".trim(),
            template.toMarkdown(targetContext),
        )
    }

    private fun getNoteTypeCardCount(notetype: NotetypeJson): Int {
        var cardCount = 0
        for (noteId in col.notetypes.nids(notetype)) {
            cardCount += col.getNote(noteId).numberOfCards()
        }
        return cardCount
    }

    private fun CardTemplateNotetype?.isOrdinalPendingAdd(ordinal: Int): Boolean {
        requireNotNull(this)
        return CardTemplateNotetype.isOrdinalPendingAdd(this, ordinal)
    }

    private suspend fun NotetypeJson.getCardIds(vararg ords: Int): List<Long>? = withCol { notetypes.getCardIdsForNoteType(id, ords) }
}

internal val CardTemplateEditor.editText: EditText
    get() = this.findViewById(R.id.edit_text)

fun RobolectricTest.withCardTemplateEditor(
    noteType: NotetypeJson = col.notetypes.basic,
    block: CardTemplateEditor.() -> Unit,
) {
    val intent = CardEditor(ntid = noteType.id).toIntent(targetContext)
    val activity = startActivityNormallyOpenCollectionWithIntent(CardTemplateEditor::class.java, intent)
    block(activity)
}

val CardTemplateEditor.previewer: TemplatePreviewerFragment
    get() = this.templateScreen!!.childFragmentManager.findFragmentById(R.id.fragment_container) as TemplatePreviewerFragment

val TemplatePreviewerFragment.ord: CardOrdinal
    get() = this.viewModel.ordFlow.value
