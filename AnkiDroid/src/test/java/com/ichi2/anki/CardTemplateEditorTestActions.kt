// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.ui.internationalization.sentenceCase
import org.robolectric.shadows.ShadowLooper
import androidx.appcompat.R as AppCompatR

enum class EditorControl { BACK, SAVE, PREVIEW, ADD, REPOSITION, RENAME, DELETE, BROWSER_APPEARANCE, INSERT_FIELD }

fun ComposeTestRule.selectSection(
    context: Context,
    section: EditorViewType,
) {
    onAllNodesWithText(templateSectionLabel(context, section)).onLast().performClick()
    waitForIdle()
}

fun ComposeTestRule.selectTemplate(
    editor: CardTemplateEditor,
    index: Int,
) {
    onAllNodesWithText(editor.tempNoteType!!.notetype.templatesNames[index]).onLast().performClick()
    waitForIdle()
    ShadowLooper.idleMainLooper()
}

fun ComposeTestRule.selectedTemplate(editor: CardTemplateEditor): Int =
    editor.tempNoteType!!.notetype.templatesNames.indexOfFirst { name ->
        onAllNodes(hasText(name) and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

fun ComposeTestRule.openTemplateActions(context: Context = ApplicationProvider.getApplicationContext()) {
    onAllNodesWithContentDescription(context.getString(AppCompatR.string.abc_action_menu_overflow_description)).onLast().performClick()
    waitForIdle()
}

fun ComposeTestRule.clickTemplateEditorAction(
    action: EditorControl,
    context: Context = ApplicationProvider.getApplicationContext(),
): Boolean {
    when (action) {
        EditorControl.BACK ->
            onAllNodesWithContentDescription(
                context.getString(AppCompatR.string.abc_action_bar_up_description),
            ).onLast().performClick()
        EditorControl.SAVE -> onAllNodesWithContentDescription(context.getString(CommonString.save)).onLast().performClick()
        EditorControl.PREVIEW ->
            onAllNodesWithContentDescription(
                context.getString(CommonString.card_editor_preview_card),
            ).onLast().performClick()
        else -> {
            openTemplateActions(context)
            val label =
                when (action) {
                    EditorControl.ADD -> context.getString(CommonString.add_card_type)
                    EditorControl.REPOSITION -> context.getString(CommonString.card_template_reposition_template)
                    EditorControl.RENAME -> context.getString(CommonString.rename_card_type)
                    EditorControl.DELETE -> context.getString(CommonString.delete_card_type)
                    EditorControl.BROWSER_APPEARANCE -> with(context) { TR.sentenceCase.browserAppearance }
                    EditorControl.INSERT_FIELD -> context.getString(CommonString.card_template_editor_insert_field)
                }
            onNode(hasText(label) and hasClickAction() and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        }
    }
    waitForIdle()
    ShadowLooper.idleMainLooper()
    return true
}
