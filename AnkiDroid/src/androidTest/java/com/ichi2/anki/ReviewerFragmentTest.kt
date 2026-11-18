// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.view.View
import android.webkit.WebView
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import anki.config.ConfigKey
import anki.config.copy
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.utils.ext.cardStateCustomizer
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.libanki.CardType
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.QueueType
import com.ichi2.anki.preferences.reviewer.ViewerAction
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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
    fun cardTemplateCanMarkAndToggleEveryFlag() {
        val card = addCardToTestDeck()
        withReviewerWebView { webView ->
            val result =
                webView.runCardApiScript(
                    """
                    const marked = await api.ankiMarkCard();
                    const values = [marked.value, (await api.ankiGetCardMark()).value];
                    for (const flag of ["red", "orange", "green", "blue", "pink", "turquoise", "purple", "purple", "red", "none"]) {
                        values.push((await api.ankiToggleFlag(flag)).value);
                        values.push((await api.ankiGetCardFlag()).value);
                    }
                    return values;
                    """.trimIndent(),
                )
            assertThat(result, equalTo("[true,true,true,1,true,2,true,3,true,4,true,5,true,6,true,7,true,0,true,1,true,0]"))
            assertThat(col.getNote(card.nid).tags, equalTo(listOf("marked")))
            assertThat(col.getCard(card.id).userFlag(), equalTo(0))
        }
    }

    @Test
    fun cardTemplateCanBuryOneCard() = assertCardTemplateChangesQueue("ankiBuryCard", QueueType.ManuallyBuried, allCards = false)

    @Test
    fun cardTemplateCanBuryTheWholeNote() = assertCardTemplateChangesQueue("ankiBuryNote", QueueType.ManuallyBuried, allCards = true)

    @Test
    fun cardTemplateCanSuspendOneCard() = assertCardTemplateChangesQueue("ankiSuspendCard", QueueType.Suspended, allCards = false)

    @Test
    fun cardTemplateCanSuspendTheWholeNote() = assertCardTemplateChangesQueue("ankiSuspendNote", QueueType.Suspended, allCards = true)

    private fun assertCardTemplateChangesQueue(
        method: String,
        expectedQueue: QueueType,
        allCards: Boolean,
    ) {
        val note = col.newNote(checkNotNull(col.notetypes.byName("Basic (and reversed card)")))
        note.setField(0, "Front of a two-card Note")
        note.setField(1, "Back of a two-card Note")
        col.addNote(note, testDeckId)
        val cards = note.cards(col)
        assertThat(cards.size, equalTo(2))
        val nextNote = addCardToTestDeck()

        withReviewerWebView { webView ->
            val currentCardId =
                JSONObject(webView.runCardApiScript("return { id: (await api.ankiGetCardId()).value };")).getLong("id")
            assertThat(cards.any { it.id == currentCardId }, equalTo(true))
            assertThat(webView.runCardApiScript("return (await api.$method()).value;"), equalTo("true"))
            for (card in cards) {
                val expected = if (card.id == currentCardId || allCards) expectedQueue else QueueType.New
                assertThat(col.getCard(card.id).queue, equalTo(expected))
            }
            assertThat(col.getCard(nextNote.id).queue, equalTo(QueueType.New))
            assertThat(
                webView.runCardApiScript("return (await api.ankiGetCardId()).value !== $currentCardId;"),
                equalTo("true"),
            )
        }
    }

    @Test
    fun cardTemplateCanSetTheCurrentCardsDueDate() {
        val card = addCardToTestDeck()
        addCardToTestDeck()
        withReviewerWebView { webView ->
            assertThat(webView.runCardApiScript("return (await api.ankiSetCardDue(10000)).value;"), equalTo("false"))
            assertThat(webView.runCardApiScript("return (await api.ankiSetCardDue(15)).value;"), equalTo("true"))
            assertThat(col.getCard(card.id).due, equalTo(col.sched.today + 15))
        }
    }

    @Test
    fun cardTemplateCanResetTheCurrentCardsProgress() {
        val card = addCardToTestDeck()
        card.ivl = 8
        card.moveToReviewQueue()

        withReviewerWebView { webView ->
            assertThat(webView.runCardApiScript("return (await api.ankiResetProgress()).value;"), equalTo("true"))
            val reset = col.getCard(card.id)
            assertThat(reset.type, equalTo(CardType.New))
            assertThat(reset.ivl, equalTo(0))
        }
    }

    @Test
    fun cardTemplateKeepsPermissionChecksForSearchAndCrossNoteTags() {
        val card = addCardToTestDeck()
        val back = """A "quoted" field with <em>markup</em>"""
        val otherNote = addNoteUsingBasicNoteType("searchCallbackTarget", back)
        val otherCard = otherNote.firstCard(col)
        val prefs = testContext.sharedPrefs()
        val key = testContext.getString(R.string.pref_allow_dangerous_js_api)
        val originalValue = prefs.getBoolean(key, false)

        try {
            prefs.edit { putBoolean(key, false) }
            withReviewerWebView { webView ->
                val denied =
                    webView.runCardApiScript(
                        """
                        window.searchResults = null;
                        addHook("ankiSearchCard", results => { window.searchResults = results; });
                        return [
                            (await api.ankiSearchCardWithCallback("searchCallbackTarget")).value,
                            (await api.ankiAddTagToNote(${otherNote.id}, "remote")).value,
                            (await api.ankiAddTagToNote(${card.nid}, "local")).value,
                            JSON.parse((await api.ankiGetNoteTags()).value),
                            window.searchResults,
                        ];
                        """.trimIndent(),
                    )
                assertThat(denied, equalTo("""[false,false,true,["local"],null]"""))
                assertThat(col.getNote(otherNote.id).tags, equalTo(emptyList()))

                prefs.edit { putBoolean(key, true) }
                val allowed =
                    webView.runCardApiScript(
                        """
                        return [
                            (await api.ankiSearchCardWithCallback("searchCallbackTarget")).value,
                            (await api.ankiAddTagToNote(${otherNote.id}, "remote")).value,
                            (await api.ankiSetNoteTags(["a tag", "kept"])).value,
                            JSON.parse((await api.ankiGetNoteTags()).value),
                        ];
                        """.trimIndent(),
                    )
                assertThat(allowed, equalTo("""[true,true,true,["a_tag","kept"]]"""))
                webView.awaitJavascript("Boolean(window.searchResults)")
                val match = webView.evaluate("window.searchResults.find(result => result.noteId === ${otherNote.id})")
                val data = JSONObject(match)
                assertThat(data.getLong("cardId"), equalTo(otherCard.id))
                assertThat(data.getJSONObject("fieldsData").getString("Back"), equalTo(back))
                assertThat(col.getNote(otherNote.id).tags, equalTo(listOf("remote")))
            }
        } finally {
            prefs.edit { putBoolean(key, originalValue) }
            col.removeNotes(listOf(otherNote.id))
        }
    }

    @Test
    fun cardTemplateCanControlTheCardFrameAndOpenMenus() {
        addCardToTestDeck()
        withReviewerWebView { webView ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val device = UiDevice.getInstance(instrumentation)

            fun assertFrame(
                toolbarVisible: Boolean,
                answersVisible: Boolean,
            ) {
                instrumentation.runOnMainSync {
                    val frame = webView.rootView
                    assertThat(frame.requireViewById<View>(R.id.study_bar).isShown, equalTo(toolbarVisible))
                    assertThat(frame.requireViewById<View>(R.id.answer_area).isShown, equalTo(answersVisible))
                }
            }

            assertThat(
                webView.runCardApiScript(
                    """
                    return [
                        (await api.ankiSetFullscreenButtonsOnly()).value,
                        (await api.ankiIsInFullscreen()).value,
                        (await api.ankiIsTopbarShown()).value,
                    ];
                    """.trimIndent(),
                ),
                equalTo("[true,true,false]"),
            )
            assertFrame(toolbarVisible = false, answersVisible = true)

            assertThat(webView.runCardApiScript("return (await api.ankiSetFullscreenAllGone()).value;"), equalTo("true"))
            assertFrame(toolbarVisible = false, answersVisible = false)

            assertThat(webView.runCardApiScript("return (await api.ankiShowOptionsMenu()).value;"), equalTo("true"))
            assertThat(device.wait(Until.hasObject(By.text(ViewerAction.MARK.title(testContext))), 10000), equalTo(true))
            device.pressBack()

            assertThat(
                webView.runCardApiScript(
                    """
                    return [
                        (await api.ankiSetFullscreenButtonsAndMenu()).value,
                        (await api.ankiIsInFullscreen()).value,
                        (await api.ankiIsTopbarShown()).value,
                        (await api.ankiEnableHorizontalScrollbar(false)).value,
                        (await api.ankiEnableVerticalScrollbar(true)).value,
                    ];
                    """.trimIndent(),
                ),
                equalTo("[true,false,true,true,true]"),
            )
            assertFrame(toolbarVisible = true, answersVisible = true)
            instrumentation.runOnMainSync {
                assertThat(webView.isHorizontalScrollBarEnabled, equalTo(false))
                assertThat(webView.isVerticalScrollBarEnabled, equalTo(true))
            }

            assertThat(webView.runCardApiScript("return (await api.ankiShowNavigationDrawer()).value;"), equalTo("true"))
            assertThat(device.wait(Until.hasObject(By.text(CollectionManager.TR.actionsDecks())), 10000), equalTo(true))
            device.pressBack()
        }
    }

    @Test
    fun cardTemplateCanOpenTagsAndSearchInTheBrowser() {
        addCardToTestDeck()
        withReviewerWebView { webView ->
            assertThat(webView.runCardApiScript("return (await api.ankiAddTagToCard()).value;"), equalTo("true"))
            onView(withId(R.id.tags_list)).check(matches(isDisplayed()))
            pressBack()

            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val browserMonitor = instrumentation.addMonitor(CardBrowser::class.java.name, null, false)
            try {
                assertThat(webView.runCardApiScript("""return (await api.ankiSearchCard("foo")).value;"""), equalTo("true"))
                checkNotNull(browserMonitor.waitForActivityWithTimeout(5000)) { "Card Browser did not open" }
            } finally {
                instrumentation.removeMonitor(browserMonitor)
                browserMonitor.lastActivity?.let { activity -> instrumentation.runOnMainSync { activity.finish() } }
            }
        }
    }

    @Test
    fun cardTemplateCanReloadItsCurrentContent() {
        addCardToTestDeck()
        withReviewerWebView { webView ->
            val content = "document.getElementById('qa').textContent"
            val original = webView.evaluate(content)
            webView.evaluate(
                """
                document.getElementById("qa").textContent = "Temporary Card Content";
                reloadPage();
                """.trimIndent(),
            )
            var actual = ""
            waitUntil(message = { "Card Content after reload was $actual. Expected $original" }) {
                actual = webView.evaluate(content)
                actual == original
            }
        }
    }

    @Test
    fun cardTemplateCanCallTheAndroidTextToSpeechEngine() {
        addCardToTestDeck()
        withReviewerWebView { webView ->
            val response =
                webView.runCardApiScript(
                    """
                    await api.ankiTtsIsSpeaking();
                    const deadline = Date.now() + 4000;
                    let language;
                    do {
                        language = await api.ankiTtsSetLanguage("en-US");
                        if (language.value >= 0) break;
                        await new Promise(resolve => setTimeout(resolve, 50));
                    } while (Date.now() < deadline);
                    return {
                        language: language.value,
                        pitch: (await api.ankiTtsSetPitch(1.1)).value,
                        rate: (await api.ankiTtsSetSpeechRate(1)).value,
                        speak: (await api.ankiTtsSpeak("Study Card.", 0)).value,
                        isSpeaking: (await api.ankiTtsIsSpeaking()).value,
                        stop: (await api.ankiTtsStop()).value,
                    };
                    """.trimIndent(),
                )
            val results = JSONObject(response)
            assertThat(results.getInt("language") in -2..2, equalTo(true))
            assertThat(results.getInt("pitch"), equalTo(0))
            assertThat(results.getInt("rate"), equalTo(0))
            assertThat(results.get("isSpeaking") is Boolean, equalTo(true))
            // Some system images contain no synthesizer or usable voice.
            val engineReady = results.getInt("language") >= 0
            assertThat(results.getInt("speak") in (if (engineReady) 0..0 else -1..0), equalTo(true))
            assertThat(results.getInt("stop") in (if (engineReady) 0..0 else -1..0), equalTo(true))
        }
    }

    @Test
    fun cardTemplateGetsASpeechRecognitionErrorWithoutRecordingConsent() {
        addCardToTestDeck()
        val originalValue = Prefs.allowTemplatesToRecordAudio
        try {
            Prefs.allowTemplatesToRecordAudio = false
            withReviewerWebView { webView ->
                val response =
                    webView.runCardApiScript(
                        """
                        window.recognition = null;
                        addHook("ankiSttResult", result => { window.recognition = result; });
                        return [
                            (await api.ankiSttSetLanguage("en-US")).value,
                            (await api.ankiSttStart()).value,
                            (await api.ankiSttStop()).value,
                            (await api.ankiTtsFieldModifierIsAvailable()).value,
                            (await api.ankiShowToast(encodeURIComponent("日本語 + English"), false)).value,
                        ];
                        """.trimIndent(),
                    )
                assertThat(response, equalTo("[true,false,true,false,true]"))
                webView.awaitJavascript("Boolean(window.recognition)")
                assertThat(
                    webView.evaluate("window.recognition"),
                    equalTo("""{"success":false,"value":"Audio recording is disabled for card templates."}"""),
                )
            }
        } finally {
            Prefs.allowTemplatesToRecordAudio = originalValue
        }
    }

    @Test
    fun audioAndSpeechPlayButtonsFollowTheCollectionSetting() {
        val noteType = col.notetypes.copy(col.notetypes.byName("Basic")!!)
        val key = ConfigKey.Bool.HIDE_AUDIO_PLAY_BUTTONS
        val original = col.config.getBool(key)
        try {
            noteType.templates[0].qfmt = "<p id='audio-question'>{{Front}}</p>{{tts en_GB:Back}}"
            col.notetypes.save(noteType)
            val note =
                col.newNote(noteType).apply {
                    setField(0, "Audio question [sound:recording.mp3]")
                    setField(1, "Text to speak")
                }
            col.addNote(note, testDeckId)
            for (hide in listOf(false, true)) {
                col.config.setBool(key, hide)
                withReviewerWebView { webView ->
                    webView.awaitJavascript("document.getElementById('audio-question')?.textContent.includes('Audio question')")
                    assertEquals(
                        if (hide) "0" else "2",
                        webView.evaluate("document.querySelectorAll('a.replay-button').length"),
                    )
                }
            }
        } finally {
            col.config.setBool(key, original)
            col.notetypes.remove(noteType.id)
        }
    }

    @Test
    fun cardTemplateReadsTheReviewerApiValuesOnBothSides() {
        val noteType = col.notetypes.copy(col.notetypes.byName("Basic")!!)
        val preferences = col.getPreferences()
        val sharedPrefs = testContext.sharedPrefs()
        val etaKey = "showETA"
        val storedEta = sharedPrefs.all[etaKey] as? Boolean
        sharedPrefs.edit { putBoolean(etaKey, false) }
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

            val card = note.firstCard(col)
            // Compare the public JSON responses with the fixture, including types and values
            // previously compared across both reviewers in ticket 22.
            val cardData =
                mapOf(
                    "NewCardCount" to 1,
                    "LrnCardCount" to 0,
                    "RevCardCount" to 0,
                    "ETA" to 0,
                    "CardMark" to true,
                    "CardFlag" to 0,
                    "CardReps" to 0,
                    "CardInterval" to 0,
                    "CardFactor" to card.factor,
                    "CardMod" to card.mod,
                    "CardId" to card.id,
                    "CardNid" to note.id,
                    "CardType" to 0,
                    "CardDid" to testDeckId,
                    "CardLeft" to 0,
                    "CardODid" to 0,
                    "CardODue" to 0,
                    "CardQueue" to 0,
                    "CardLapses" to 0,
                    "CardDue" to card.due,
                    "DeckName" to col.decks.name(testDeckId),
                    "NoteTags" to JSONArray(listOf("marked")).toString(),
                )
            val queue = assertNotNull(col.sched.currentQueueState())
            val intervals =
                col.sched
                    .describeNextStates(queue.states)
                    .mapIndexed { index, interval -> "NextTime${index + 1}" to interval }
                    .toMap()
            withReviewerWebView { webView ->
                val question = webView.readCardDataSide("question")
                assertCardData(question, cardData + intervals.mapValues { "" } + ("IsDisplayingAnswer" to false))

                webView.evaluate("void window.flipTestCard()")

                val answer = webView.readCardDataSide("answer")
                assertCardData(answer, cardData + intervals + ("IsDisplayingAnswer" to true))
                onView(withId(android.R.id.button1)).check(doesNotExist())
            }
        } finally {
            sharedPrefs.edit {
                if (storedEta == null) remove(etaKey) else putBoolean(etaKey, storedEta)
            }
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

    private fun assertCardData(
        response: JSONObject,
        expected: Map<String, Any>,
    ) {
        assertEquals(expected.keys, response.keys().asSequence().toSet())
        for ((name, value) in expected) {
            val raw = response.get(name)
            val result =
                if (name.startsWith("NextTime") || name == "DeckName") {
                    JSONObject(assertIs<String>(raw, name))
                } else {
                    assertIs<JSONObject>(raw, name)
                }
            assertEquals(true, result.get("success"), name)
            val actual = result.get("value")
            if (value is Number) {
                assertEquals(value.toLong(), assertIs<Number>(actual, name).toLong(), name)
            } else {
                assertEquals(value, actual, name)
            }
        }
    }

    private fun WebView.readCardDataSide(side: String): JSONObject {
        val data = "document.getElementById('$side-data').textContent"
        awaitJavascript("Boolean(document.getElementById('$side-data')?.textContent)")
        return JSONObject(evaluate("JSON.parse($data)"))
    }

    private fun withReviewerWebView(block: (WebView) -> Unit) {
        ActivityScenario.launch<CardViewerActivity>(ReviewerFragment.getIntent(testContext)).use { scenario ->
            lateinit var webView: WebView
            scenario.onActivity { activity ->
                webView = (activity.fragment as ReviewerFragment).binding.webViewLayout.getChildAt(0) as WebView
            }
            webView.awaitJavascript("Boolean(document.getElementById('qa')?.textContent.trim())")
            block(webView)
        }
    }

    private fun WebView.runCardApiScript(script: String): String {
        evaluate(
            """
            window.testApiResult = null;
            void (async () => {
                const api = AnkiDroidJS.init($JS_API_OPTIONS);
                $script
            })().then(
                value => { window.testApiResult = JSON.stringify(value); },
                error => { window.testApiResult = JSON.stringify(String(error)); }
            );
            """.trimIndent(),
        )
        awaitJavascript("Boolean(window.testApiResult)")
        return evaluate("JSON.parse(window.testApiResult)")
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
