// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.ext.clear
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import com.ichi2.anki.common.android.R as CommonR

/**
 * The study screen's Card Frame against the Core Loop Mockup (`docs/redesign/core-loop/question-*.png`,
 * `answer-*.png`), across the Quality Bar:
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light,dark,black --tests '*.StudyScreenCardFrameScreenshotTest'`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class StudyScreenCardFrameScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun question() =
        studying {
            composeRule.onNodeWithText(showAnswer).assertIsDisplayed()
            captureScreen("question")
        }

    @Test
    fun answer() =
        studying {
            composeRule.onNodeWithText(showAnswer).performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                ShadowLooper.idleMainLooper()
                runCatching { composeRule.onNodeWithText(again, substring = true).assertIsDisplayed() }.isSuccess
            }
            captureScreen("answer")
        }

    private val showAnswer get() = targetContext.getString(CommonR.string.show_answer)
    private val again get() = targetContext.getString(CommonR.string.ease_button_again)

    private fun studying(block: () -> Unit) {
        val did = addDeck("中文::HSK 4", setAsSelected = true)
        listOf("熟悉" to "shúxī — familiar with", "认真" to "rènzhēn — serious", "安排" to "ānpái — arrange")
            .forEach { (front, back) ->
                val card = addBasicNote(front, back).firstCard()
                col.setDeck(listOf(card.id), did)
            }
        ActivityScenario.launch<CardViewerActivity>(ReviewerFragment.getIntent(targetContext)).use { scenario ->
            // Robolectric's WebView never finishes loading the page: tell the study screen it did
            scenario.onActivity { (it.fragment as ReviewerFragment).viewModel.onPageFinished(false) }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText("HSK 4", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            block()
        }
    }
}
