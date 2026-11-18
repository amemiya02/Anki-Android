// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import anki.notetypes.StockNotetype
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.NotetypeFile
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.libanki.getStockNotetype
import com.ichi2.testutils.createTransientDirectory
import com.ichi2.testutils.ext.reopenWithLanguage
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class TemplatePreviewerScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun previewFromNoteEditor() {
        runTest { col.reopenWithLanguage(language.option) }
        val notetype = requireNotNull(col.notetypes.byName(col.getStockNotetype(StockNotetype.Kind.KIND_BASIC_AND_REVERSED).name))
        val arguments =
            TemplatePreviewerArguments(
                notetypeFile = NotetypeFile(createTransientDirectory(), notetype),
                fields = listOf("熟悉", "Familiar with"),
                tags = emptyList(),
            )
        val intent = TemplatePreviewerPage.getIntent(targetContext, arguments)

        ActivityScenario.launch<CardViewerActivity>(intent).use {
            val firstTemplate = notetype.templatesNames.first()
            composeRule.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText(firstTemplate).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText(firstTemplate).assertIsSelected()
            composeRule.onNodeWithText(targetContext.getString(CommonString.show_answer)).assertIsDisplayed()
            captureScreen("templates")
        }
    }
}
