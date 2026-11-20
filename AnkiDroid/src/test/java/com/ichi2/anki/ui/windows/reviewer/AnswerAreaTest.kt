// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki.ui.windows.reviewer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import com.ichi2.compose.theme.AnkiDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class AnswerAreaTest : RobolectricTest() {
    @get:Rule
    val composeRule = createComposeRule()

    private val showAnswer get() = targetContext.getString(CommonR.string.show_answer)
    private val again get() = targetContext.getString(CommonR.string.ease_button_again)
    private val hard get() = targetContext.getString(CommonR.string.ease_button_hard)
    private val good get() = targetContext.getString(CommonR.string.ease_button_good)
    private val easy get() = targetContext.getString(CommonR.string.ease_button_easy)

    private fun setAnswerArea(
        isAnswerShown: Boolean,
        nextTimes: AnswerButtonsNextTime? = null,
        showHardAndEasyButtons: Boolean = true,
        onRatingClicked: (Rating) -> Unit = {},
        onShowAnswerClicked: () -> Unit = {},
    ) {
        composeRule.setContent {
            AnkiDroidTheme {
                AnswerArea(
                    isAnswerShown = isAnswerShown,
                    nextTimes = nextTimes,
                    onRatingClicked = onRatingClicked,
                    onShowAnswerClicked = onShowAnswerClicked,
                    showHardAndEasyButtons = showHardAndEasyButtons,
                )
            }
        }
    }

    @Test
    fun `four ratings reflow into reading order at double text size`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                AnkiDroidTheme {
                    AnswerArea(isAnswerShown = true, nextTimes = null, onRatingClicked = {}, onShowAnswerClicked = {})
                }
            }
        }

        fun bounds(label: String) = composeRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        assertEquals(bounds(again).top, bounds(hard).top)
        assertTrue(bounds(good).top >= bounds(again).bottom, "Good and Easy must be in the second row")
        assertEquals(bounds(good).top, bounds(easy).top)
    }

    @Test
    fun `a rating keeps its size when pressed with Remove animations enabled`() {
        Prefs.sharedPrefs.edit { putBoolean(Prefs.key(R.string.safe_display_key), true) }
        setAnswerArea(isAnswerShown = true)
        val button = composeRule.onNodeWithText(good)
        val restingBounds = button.fetchSemanticsNode().boundsInRoot

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(200)

        assertEquals(restingBounds, button.fetchSemanticsNode().boundsInRoot)
        button.performTouchInput { cancel() }
    }

    @Test
    fun `clicking show answer button calls onShowAnswerClicked`() {
        var clicks = 0
        setAnswerArea(isAnswerShown = false, onShowAnswerClicked = { clicks++ })

        composeRule.onNodeWithText(showAnswer).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `clicking rating buttons calls onRatingClicked with correct rating`() {
        val ratings = mutableListOf<Rating>()
        setAnswerArea(isAnswerShown = true, onRatingClicked = { ratings += it })

        listOf(again, hard, good, easy).forEach { composeRule.onNodeWithText(it).performClick() }

        assertEquals(listOf(Rating.AGAIN, Rating.HARD, Rating.GOOD, Rating.EASY), ratings)
    }

    @Test
    fun `showing the answer shows rating buttons and hides show answer button`() {
        setAnswerArea(isAnswerShown = true)

        composeRule.onNodeWithText(showAnswer).assertIsNotDisplayed()
        listOf(again, hard, good, easy).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `showing the question hides rating buttons and shows show answer button`() {
        setAnswerArea(isAnswerShown = false)

        composeRule.onNodeWithText(showAnswer).assertIsDisplayed()
        listOf(again, hard, good, easy).forEach { composeRule.onNodeWithText(it).assertIsNotDisplayed() }
    }

    @Test
    fun `toggling the answer state swaps the visible buttons`() {
        var isAnswerShown by mutableStateOf(false)
        composeRule.setContent {
            AnkiDroidTheme {
                AnswerArea(
                    isAnswerShown = isAnswerShown,
                    nextTimes = null,
                    onRatingClicked = {},
                    onShowAnswerClicked = {},
                )
            }
        }

        isAnswerShown = true

        composeRule.onNodeWithText(showAnswer).assertIsNotDisplayed()
        composeRule.onNodeWithText(good).assertIsDisplayed()
    }

    @Test
    fun `hard and easy buttons can be hidden`() {
        setAnswerArea(isAnswerShown = true, showHardAndEasyButtons = false)

        composeRule.onNodeWithText(hard).assertDoesNotExist()
        composeRule.onNodeWithText(easy).assertDoesNotExist()
        composeRule.onNodeWithText(again).assertIsDisplayed()
        composeRule.onNodeWithText(good).assertIsDisplayed()
    }

    @Test
    fun `next times are shown above the ease names`() {
        val nextTimes = AnswerButtonsNextTime(again = "<1m", hard = "6m", good = "10m", easy = "4d")
        setAnswerArea(isAnswerShown = true, nextTimes = nextTimes)

        composeRule.onNodeWithText("<1m\n$again").assertIsDisplayed()
        composeRule.onNodeWithText("6m\n$hard").assertIsDisplayed()
        composeRule.onNodeWithText("10m\n$good").assertIsDisplayed()
        composeRule.onNodeWithText("4d\n$easy").assertIsDisplayed()
    }

    @Test
    fun `buttons height percentage scales the answer area height`() {
        var percentage by mutableStateOf(100)
        composeRule.setContent {
            AnkiDroidTheme {
                AnswerArea(
                    isAnswerShown = true,
                    nextTimes = null,
                    onRatingClicked = {},
                    onShowAnswerClicked = {},
                    modifier = Modifier.testTag("answer_area"),
                    buttonsHeightPercentage = percentage,
                )
            }
        }
        val node = composeRule.onNodeWithTag("answer_area")
        val defaultHeight = node.fetchSemanticsNode().size.height
        assertTrue(defaultHeight > 0)

        percentage = 200

        assertEquals(defaultHeight * 2, node.fetchSemanticsNode().size.height)
    }

    @Test
    fun `with a minimum press time, Show answer needs a press that long`() {
        var shown = 0
        var hints = 0
        composeRule.setContent {
            AnkiDroidTheme {
                AnswerArea(
                    isAnswerShown = false,
                    nextTimes = null,
                    onRatingClicked = {},
                    onShowAnswerClicked = { shown++ },
                    onShowAnswerPressTooShort = { hints++ },
                    showAnswerMinimumPressMs = 500,
                )
            }
        }
        val button = composeRule.onNodeWithText(showAnswer)

        button.performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals(0, shown, "a tap does not show the answer")
        assertEquals(1, hints, "a tap says why nothing happened")

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(600)
        assertEquals(1, shown, "a press held for the minimum time shows the answer")
        button.performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(100)
        assertEquals(1, shown)
        assertEquals(1, hints, "releasing after the answer showed is not a tap")
    }

    @Test
    fun `a second press within the double tap interval is ignored`() {
        var isAnswerShown by mutableStateOf(false)
        val presses = mutableListOf<String>()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AnkiDroidTheme {
                AnswerArea(
                    isAnswerShown = isAnswerShown,
                    nextTimes = null,
                    onRatingClicked = { presses += it.name },
                    onShowAnswerClicked = {
                        presses += "show"
                        isAnswerShown = true
                    },
                    doubleTapIntervalMs = 200,
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText(showAnswer).performClick()
        composeRule.mainClock.advanceTimeBy(50)
        composeRule.onNodeWithText(again).performClick()
        assertEquals(listOf("show"), presses, "Again pressed right after Show answer is a double tap")

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithText(good).performClick()
        assertEquals(listOf("show", "GOOD"), presses)
    }
}
