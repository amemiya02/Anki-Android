// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Activity
import android.content.Intent
import android.webkit.WebView
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.config.copy
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.utils.ext.cardStateCustomizer
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.AvoidDayRolloverRule
import com.ichi2.anki.testutil.GrantStoragePermission.storagePermission
import com.ichi2.anki.testutil.awaitDisplayed
import com.ichi2.anki.testutil.awaitJavascript
import com.ichi2.anki.testutil.clickShowAnswer
import com.ichi2.anki.testutil.ensureWebViewIsSupported
import com.ichi2.anki.testutil.evaluate
import com.ichi2.anki.testutil.goodButton
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.testutil.notificationPermission
import com.ichi2.anki.testutil.waitUntil
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ReviewerFragmentTest : InstrumentedTest() {
    @get:Rule(order = 0)
    val avoidDayRollover = AvoidDayRolloverRule()

    @get:Rule
    val runtimePermissionRule = grantPermissions(storagePermission, notificationPermission)

    @get:Rule
    val composeRule = createEmptyComposeRule()

    /** The collection is shared between tests: review a deck which only contains this test's cards */
    private var testDeckId: DeckId = 0

    @Before
    fun setUp() {
        testContext.sharedPrefs().edit {
            putBoolean("newReviewer", true)
            putBoolean("newReviewerOptions", true)
        }
        testDeckId = col.decks.addNormalDeckWithName("ReviewerFragmentTest-${UUID.randomUUID()}").id
        col.decks.select(testDeckId)
    }

    @After
    fun tearDown() {
        col.decks.remove(listOf(testDeckId))
        col.config.cardStateCustomizer = ""
    }

    @Test
    fun testCustomSchedulerWithCustomData() = testCustomSchedulerWithCustomData(schedulerDelayMs = 0)

    /**
     * Issue 17298: a card must not be answered until the custom scheduler has
     * completed (statesMutated).
     */
    @Test
    fun testCustomSchedulerWithCustomDataAndSlowScheduler() = testCustomSchedulerWithCustomData(schedulerDelayMs = 5000)

    private fun testCustomSchedulerWithCustomData(schedulerDelayMs: Long) {
        val delayJs =
            if (schedulerDelayMs > 0) {
                "await new Promise(resolve => setTimeout(resolve, $schedulerDelayMs));"
            } else {
                ""
            }
        col.config.cardStateCustomizer =
            """
            $delayJs
            states.good.normal.review.easeFactor = 3.0;
            states.good.normal.review.scheduledDays = 123;
            customData.good.c += 1;
            """
        val card = addCardToTestDeck()
        card.moveToReviewQueue()
        col.backend.updateCards(
            listOf(
                card
                    .toBackendCard()
                    .toBuilder()
                    .setCustomData("""{"c":1}""")
                    .build(),
            ),
            true,
        )

        withReviewer {
            var cardFromDb = col.getCard(card.id).toBackendCard()
            assertThat(cardFromDb.easeFactor, equalTo(card.factor))
            assertThat(cardFromDb.interval, equalTo(card.ivl))
            assertThat(cardFromDb.customData, equalTo("""{"c":1}"""))

            clickShowAnswerAndAnswerGood()
            // Answering runs on the IO dispatcher, which Espresso does not wait for.
            waitUntil(message = { "The review of card ${card.id} was not saved" }) {
                col.getCard(card.id).reps == card.reps + 1
            }

            cardFromDb = col.getCard(card.id).toBackendCard()
            assertThat(cardFromDb.easeFactor, equalTo(3000))
            assertThat(cardFromDb.interval, equalTo(123))
            assertThat(cardFromDb.customData, equalTo("""{"c":2}"""))
        }
    }

    @Test
    fun testCustomSchedulerWithRuntimeError() {
        // Issue 15035 - runtime errors weren't handled
        col.config.cardStateCustomizer = "states.this_is_not_defined.normal.review = 12;"
        addCardToTestDeck()

        withReviewer {
            clickShowAnswer()
            ensureAnswerButtonsAreDisplayed()
        }
    }

    @Test
    fun cardTemplateShowsAndGradesItsOwnAnswer() {
        col.config.cardStateCustomizer = "await new Promise(resolve => setTimeout(resolve, 500));"
        assertCardTemplateGradesItsAnswer(
            frontAction =
                """
                void (async () => {
                    const api = new AnkiDroidJS($JS_API_OPTIONS);
                    await api.ankiShowAnswer();
                    if ((await api.ankiIsDisplayingAnswer()).value) await api.ankiAnswerEase4();
                })()
                """.trimIndent(),
            backAction = "",
            expectedRating = 4,
        )
    }

    @Test
    fun cardTemplateCanShowAndGradeAnAnswerWithTheOlderHelpers() =
        assertCardTemplateGradesItsAnswer(
            frontAction = "showAnswer()",
            backAction = "buttonAnswerEase2()",
            expectedRating = 2,
        )

    private fun assertCardTemplateGradesItsAnswer(
        frontAction: String,
        backAction: String,
        expectedRating: Int,
    ) {
        val noteType = col.notetypes.copy(col.notetypes.byName("Basic")!!)
        try {
            noteType.templates[0].qfmt = "{{Front}}<script>$frontAction;</script>"
            noteType.templates[0].afmt =
                """
                {{Back}}
                <script>
                    if (!window.gradedTestCard) {
                        window.gradedTestCard = true;
                        $backAction;
                    }
                </script>
                """.trimIndent()
            col.notetypes.save(noteType)
            val note =
                col.newNote(noteType).apply {
                    setField(0, "Question")
                    setField(1, "Answer")
                }
            col.addNote(note, testDeckId)
            val card = note.firstCard(col)

            withReviewer {
                waitUntil(message = { "The Card Template did not grade the displayed Card" }) {
                    col.getCard(card.id).reps == 1
                }
                val review = col.cardStatsData(card.id).revlogList.single()
                assertThat(review.buttonChosen, equalTo(expectedRating))
            }
        } finally {
            col.notetypes.remove(noteType.id)
        }
    }

    @Test
    fun cardTemplateReadsTheSameValuesInBothStudyScreens() {
        val noteType = col.notetypes.copy(col.notetypes.byName("Basic")!!)
        val preferences = col.getPreferences()
        col.setPreferences(preferences.copy { reviewing = reviewing.copy { showIntervalsOnButtons = true } })
        try {
            noteType.templates[0].qfmt = cardDataTemplate("question", "Front")
            noteType.templates[0].afmt = cardDataTemplate("answer", "Back")
            col.notetypes.save(noteType)
            val note =
                col.newNote(noteType).apply {
                    setField(0, "Question")
                    setField(1, "Answer")
                    addTag("marked")
                }
            col.addNote(note, testDeckId)

            val legacy = readCardData(Intent(testContext, Reviewer::class.java))
            val study = readCardData(ReviewerFragment.getIntent(testContext))
            assertThat(study, equalTo(legacy))
        } finally {
            col.setPreferences(preferences)
            col.notetypes.remove(noteType.id)
        }
    }

    private fun cardDataTemplate(
        side: String,
        field: String,
    ): String =
        """
        {{$field}}
        <pre id="$side-data"></pre>
        <script>
            (() => {
                const api = AnkiDroidJS.init($JS_API_OPTIONS);
                const output = document.getElementById("$side-data");
                const read = async () => {
                    const suffixes = [
                        "NewCardCount", "LrnCardCount", "RevCardCount", "ETA", "CardMark", "CardFlag",
                        "NextTime1", "NextTime2", "NextTime3", "NextTime4", "CardReps", "CardInterval",
                        "CardFactor", "CardMod", "CardId", "CardNid", "CardType", "CardDid", "CardLeft",
                        "CardODid", "CardODue", "CardQueue", "CardLapses", "CardDue", "DeckName", "NoteTags",
                    ];
                    const values = {};
                    for (const suffix of suffixes) values[suffix] = await api["ankiGet" + suffix]();
                    values.IsDisplayingAnswer = await api.ankiIsDisplayingAnswer();
                    if (output.id === "answer-data" && JSON.parse(values.NextTime1).value === "") {
                        setTimeout(read, 20);
                        return;
                    }
                    output.textContent = JSON.stringify(values);
                };
                read();
                window.flipTestCard = () => api.ankiShowAnswer();
            })();
        </script>
        """.trimIndent()

    private fun readCardData(intent: Intent): List<String> =
        ActivityScenario.launch<Activity>(intent).use { scenario ->
            lateinit var webView: WebView
            scenario.onActivity { activity ->
                webView =
                    when (activity) {
                        is Reviewer -> checkNotNull(activity.webView)
                        is CardViewerActivity -> (activity.fragment as ReviewerFragment).binding.webViewLayout.getChildAt(0) as WebView
                        else -> error("Expected a study screen")
                    }
            }
            val question = webView.readCardDataSide("question")
            webView.evaluate("void window.flipTestCard()")
            val answer = webView.readCardDataSide("answer")
            onView(withText(CommonString.feature_not_supported_by_study_screen)).check(doesNotExist())
            listOf(question, answer)
        }

    private fun WebView.readCardDataSide(side: String): String {
        val data = "document.getElementById('$side-data').textContent"
        awaitJavascript("Boolean(document.getElementById('$side-data')?.textContent)")
        return evaluate("JSON.parse($data)")
    }

    private fun addCardToTestDeck(): Card = addNoteUsingBasicNoteType("foo", "bar").firstCard(col).update { did = testDeckId }

    private fun withReviewer(block: () -> Unit) {
        ActivityScenario.launch<CardViewerActivity>(ReviewerFragment.getIntent(testContext)).use { block() }
    }

    private fun clickShowAnswerAndAnswerGood() {
        clickShowAnswer()
        ensureAnswerButtonsAreDisplayed()
        composeRule.mainClock.advanceTimeBy(Prefs.doubleTapInterval.toLong())
        composeRule.goodButton().performClick()
    }

    private fun clickShowAnswer() = composeRule.clickShowAnswer()

    private fun ensureAnswerButtonsAreDisplayed() {
        // We need to wait for the card to fully load to allow enough time for
        // the messages to be passed in and out of the WebView when evaluating
        // the custom JS scheduler code. The ease buttons are hidden until the
        // custom scheduler has finished running
        // Increase to a max of 30 seconds because CI builds can be very slow
        composeRule.awaitDisplayed(timeout = 30.seconds) { goodButton() }
    }

    companion object {
        private const val JS_API_OPTIONS = """{ developer: "test@example.com", version: "0.0.3" }"""

        @JvmStatic
        @BeforeClass
        fun checkWebView() = ensureWebViewIsSupported()
    }
}
