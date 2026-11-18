// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.graphics.Path
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.core.view.isVisible
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
import com.ichi2.anki.ui.windows.reviewer.whiteboard.WhiteboardFragment
import com.ichi2.testutils.ext.clear
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertTrue

/**
 * The study screen's tools around the Card (type-in-the-answer, whiteboard, voice recorder and
 * timer) against the Card Frame of the Core Loop Mockup, across the Quality Bar:
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light,dark,black --tests '*.StudyScreenToolsScreenshotTest'`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class StudyScreenToolsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun typeAnswer() {
        val did = addDeck("中文::HSK 4", setAsSelected = true)
        val card = addBasicWithTypingNote("熟悉", "shúxī").firstCard()
        col.setDeck(listOf(card.id), did)
        studying { reviewer ->
            assertTrue(reviewer.binding.typeAnswerContainer.isVisible, "the answer field shows")
            reviewer.binding.typeAnswerEditText.setText("shúxī")
            reviewer.binding.typeAnswerEditText.clearFocus()
            captureScreen("type_answer")
        }
    }

    @Test
    fun whiteboard() {
        StudyScreenRepository().isWhiteboardEnabled = true
        addCards()
        studying { reviewer ->
            val whiteboard = reviewer.childFragmentManager.findFragmentByTag(WhiteboardFragment::class.jvmName) as WhiteboardFragment
            whiteboard.viewModel.addPath(
                Path().apply {
                    moveTo(120f, 420f)
                    cubicTo(220f, 300f, 320f, 560f, 460f, 400f)
                },
            )
            ShadowLooper.idleMainLooper()
            captureScreen("whiteboard")
        }
    }

    @Test
    fun voiceRecorder() {
        StudyScreenRepository().isRecordVoiceEnabled = true
        addCards()
        studying { reviewer ->
            assertTrue(reviewer.binding.checkPronunciationContainer.isVisible, "the voice recorder shows")
            captureScreen("voice_recorder")
        }
    }

    @Test
    fun timer() {
        updateDeckConfig(col.decks.selected()) { jsonObject.put("timer", 1) }
        addCards()
        studying { reviewer ->
            assertTrue(reviewer.binding.timer.isVisible, "the answer timer shows")
            captureScreen("timer")
        }
    }

    private fun addCards() {
        val did = addDeck("中文::HSK 4", setAsSelected = true)
        listOf("熟悉" to "shúxī — familiar with", "认真" to "rènzhēn — serious")
            .forEach { (front, back) ->
                val card = addBasicNote(front, back).firstCard()
                col.setDeck(listOf(card.id), did)
            }
    }

    private fun studying(block: (ReviewerFragment) -> Unit) {
        ActivityScenario.launch<CardViewerActivity>(ReviewerFragment.getIntent(targetContext)).use { scenario ->
            lateinit var reviewer: ReviewerFragment
            // Robolectric's WebView never finishes loading the page: tell the study screen it did
            scenario.onActivity {
                reviewer = it.fragment as ReviewerFragment
                reviewer.viewModel.onPageFinished(false)
            }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText("HSK 4", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            ShadowLooper.idleMainLooper()
            scenario.onActivity { block(reviewer) }
        }
    }
}
