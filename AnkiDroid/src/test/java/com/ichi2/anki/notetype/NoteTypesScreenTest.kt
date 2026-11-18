// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardTemplateEditor
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.servicelayer.LanguageHintService.languageHint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NoteTypesScreenTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `opening a note type shows its fields and card templates on the same page`() {
        val activity = openType("Basic (and reversed card)")

        compose.onNodeWithText("Fields").assertIsDisplayed()
        compose.onNodeWithText("Card Templates").assertIsDisplayed()
        compose.onNodeWithText("Card 2").performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(CardTemplateEditor::class.java.name, intent.component?.className)
        assertEquals(1, intent.getIntExtra("ordId", -1))
    }

    @Test
    fun `stock types can be added and custom types can be cloned`() {
        startTypes()
        compose.openAddType()
        compose.fillName("Study prompts")
        compose.confirmAdd()
        compose.onNodeWithText("Study prompts").assertIsDisplayed()

        compose.openAddType()
        compose.onNodeWithText("Study prompts").performClick()
        compose.fillName("Practice prompts")
        compose.confirmAdd()

        assertEquals(listOf("Front", "Back"), col.notetypes.byName("Practice prompts")?.fieldsNames)
    }

    @Test
    fun `the list can be filtered and a note type renamed from its own page`() {
        startTypes()
        compose.onNodeWithContentDescription(targetContext.getString(androidx.appcompat.R.string.abc_search_hint)).performClick()
        compose.fillName("basic")
        compose.onNodeWithText("Cloze").assertDoesNotExist()
        compose.onNodeWithText("Basic").performClick()
        compose
            .onNodeWithContentDescription(
                targetContext.getString(androidx.appcompat.R.string.abc_action_menu_overflow_description),
            ).performClick()
        compose.onNodeWithText("Rename").performClick()

        compose.fillName(" ")
        compose.onNodeWithText("Rename").assertIsNotEnabled()
        compose.fillName("Cloze")
        compose.onNodeWithText("Rename").assertIsNotEnabled()
        compose.fillName("Basic vocabulary")
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("Basic vocabulary").assertIsDisplayed()
        assertNull(col.notetypes.byName("Basic"))
    }

    @Test
    fun `bulk deletion uses the same full sync gate as one type`() {
        startTypes()
        markSynced()
        compose.onNodeWithText("Cloze").performTouchInput { longClick() }
        compose.onNodeWithText("Basic (type in the answer)").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("This change needs a one-way sync").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Cloze").assertDoesNotExist()
        assertNull(col.notetypes.byName("Cloze"))
        assertNull(col.notetypes.byName("Basic (type in the answer)"))
        assertTrue(col.schemaChanged())
    }

    @Test
    fun `adding a field warns before marking the schema and keeps its submitted name`() {
        openType()
        markSynced()
        compose.onNodeWithText("Add field").performClick()
        compose.fillName("# {Usage}")
        compose.confirmAdd()
        compose.onNodeWithText("This change needs a one-way sync").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()

        assertFalse(col.schemaChanged())
        assertEquals(listOf("Front", "Back"), type().fieldsNames)

        compose.onNodeWithText("Add field").performClick()
        compose.fillName("# {Usage}")
        compose.confirmAdd()
        compose.onNodeWithText("Continue").performClick()
        assertEquals(listOf("Front", "Back", "Usage"), type().fieldsNames)
    }

    @Test
    fun `the full sync warning restores the chosen field across recreation and reorder`() {
        ensureCollectionLoadIsSynchronous()
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(ManageNotetypes::class.java, Intent())
        compose.onNodeWithText("Basic").performClick()
        markSynced()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Delete field").performClick()
        compose.onNodeWithText("This change needs a one-way sync").assertIsDisplayed()

        val original = type().deepClone()
        col.notetypes.moveFieldLegacy(original, original.getField(1), 0)
        markSynced()
        val night =
            Configuration(controller.get().resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
        controller.configurationChange(night)

        compose.onNodeWithText("This change needs a one-way sync").assertIsDisplayed()
        compose.onNodeWithText("Delete field").performClick()
        assertEquals(listOf("Front"), type().fieldsNames)
    }

    @Test
    fun `fields move through the handle and the accessible position picker`() {
        openType()
        col.modSchema(check = false)
        compose.onNodeWithText("Front").performTouchInput {
            swipe(start = Offset(24f, center.y), end = Offset(24f, center.y + height * 0.8f), durationMillis = 300)
        }
        assertEquals(listOf("Back", "Front"), type().fieldsNames)

        compose.onNodeWithText("Front").performClick()
        compose.onNodeWithText("Move to position…").performClick()
        compose.onNodeWithText("1").performClick()
        assertEquals(listOf("Front", "Back"), type().fieldsNames)
    }

    @Test
    fun `rename sort keyboard language and deletion apply to the chosen field`() {
        openType()
        col.modSchema(check = false)
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Rename field").performClick()
        compose.fillName("Meaning")
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("Meaning").performClick()
        compose.onNodeWithText("Sort the browser by this field").performClick()
        assertEquals("Meaning", type().getField(type().sortf).name)

        compose.onNodeWithText("Meaning").performClick()
        compose.onNodeWithText("Keyboard language", substring = false).performClick()
        compose.fillName("ja-JP")
        compose.onNodeWithText("Japanese (Japan)").performClick()
        assertEquals("ja-JP", type().getField(type().sortf).languageHint?.toLanguageTag())

        compose.onNodeWithText("Meaning").performClick()
        compose.onNodeWithText("Delete field").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(listOf("Front"), type().fieldsNames)
    }

    private fun openType(name: String = "Basic"): ManageNotetypes = startTypes().also { compose.onNodeWithText(name).performClick() }

    private fun startTypes(): ManageNotetypes {
        ensureCollectionLoadIsSynchronous()
        return startRegularActivity<ManageNotetypes>()
    }

    private fun markSynced() {
        col.db.execute("update col set ls = scm")
    }

    private fun type() = col.notetypes.byName("Basic")!!
}

internal fun androidx.compose.ui.test.junit4.ComposeTestRule.openAddType() = onNodeWithContentDescription("Add Note Type").performClick()

internal fun androidx.compose.ui.test.junit4.ComposeTestRule.fillName(input: String) =
    onNode(hasSetTextAction()).performTextReplacement(input)

internal fun androidx.compose.ui.test.junit4.ComposeTestRule.confirmAdd() = onNodeWithText("Add", substring = false).performClick()
