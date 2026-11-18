// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.servicelayer.LanguageHintService.setLanguageHintForField
import com.ichi2.testutils.ext.reopenWithLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class NoteTypesScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun note_type_workflow() {
        runBlocking { col.reopenWithLanguage(language.option) }
        val studyType = "日本語 語彙"
        addStandardNoteType(
            studyType,
            arrayOf("Expression", "Reading", "Meaning", "Example", "Audio"),
            "{{Expression}}",
            "{{Reading}}, {{Meaning}}",
            templateCount = 2,
        )
        addStandardNoteType("汉字 HSK", arrayOf("汉字", "拼音", "释义"), "{{汉字}}", "{{拼音}} · {{释义}}")
        addNoteUsingNoteTypeName(studyType, "橋", "はし", "Bridge", "", "")
        addNoteUsingNoteTypeName("汉字 HSK", "熟悉", "shúxī", "Familiar")
        val noteType = checkNotNull(col.notetypes.byName(studyType))
        noteType.templates[0].name = "Recognition"
        noteType.templates[1].name = "Recall"
        col.notetypes.save(noteType)
        setLanguageHintForField(col.notetypes, noteType, 0, Locale.JAPANESE)
        setLanguageHintForField(col.notetypes, noteType, 1, Locale.JAPANESE)
        col.db.execute("update col set ls = scm")
        ensureCollectionLoadIsSynchronous()
        val activity = startRegularActivity<ManageNotetypes>()

        compose.onNodeWithText(activity.getString(R.string.note_types_title)).assertIsDisplayed()
        captureScreen("note_types")

        compose.onNodeWithContentDescription(activity.getString(R.string.note_type_add_title)).performClick()
        compose.onNodeWithText(activity.getString(R.string.note_type_add_title)).assertIsDisplayed()
        captureScreen("add_note_type")
        compose.onNodeWithText(activity.getString(CommonString.dialog_cancel)).performClick()
        compose.waitForIdle()

        // At 200% the long stock type names can put the example below the initial viewport.
        activity.viewModel.filter(studyType)
        compose.onNodeWithText(studyType).performClick()
        compose.onNodeWithText(activity.getString(R.string.note_type_fields)).assertIsDisplayed()
        captureScreen("note_type")

        compose.onNodeWithText("Expression").performClick()
        compose.onNodeWithText(activity.getString(CommonString.model_field_editor_rename)).assertIsDisplayed()
        captureScreen("field_actions")
        compose.onNodeWithText(activity.getString(CommonString.model_field_editor_delete)).performClick()
        compose.onNodeWithText(activity.getString(R.string.note_type_full_sync_title)).assertIsDisplayed()
        captureScreen("full_sync")
    }
}
