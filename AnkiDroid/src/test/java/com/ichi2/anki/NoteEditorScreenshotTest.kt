// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.RoundedCornerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.ui.TransitionDirection
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.noteeditor.toIntent
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.insetsOf
import com.ichi2.testutils.simulateKeyboard
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * Screenshot tests for [NoteEditorActivity]
 *
 * `./gradlew :AnkiDroid:verifyRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.NoteEditorScreenshotTest"`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class NoteEditorScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun duplicateFirstField() {
        addBasicNote("熟悉", "familiar with")
        withNoteEditor {
            val front =
                col.notetypes.basic.fields
                    .first()
                    .name
            onView(allOf(withContentDescription(front), isDisplayed())).perform(replaceText("熟悉"))
            composeRule.onNodeWithText(TR.editingShowDuplicates()).assertIsDisplayed()
            captureScreen("duplicate_first_field")
        }
    }

    @Test
    fun noteTypeAndDeckSelection() {
        val deckName = "Nihongo"
        addDeck(deckName)
        val basic = col.notetypes.basic
        val cloze = col.notetypes.cloze
        withNoteEditor(addNoteIntent(basic.id)) {
            composeRule.onNodeWithText(basic.name).performClick()
            onView(withText(cloze.name)).inRoot(isDialog()).perform(click())
            val textField = onView(allOf(withContentDescription(cloze.fields.first().name), isDisplayed()))
            val extraField = onView(allOf(withContentDescription(cloze.fields.last().name), isDisplayed()))
            textField.perform(click(), composeWithIme("nihongo", "{{c1::日本語}}"))
            extraField.perform(click(), composeWithIme("hanzi", "汉字"))
            textField.check(matches(withText("{{c1::日本語}}")))
            extraField.check(matches(withText("汉字")))
            composeRule.onNodeWithText(col.decks.name(1L)).performClick()
            advanceRobolectricLooper()
            onView(withText(deckName)).inRoot(isDialog()).perform(click())
            advanceRobolectricLooper()
            composeRule.onNodeWithText(deckName).assertIsDisplayed()
            composeRule.onNode(hasText(TR.notetypesNotetype()) and hasText(cloze.name)).assertIsDisplayed()
            captureScreen("selected_note_type_and_deck")
            composeRule.onNode(hasText(targetContext.getString(CommonString.menu_add)) and hasClickAction()).performClick()
            advanceRobolectricLooper()
            textField.check(matches(withText("")))
            extraField.check(matches(withText("")))
        }
    }

    @Test
    fun draftSurvivesCancelingDiscard() {
        val basic = col.notetypes.basic
        withNoteEditor(addNoteIntent(basic.id)) {
            val front = basic.fields.first().name
            onView(allOf(withContentDescription(front), isDisplayed())).perform(replaceText("下書き"))
            composeRule.onNodeWithContentDescription(targetContext.getString(CommonString.close)).performClick()
            onView(withText(TR.addingDiscardCurrentInput())).inRoot(isDialog()).check(matches(isDisplayed()))
            val keepEditing = with(targetContext) { TR.sentenceCase.keepEditing }
            onView(withText(keepEditing)).inRoot(isDialog()).perform(click())
            onView(allOf(withContentDescription(front), isDisplayed())).check(matches(withText("下書き")))
            captureScreen("kept_draft")
        }
    }

    @Test
    fun editExistingNote() {
        val card = addBasicNote("熟悉", "familiar with").firstCard()
        val destination = NoteEditorDestination.EditSelection(listOf(card.id), TransitionDirection.DEFAULT)
        withNoteEditor(destination.toIntent(targetContext)) {
            val basic = col.notetypes.basic
            val template = basic.templates.first().name
            composeRule.onNodeWithText(template).assertIsDisplayed()
            val back = basic.fields.last().name
            onView(allOf(withContentDescription(back), isDisplayed())).perform(replaceText("well acquainted"))
            captureScreen("edit")
            composeRule.onNodeWithText(targetContext.getString(CommonString.save)).performClick()
            advanceRobolectricLooper()
        }
        withNoteEditor(destination.toIntent(targetContext)) {
            val back =
                col.notetypes.basic.fields
                    .last()
                    .name
            onView(allOf(withContentDescription(back), isDisplayed())).check(matches(withText("well acquainted")))
        }
    }

    @Test
    fun sharedTextArrivesInTheFields() {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "日本語")
                putExtra(Intent.EXTRA_TEXT, "日本語の例")
            }
        withNoteEditor(NoteEditorDestination.PassArguments.from(intent, intent.extras!!).toIntent(targetContext)) {
            val basic = col.notetypes.basic
            val front = basic.fields.first().name
            val back = basic.fields.last().name
            onView(allOf(withContentDescription(front), isDisplayed())).check(matches(withText("日本語")))
            onView(allOf(withContentDescription(back), isDisplayed())).check(matches(withText("日本語の例")))
            captureScreen("shared_text")
        }
    }

    /** 3-button navigation: a tall bottom inset which the formatting toolbar rests above */
    @Test
    fun noteEditorPortrait() =
        withNoteEditor { activity ->
            activity.simulateNavigationBar()
            composeRule.onNode(hasText(targetContext.getString(CommonString.menu_add)) and hasClickAction()).assertIsDisplayed()
            composeRule.onNodeWithText(col.decks.name(1L)).assertIsDisplayed()
            captureScreen("portrait")
        }

    /** Gesture navigation: a short inset, but larger rounded corners to clear */
    @Test
    fun noteEditorGestureNavigation() =
        withNoteEditor { activity ->
            activity.simulateGestureNavigationBar()
            captureScreen("gesture_navigation")
        }

    /** As [noteEditorPortrait], but landscape: a side navigation bar and an opposite cutout */
    @Test
    fun noteEditorLandscape() {
        RuntimeEnvironment.setQualifiers("+land")
        withNoteEditor { activity ->
            activity.simulateSideNavigationBar()
            captureScreen("landscape")
        }
    }

    @Test
    fun `gesture navigation corners do not leak into landscape insets`() {
        RuntimeEnvironment.setQualifiers("+land")
        withNoteEditor { activity ->
            // WindowInsets.Builder shares rounded corners: model a gesture test running first.
            activity.simulateGestureNavigationBar()
            activity.simulateSideNavigationBar()

            val toolbar = activity.noteEditorFragment.binding.editorToolbar.scrollView
            assertThat(toolbar.paddingLeft, equalTo(0))
            assertThat(toolbar.paddingRight, equalTo(0))
        }
    }

    /** The two-pane tablet layout: the editor beside the card previewer */
    @Test
    fun noteEditorTablet() {
        setTabletQualifiers()
        withNoteEditor { activity ->
            activity.simulateNavigationBar()
            captureScreen("tablet")
        }
    }

    /** The formatting toolbar rides above the keyboard */
    @Test
    fun noteEditorKeyboard() =
        withNoteEditor { activity ->
            activity.simulateKeyboard()
            composeRule.onNode(hasText(targetContext.getString(CommonString.menu_add)) and hasClickAction()).assertIsDisplayed()
            captureScreen("keyboard")
        }

    /** As [noteEditorKeyboard], but two-pane: 'Show answer' also stays above the keyboard */
    @Test
    fun noteEditorTabletKeyboard() {
        setTabletQualifiers()
        withNoteEditor { activity ->
            activity.dispatchInsets(navBarBottom = 48.dp, imeBottom = 300.dp)
            captureScreen("tablet_keyboard")
        }
    }

    @Test
    fun `tablet preview tabs are ready before capture`() {
        setTabletQualifiers()
        withNoteEditor { activity ->
            val tabs = activity.binding.previewerTabLayout!!
            assertThat(tabs.tabCount, equalTo(1))
            val name =
                col.notetypes.basic.templates
                    .first()
                    .name
            assertThat(
                tabs.getTabAt(0)?.text.toString(),
                equalTo(activity.getString(CommonString.card_previewer_empty_front_indicator, name)),
            )
            assertThat(tabs.selectedTabPosition, equalTo(0))
        }
    }

    private fun composeWithIme(
        composing: String,
        committed: String,
    ): ViewAction =
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isAssignableFrom(EditText::class.java))

            override fun getDescription() = "Compose and commit text through the Android input connection"

            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                val connection = checkNotNull((view as EditText).onCreateInputConnection(EditorInfo()))
                check(connection.setComposingText(composing, 1))
                check(connection.commitText(committed, 1))
                uiController.loopMainThreadUntilIdle()
            }
        }

    private fun addNoteIntent(noteTypeId: Long) =
        NoteEditorDestination
            .AddNote()
            .toIntent(targetContext)
            .putExtra(NoteEditorFragment.EXTRA_NOTE_TYPE_ID, noteTypeId)

    private fun withNoteEditor(
        intent: Intent = NoteEditorDestination.AddNote().toIntent(targetContext),
        block: (NoteEditorActivity) -> Unit,
    ) {
        val activity =
            startActivityNormallyOpenCollectionWithIntent(
                NoteEditorActivity::class.java,
                intent,
            )
        advanceRobolectricLooper()
        activity.waitForPreviewTabs()
        block(activity)
    }

    private fun NoteEditorActivity.waitForPreviewTabs() {
        if (!isPreviewerVisible) return
        val tabs = binding.previewerTabLayout!!
        // Wait for the background card rendering to complete.
        advanceRobolectricLooperUntil(
            lazyMessage = { "Note editor preview tabs did not finish loading" },
        ) {
            tabs.tabCount > 0 && tabs.selectedTabPosition == 0
        }
    }

    private fun NoteEditorActivity.simulateNavigationBar() {
        val navBarHeight = 48.dp
        val insets =
            with(targetContext) {
                WindowInsetsCompat
                    .Builder()
                    .setInsets(statusBars(), insetsOf(top = 24.dp))
                    .setInsets(navigationBars(), insetsOf(bottom = navBarHeight))
                    .build()
            }
        ViewCompat.dispatchApplyWindowInsets(window.decorView, insets)
        addOverlay(FrameLayout.LayoutParams.MATCH_PARENT, navBarHeight.toPx(targetContext), Gravity.BOTTOM)
    }

    /** As [simulateNavigationBar], but gesture navigation: a short inset, larger rounded corners */
    private fun NoteEditorActivity.simulateGestureNavigationBar() {
        val navBarHeight = 24.dp
        val insets =
            with(targetContext) {
                WindowInsetsCompat
                    .Builder()
                    .setInsets(statusBars(), insetsOf(top = 24.dp))
                    .setInsets(navigationBars(), insetsOf(bottom = navBarHeight))
                    .apply {
                        val radius = 34.dp.toPx(targetContext)
                        setRoundedCorner(
                            RoundedCornerCompat.POSITION_BOTTOM_LEFT,
                            RoundedCornerCompat(RoundedCornerCompat.POSITION_BOTTOM_LEFT, radius, radius, radius),
                        )
                    }.build()
            }
        ViewCompat.dispatchApplyWindowInsets(window.decorView, insets)
        addOverlay(FrameLayout.LayoutParams.MATCH_PARENT, navBarHeight.toPx(targetContext), Gravity.BOTTOM)
    }

    /** As [simulateNavigationBar], but landscape: a side navigation bar and an opposite cutout */
    private fun NoteEditorActivity.simulateSideNavigationBar() {
        val navBarWidth = 48.dp
        // The shared helper also resets rounded corners leaked by other tests.
        dispatchInsets(navBarRight = navBarWidth, cutoutLeft = 32.dp)
        addOverlay(navBarWidth.toPx(targetContext), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END)
    }

    private fun NoteEditorActivity.addOverlay(
        width: Int,
        height: Int,
        gravity: Int,
    ) {
        val decor = window.decorView as ViewGroup
        val overlay = View(this).apply { setBackgroundColor(0x80000000.toInt()) }
        decor.addView(overlay, FrameLayout.LayoutParams(width, height, gravity))
    }
}
