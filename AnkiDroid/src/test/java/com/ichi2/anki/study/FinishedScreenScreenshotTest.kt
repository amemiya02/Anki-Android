// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.libanki.DeckId
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertNotNull

/**
 * The finished screen against the Core Loop Mockups (`docs/redesign/core-loop/finished-*.png`),
 * in each state the Engine can report, across the Quality Bar:
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light,dark,black --tests '*.FinishedScreenScreenshotTest'`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class FinishedScreenScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun later() {
        val did = hsk4()
        col.config.set("collapseTime", 0)
        val config = col.decks.configDictForDeckId(did)
        config.new.delays = JSONArray(listOf(12))
        col.decks.save(config)
        addCards(did, 2)
        repeat(2) { col.sched.answerCard(assertNotNull(col.sched.card), Rating.AGAIN) }
        finished("later")
    }

    @Test
    fun limit() {
        val did = hsk4()
        val config = col.decks.configDictForDeckId(did)
        config.new.perDay = 0
        col.decks.save(config)
        addCards(did, 2)
        finished("limit")
    }

    @Test
    fun buried() {
        val did = hsk4()
        val cards = addCards(did, 2)
        col.sched.answerCard(assertNotNull(col.sched.card), Rating.EASY)
        col.sched.buryCards(listOf(cards.last()))
        finished("buried")
    }

    @Test
    fun done() {
        val did = hsk4()
        addCards(did, 1)
        col.sched.answerCard(assertNotNull(col.sched.card), Rating.EASY)
        finished("done")
    }

    @Test
    fun filtered() {
        addCards(hsk4(), 1)
        val filtered = addDynamicDeck("中文::Filtered", "")
        col.sched.emptyFilteredDeck(filtered)
        finished("filtered")
    }

    private fun hsk4(): DeckId = addDeck("中文::HSK 4", setAsSelected = true)

    private fun addCards(
        did: DeckId,
        count: Int,
    ) = listOf("熟悉" to "shúxī — familiar with", "认真" to "rènzhēn — serious")
        .take(count)
        .map { (front, back) ->
            addBasicNote(front, back).firstCard().id.also { col.setDeck(listOf(it), did) }
        }

    private fun finished(name: String) {
        val intent = FinishedFragment.getIntent(targetContext)
        ActivityScenario.launch<SingleFragmentActivity>(intent).use {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText("中文", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            captureScreen(name)
        }
    }
}
