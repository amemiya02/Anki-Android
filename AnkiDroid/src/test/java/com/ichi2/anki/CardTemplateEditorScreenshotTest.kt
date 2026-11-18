// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.libanki.NotetypeJson
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.testutils.ext.reopenWithLanguage
import com.ichi2.testutils.simulateKeyboard
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class CardTemplateEditorScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun editorAndActions() {
        runTest { col.reopenWithLanguage(language.option) }
        withCardTemplateEditor(vocabularyNoteType()) {
            compose.onNodeWithText(getString(R.string.note_type_templates)).assertIsDisplayed()
            val first = tempNoteType!!.notetype.templatesNames.first()
            compose.onNodeWithText(first).assertIsSelected()
            compose.selectSection(this, EditorViewType.BACK)
            simulateKeyboard()
            compose.onNodeWithText(templateSectionLabel(this, EditorViewType.BACK)).assertIsSelected()
            captureScreen("template")
            compose.openTemplateActions(this)
            compose.onNodeWithText(getString(CommonString.add_card_type)).assertExists()
            captureScreen("template_actions")
        }
    }

    @Test
    fun preview() {
        runTest { col.reopenWithLanguage(language.option) }
        val noteType = vocabularyNoteType()
        val first = noteType.templatesNames.first()
        ensureCollectionLoadIsSynchronous()
        val editorIntent = CardTemplateEditor.getIntent(targetContext, noteType.id)
        val previewIntent =
            ActivityScenario.launch<CardTemplateEditor>(editorIntent).use { scenario ->
                lateinit var editor: CardTemplateEditor
                scenario.onActivity { editor = it }
                compose.onNodeWithContentDescription(editor.getString(CommonString.card_editor_preview_card)).performClick()
                val shadow = shadowOf(editor)
                compose.waitUntil(10_000) {
                    ShadowLooper.idleMainLooper()
                    shadow.peekNextStartedActivity() != null
                }
                shadow.nextStartedActivity
            }
        ActivityScenario.launch<CardViewerActivity>(previewIntent).use {
            compose.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                runCatching { compose.onNodeWithText(first).assertIsSelected() }.isSuccess
            }
            compose.onNodeWithText(targetContext.getString(R.string.template_previewer_first_note)).assertIsDisplayed()
            compose.onNodeWithText(targetContext.getString(CommonString.show_answer)).assertIsDisplayed()
            captureScreen("template_preview")
        }
    }

    @Test
    fun tablet() {
        RuntimeEnvironment.setQualifiers("+sw700dp-w900dp-h800dp")
        runTest { col.reopenWithLanguage(language.option) }
        withCardTemplateEditor(vocabularyNoteType()) {
            compose.onNodeWithText(getString(R.string.note_type_templates)).assertIsDisplayed()
            compose.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                kotlin
                    .runCatching {
                        compose.onNodeWithText(getString(R.string.template_previewer_first_note)).assertIsDisplayed()
                    }.isSuccess
            }
            captureScreen("template_tablet")
        }
    }

    private fun vocabularyNoteType(): NotetypeJson {
        val name =
            addStandardNoteType(
                "日本語 語彙",
                arrayOf("Expression", "Reading", "Meaning", "Example", "Audio"),
                "<div class=\"expression\">{{Expression}}</div>",
                """
                {{FrontSide}}

                <hr id="answer">
                <div class="reading">{{furigana:Reading}}</div>
                <div class="meaning">{{Meaning}}</div>

                {{#Example}}
                  <p class="example">{{Example}}</p>
                {{/Example}}
                """.trimIndent(),
                templateCount = 2,
            )
        val noteType = checkNotNull(col.notetypes.byName(name))
        noteType.templates[0].name = "Recognition"
        noteType.templates[1].name = "Recall"
        noteType.templates[1].qfmt = "{{Meaning}}"
        noteType.css += "\n.expression { font-size: 38px; }"
        col.notetypes.save(noteType)
        addNoteUsingNoteTypeName(name, "熟語", "じゅくご", "idiom; compound word", "", "")
        return noteType
    }
}
