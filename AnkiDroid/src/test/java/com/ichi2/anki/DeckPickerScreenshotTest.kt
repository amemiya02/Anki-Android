// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.content.edit
import androidx.core.view.isVisible
import anki.decks.SetDeckCollapsedRequest
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.time.SECONDS_PER_DAY
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.libanki.DeckId
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The deck list against the Core Loop Mockups (`docs/redesign/core-loop/decks-*.png`), with the
 * Mockup's Decks, with long Chinese and Japanese names, and for a Newcomer, across the Quality Bar:
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light,dark,black --tests '*.DeckPickerScreenshotTest'`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class DeckPickerScreenshotTest : DeckListScreenshotTest() {
    /** The Mockup's sample Decks: one done for today, two collapsed, one expanded with the current sub-Deck */
    @Test
    fun decks() {
        seedMockupDecks()
        showDeckList("decks", withAddButton = false)
    }

    /** Long names wrap once and are then cut; the counts beside them stay in place */
    @Test
    fun long_names() {
        studiedToday(31, deck = addDeck("唐诗三百首 · 全本精选（附注释、译文与赏析）"))
        deck("汉语水平考试 HSK 5 级核心词汇 2500 · 分课练习::第 11–20 课：生词、固定搭配与例句", new = 15, learning = 4, due = 38)
        deck("汉语水平考试 HSK 5 级核心词汇 2500 · 分课练习::听力与阅读", new = 0, learning = 0, due = 24)
        val current = deck("日本語能力試験 N2 文法・語彙・読解 総合対策::第3章 敬語と複合動詞の使い分け", new = 10, learning = 5, due = 90)
        deck("日本語能力試験 N2 文法・語彙・読解 総合対策::読解練習", new = 10, learning = 0, due = 58)
        expand("汉语水平考试 HSK 5 级核心词汇 2500 · 分课练习")
        expand("日本語能力試験 N2 文法・語彙・読解 総合対策")
        col.decks.select(current)

        showDeckList("long_names", withAddButton = false)
    }

    /** A Newcomer with no Decks */
    @Test
    fun empty() {
        showDeckList("empty", withAddButton = false)
    }
}

/** Seeds Decks with chosen counts and opens the deck list */
abstract class DeckListScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    override fun setUp() {
        super.setUp()
        ensureCollectionLoadIsSynchronous()
        setIntroductionSlidesShown(true)
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        // suppress the periodic 'backup your collection' prompt so the screenshot is just the activity
        targetContext.sharedPrefs().edit { putBoolean("backupPromptDisabled", true) }
    }

    @After
    override fun tearDown() {
        super.tearDown()
        BackupManagerTestUtilities.reset()
    }

    protected fun seedMockupDecks() {
        studiedToday(9, deck = addDeck("Anatomy"))
        deck("English::Vocabulary", new = 7, learning = 0, due = 9)
        deck("中文::HSK 4", new = 8, learning = 2, due = 6)
        val current = deck("日本語::N3 語彙", new = 6, learning = 3, due = 7)
        deck("日本語::文法", new = 6, learning = 0, due = 4)
        expand("日本語")
        col.decks.select(current)
    }

    protected fun showDeckList(
        name: String,
        withAddButton: Boolean = true,
    ) {
        val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent())
        advanceRobolectricLooperUntil(lazyMessage = { "the deck list should be shown" }) {
            deckPicker.viewModel.flowOfDeckListScreen.value != null
        }
        // The add button is ticket 18's. The accessibility check estimates its label colour from the
        // picture (the background around its icon), and reports Deck rows lying under it
        if (!withAddButton) deckPicker.floatingActionButtonBinding.root.isVisible = false
        composeRule.waitForIdle()
        advanceRobolectricLooper()
        captureScreen(name)
    }

    /**
     * Adds a Deck holding [new] new Cards, [learning] Cards in learning and [due] review Cards
     * due today
     */
    protected fun deck(
        name: String,
        new: Int,
        learning: Int,
        due: Int,
    ): DeckId {
        val deckId = addDeck(name)

        fun cards(count: Int) = List(count) { addBasicNote("$name $it", "back").firstCard().id }.onEach { col.setDeck(listOf(it), deckId) }
        cards(new)
        cards(due).forEach {
            // a review Card due on the collection's first day: due today by the test clock, which the
            // deck list's counts follow (the Engine's own day is the real one)
            col.db.execute("update cards set type = 2, queue = 2, due = 0, ivl = 3 where id = ?", it)
        }
        cards(learning).forEach {
            // an intraday learning Card, due now
            col.db.execute("update cards set type = 1, queue = 1, due = ?, left = 1001 where id = ?", TimeManager.time.intTime(), it)
        }
        return deckId
    }

    protected fun expand(name: String) =
        col.decks.setCollapsed(col.decks.id(name), collapsed = false, SetDeckCollapsedRequest.Scope.REVIEWER)

    /** Logs [count] reviews in the Engine's today, of a Card in [deck] which is then done for today */
    protected fun studiedToday(
        count: Int,
        deck: DeckId,
    ) {
        val dayStart = (col.sched.dayCutoff - SECONDS_PER_DAY) * 1000
        val card = addBasicNote("studied", "today").firstCard().id
        col.setDeck(listOf(card), deck)
        repeat(count) {
            col.db.execute(
                "insert into revlog (id, cid, usn, ease, ivl, lastIvl, factor, time, type) values (?, ?, -1, 3, 1, 0, 2500, 6000, 1)",
                dayStart + 1 + it,
                card,
            )
        }
        // studied: not due again today
        col.sched.suspendCards(listOf(card))
    }
}
