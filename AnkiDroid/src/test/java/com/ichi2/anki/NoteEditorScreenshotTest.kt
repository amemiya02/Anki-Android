// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.Manifest.permission.RECORD_AUDIO
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.provider.MediaStore
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.core.view.RoundedCornerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import anki.notetypes.StockNotetype
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.ui.TransitionDirection
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.libanki.getStockNotetype
import com.ichi2.anki.multimedia.MultimediaActivity
import com.ichi2.anki.noteeditor.CustomToolbarButton
import com.ichi2.anki.noteeditor.label
import com.ichi2.anki.noteeditor.presentation.ToolbarLayoutMode
import com.ichi2.anki.noteeditor.toIntent
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.ext.reopenWithLanguage
import com.ichi2.testutils.grantPermissions
import com.ichi2.testutils.insetsOf
import com.ichi2.testutils.simulateKeyboard
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowMediaRecorder
import java.io.File
import androidx.appcompat.R as AppCompatR
import com.ichi2.testutils.Robolectric as RobolectricActivities

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

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    override fun applyDeviceConfig() {
        super.applyDeviceConfig()
        runTest { col.reopenWithLanguage(language.option) }
    }

    @Test
    fun attachSheetNamesTheSelectedField() {
        val camera = ComponentName("com.android.camera2", "com.android.camera.CaptureActivity")
        shadowOf(targetContext.packageManager).apply {
            addActivityIfNotPresent(camera)
            addIntentFilterForActivity(camera, IntentFilter(MediaStore.ACTION_IMAGE_CAPTURE).apply { addCategory(Intent.CATEGORY_DEFAULT) })
        }
        val basic = basicNoteType
        withNoteEditor(addNoteIntent(basic.id)) {
            val field = basic.fields.last().name
            val front = basic.fields.first().name
            onView(allOf(withContentDescription(front), isDisplayed())).perform(replaceText("熟悉"))
            onView(allOf(withContentDescription(field), isDisplayed())).perform(click(), replaceText("familiar with"))
            composeRule
                .onNodeWithContentDescription(targetContext.getString(CommonString.multimedia_editor_attach_mm_content, field))
                .performClick()
            composeRule.onNodeWithText(targetContext.getString(R.string.note_editor_attach_to_field, field)).assertIsDisplayed()
            listOf(
                R.string.note_editor_attach_image,
                R.string.note_editor_attach_camera,
                CommonString.drawing,
                CommonString.multimedia_editor_popup_audio,
                R.string.note_editor_attach_audio_file,
                R.string.note_editor_attach_video_file,
            ).forEach { label -> composeRule.onNodeWithText(targetContext.getString(label)).assertIsDisplayed() }
            captureScreen("attach_to_field")
        }
    }

    @Test
    fun attachmentChoicesHideCameraWithoutACameraApp() =
        withNoteEditor(addNoteIntent(basicNoteType.id)) {
            val field = basicNoteType.fields.first().name
            composeRule
                .onNodeWithContentDescription(targetContext.getString(CommonString.multimedia_editor_attach_mm_content, field))
                .performClick()
            composeRule.onNodeWithText(targetContext.getString(R.string.note_editor_attach_image)).assertIsDisplayed()
            composeRule.onNodeWithText(targetContext.getString(R.string.note_editor_attach_camera)).assertDoesNotExist()
        }

    @Test
    @Config(shadows = [RecordingMicrophone::class])
    fun recordedAudioReturnsToTheSelectedField() {
        grantPermissions(RECORD_AUDIO)
        val basic = basicNoteType
        withNoteEditor(addNoteIntent(basic.id)) { activity ->
            val front = onView(allOf(withContentDescription(basic.fields.first().name), isDisplayed()))
            val back = basic.fields.last().name
            front.perform(replaceText("声"))
            composeRule
                .onNodeWithContentDescription(targetContext.getString(CommonString.multimedia_editor_attach_mm_content, back))
                .performClick()
            composeRule.onNodeWithText(targetContext.getString(CommonString.multimedia_editor_popup_audio)).performClick()
            advanceRobolectricLooper()

            val editor = shadowOf(activity)
            val request = editor.nextStartedActivityForResult.intent
            val recorder = startActivityControllerNormallyOpenCollectionWithIntent(MultimediaActivity::class.java, request)
            advanceRobolectricLooper()
            composeRule
                .onNodeWithText(targetContext.getString(CommonString.multimedia_editor_field_editing_audio))
                .assertIsDisplayed()
            captureScreen("attachment_recorder")
            onView(withContentDescription(targetContext.getString(CommonString.record_voice))).perform(click())
            onView(withContentDescription(targetContext.getString(CommonString.save_voice))).perform(click())
            onView(withText(targetContext.getString(CommonString.multimedia_editor_field_editing_done))).perform(click())
            advanceRobolectricLooper()
            val result = shadowOf(recorder.get())
            val resultCode = result.resultCode
            val resultIntent = result.resultIntent
            RobolectricActivities.closeActivity(recorder)
            editor.receiveResult(request, resultCode, resultIntent)
            advanceRobolectricLooper()

            onView(allOf(withContentDescription(back), isDisplayed())).check(matches(withText(startsWith("[sound:"))))
            front.check(matches(withText("声")))
        }
    }

    @Implements(MediaRecorder::class)
    class RecordingMicrophone : ShadowMediaRecorder() {
        @Implementation
        override fun stop() {
            super.stop()
            File(outputPath).writeBytes(ByteArray(256))
        }

        @Implementation
        fun getMaxAmplitude() = 0
    }

    @Test
    fun clozeCommandsLeadTheFormattingToolbar() =
        withNoteEditor(addNoteIntent(clozeNoteType.id)) { activity ->
            val cloze = composeRule.onNodeWithContentDescription(TR.editingClozeDeletion())
            val repeat = composeRule.onNodeWithContentDescription(TR.editingClozeDeletionRepeat())
            val bold = composeRule.onNodeWithContentDescription(targetContext.getString(CommonString.format_insert_bold))
            fun leadingX(node: SemanticsNodeInteraction): Float {
                val bounds = node.assertIsDisplayed().getUnclippedBoundsInRoot()
                return if (language.isRightToLeft) -bounds.right.value else bounds.left.value
            }
            check(leadingX(cloze) < leadingX(repeat) && leadingX(repeat) < leadingX(bold)) {
                "The cloze controls must be at the leading end of the toolbar"
            }

            val name = clozeNoteType.fields.first().name
            val field = onView(allOf(withContentDescription(name), isDisplayed(), isAssignableFrom(EditText::class.java)))
            field.perform(click(), replaceText("熟悉"), selectRange(0, 2))
            cloze.performClick()
            field.check(matches(withText("{{c1::熟悉}}")))
            field.perform(replaceText("{{c1::熟悉}} 日本語"), selectRange(11, 14))
            repeat.performClick()
            field.check(matches(withText("{{c1::熟悉}} {{c1::日本語}}")))
            activity.simulateKeyboard()
            captureScreen("cloze_formatting")
        }

    @Test
    fun formattingCommandsAndLayoutFollowEditorSettings() {
        val customLabel = "BR"
        getPreferences().edit {
            putStringSet(
                "note_editor_custom_buttons",
                CustomToolbarButton.toStringSet(arrayListOf(CustomToolbarButton(0, customLabel, "", "<br>"))),
            )
            putBoolean("noteEditorScrollToolbar", false)
        }
        withNoteEditor(addNoteIntent(basicNoteType.id)) {
            val field = onView(allOf(withContentDescription(basicNoteType.fields.first().name), isDisplayed()))
            val bold = composeRule.onNodeWithContentDescription(targetContext.getString(CommonString.format_insert_bold))
            val custom = composeRule.onNodeWithContentDescription(customLabel)
            field.perform(click(), replaceText("熟悉"), selectRange(0, 2))
            bold.assertIsDisplayed().performClick()
            field.check(matches(withText("<b>熟悉</b>")))
            field.perform(pressKey(KeyEvent.KEYCODE_MOVE_END))
            custom.assertIsDisplayed().performClick()
            field.check(matches(withText("<b>熟悉</b><br>")))
            captureScreen("stacked_formatting")

            chooseToolbarLayout(ToolbarLayoutMode.HIDDEN)
            bold.assertDoesNotExist()
            custom.assertDoesNotExist()
            chooseToolbarLayout(ToolbarLayoutMode.SCROLL)
            bold.assertIsDisplayed()
            captureScreen("scrolling_formatting")
        }
    }

    @Test
    fun tagsCanBeAddedAndRemovedInTheEditor() {
        val tag = "vocab"
        withNoteEditor(addNoteIntent(basicNoteType.id)) {
            val tags = composeRule.onNodeWithText(TR.browsingSidebarTags())
            val confirm = onView(withText(targetContext.getString(CommonString.dialog_confirm))).inRoot(isDialog())
            tags.performClick()
            advanceRobolectricLooper()
            onView(withContentDescription(targetContext.getString(CommonString.add_tag))).inRoot(isDialog()).perform(click())
            onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog()).perform(replaceText(tag))
            advanceRobolectricLooper()
            onView(withText(targetContext.getString(CommonString.menu_add))).inRoot(isDialog()).perform(click())
            advanceRobolectricLooper()
            confirm.perform(click())
            composeRule.onNodeWithText(tag).assertIsDisplayed()
            captureScreen("note_with_tag")

            tags.performClick()
            advanceRobolectricLooper()
            onView(withText(tag)).inRoot(isDialog()).perform(click())
            confirm.perform(click())
            composeRule.onNodeWithText(tag).assertDoesNotExist()
        }
    }

    private fun chooseToolbarLayout(layout: ToolbarLayoutMode) {
        composeRule
            .onNodeWithContentDescription(targetContext.getString(AppCompatR.string.abc_action_menu_overflow_description))
            .performClick()
        composeRule.onNodeWithText(targetContext.getString(CommonString.note_editor_toolbar_layout)).performClick()
        composeRule.onNodeWithText(targetContext.getString(layout.label())).performClick()
    }

    private fun selectRange(
        start: Int,
        end: Int,
    ): ViewAction =
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isAssignableFrom(EditText::class.java))

            override fun getDescription() = "Select field text"

            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                (view as EditText).setSelection(start, end)
                uiController.loopMainThreadUntilIdle()
            }
        }

    @Test
    fun duplicateFirstField() {
        addNoteUsingNoteTypeName(basicNoteType.name, "熟悉", "familiar with")
        withNoteEditor {
            val front =
                basicNoteType.fields
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
        val basic = basicNoteType
        val cloze = clozeNoteType
        withNoteEditor(addNoteIntent(basic.id)) {
            composeRule.onNodeWithText(basic.name).performClick()
            onData(equalTo(cloze.name)).inRoot(isDialog()).perform(click())
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
        val basic = basicNoteType
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
        val card = addNoteUsingNoteTypeName(basicNoteType.name, "熟悉", "familiar with").firstCard()
        val destination = NoteEditorDestination.EditSelection(listOf(card.id), TransitionDirection.DEFAULT)
        withNoteEditor(destination.toIntent(targetContext)) {
            val basic = basicNoteType
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
                basicNoteType.fields
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
            val basic = basicNoteType
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
                basicNoteType.templates
                    .first()
                    .name
            assertThat(
                tabs.getTabAt(0)?.text.toString(),
                equalTo(activity.getString(CommonString.card_previewer_empty_front_indicator, name)),
            )
            assertThat(tabs.selectedTabPosition, equalTo(0))
        }
    }

    private val basicNoteType
        get() = noteType(StockNotetype.Kind.KIND_BASIC)

    private val clozeNoteType
        get() = noteType(StockNotetype.Kind.KIND_CLOZE)

    private fun noteType(kind: StockNotetype.Kind) =
        requireNotNull(col.notetypes.byName(col.getStockNotetype(kind).name))

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
