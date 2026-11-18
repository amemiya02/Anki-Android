// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.ImportDialog
import com.ichi2.anki.export.ExportDialogFragment
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.EmptyAnkiActivity
import com.ichi2.testutils.ext.reopenWithLanguage
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class RemainingDialogsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun applyDeviceConfig() {
        super.applyDeviceConfig()
        runTest { col.reopenWithLanguage(language.option) }
    }

    @Test
    fun editorDiscard() =
        withActivity { activity ->
            DiscardChangesDialog.showDialog(activity, message = TR.addingDiscardCurrentInput()) {}
            compose.onNodeWithText(with(activity) { TR.sentenceCase.keepEditing }).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(CommonString.discard)).assertIsDisplayed()
            captureScreen("editor_discard")
        }

    @Test
    fun collectionReplacement() =
        withActivity { activity ->
            ImportDialog
                .newInstance(ImportDialog.Type.DIALOG_IMPORT_REPLACE_CONFIRM, "/cache/Collection.colpkg")
                .showNow(activity.supportFragmentManager, "import")
            compose.onNodeWithText(activity.getString(CommonString.dialog_cancel)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(CommonString.paper_dialog_replace_collection)).assertIsDisplayed()
            captureScreen("replace_collection")
        }

    @Test
    fun deckExport() =
        withActivity { activity ->
            ExportDialogFragment.forDeckList(addDeck("日本語")).showNow(activity.supportFragmentManager, "export")
            compose.onNodeWithText("(.apkg)", substring = true).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(CommonString.dialog_cancel)).assertIsDisplayed()
            captureScreen("deck_export")
        }

    private fun withActivity(block: (EmptyAnkiActivity) -> Unit) {
        ensureCollectionLoadIsSynchronous()
        Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup().use { controller ->
            block(controller.get())
        }
    }
}
