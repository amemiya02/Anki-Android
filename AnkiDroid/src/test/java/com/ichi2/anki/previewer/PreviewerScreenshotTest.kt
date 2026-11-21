// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import anki.notetypes.StockNotetype
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.libanki.getStockNotetype
import com.ichi2.testutils.createTransientDirectory
import com.ichi2.testutils.ext.reopenWithLanguage
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class PreviewerScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun previewFromBrowser() {
        runTest { col.reopenWithLanguage(language.option) }
        val deck = addDeck("中文::HSK 4", setAsSelected = true)
        val notetype = requireNotNull(col.notetypes.byName(col.getStockNotetype(StockNotetype.Kind.KIND_BASIC).name))
        val ids =
            (1..25).flatMap {
                val note = col.newNote(notetype)
                note.setField(0, "熟悉")
                note.setField(1, "Familiar with")
                col.addNote(note, deck)
                note.cardIds(col)
            }
        assertEquals(1, col.setUserFlagForCards(listOf(ids[2]), Flag.RED.code).count)
        val intent =
            PreviewerFragment.getIntent(
                targetContext,
                idsFile = IdsFile(createTransientDirectory(), ids, IdsFile.Purpose.PREVIEW),
                currentIndex = 2,
            )
        ActivityScenario.launch<CardViewerActivity>(intent).use {
            val position = targetContext.getString(CommonString.preview_progress_bar_text, 3, 25)
            composeRule.onNodeWithText(position).assertIsDisplayed()
            val showAnswer = composeRule.onNodeWithText(targetContext.getString(CommonString.show_answer))
            showAnswer.assertIsDisplayed()
            captureScreen("question")

            showAnswer.performClick()
            val next = targetContext.getString(CommonString.previewer_next)
            composeRule.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText(next).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText(next).assertIsDisplayed()
            composeRule.mainClock.advanceTimeBy(500)
            composeRule.waitForIdle()
            captureScreen("answer")
        }
    }
}
