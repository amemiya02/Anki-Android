// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import android.Manifest.permission.RECORD_AUDIO
import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.multimediacard.fields.AudioRecordingField
import com.ichi2.anki.multimediacard.fields.TextField
import com.ichi2.anki.multimediacard.impl.MultimediaEditableNote
import com.ichi2.anki.recorder.MediaRecorderStub
import com.ichi2.anki.recorder.RecorderState
import com.ichi2.anki.ui.windows.reviewer.audiorecord.CheckPronunciationViewModel
import com.ichi2.testutils.grantPermissions
import io.mockk.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowToast
import kotlin.test.assertIs

/** Tests for [AudioRecordingFragment]. */
@RunWith(AndroidJUnit4::class)
class AudioRecordingFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val mediaRecorderStub = MediaRecorderStub()

    @Before
    fun installRecorder() {
        mediaRecorderStub.install()
    }

    @After
    fun resetRecorder() {
        mediaRecorderStub.uninstall()
    }

    @Test
    fun `done button is disabled when no media path is set`() =
        withAudioRecordingFragment {
            done.assertIsNotEnabled()
        }

    @Test
    fun `done button is enabled once a recording is saved`() =
        withAudioRecordingFragment {
            record()

            done.assertIsEnabled()
        }

    @Test
    fun `holding the record button saves on release`() {
        val controller = startRecorder()
        compose.onNodeWithContentDescription(getResourceString(CommonString.record_voice)).performTouchInput { longClick() }
        advanceRobolectricLooper()
        done.assertIsEnabled()
        val result = controller.finishWithDone()
        assertThat(result.field.mediaFile, equalTo(mediaRecorderStub.outputFiles.single()))
    }

    @Test
    fun `the recorder shows the note fields and record action`() {
        startRecorder()
        compose.onNodeWithText("Front of the card").assertIsDisplayed()
        compose.onNodeWithContentDescription(getResourceString(CommonString.record_voice)).assertIsDisplayed()
    }

    @Test
    fun `a recording can be added after the reviewer's recorder was used`() {
        val reviewer = CheckPronunciationViewModel()
        reviewer.onRecordingStarted()
        reviewer.onRecordingCompleted()
        val reviewRecording = assertIs<RecorderState.Recorded>(reviewer.recordingState.value).file
        val controller = startRecorder()
        record()

        done.assertIsEnabled()
        val result = controller.finishWithDone()
        val recording = result.field.mediaFile!!
        reviewer.resetAll()
        assertThat("the review recording was discarded", reviewRecording.exists(), equalTo(false))
        assertThat("the recording exists", recording.exists(), equalTo(true))
        assertThat("the recording is not empty", recording.length(), greaterThan(0L))
    }

    @Test
    fun `a recording that fails to stop cannot be added and shows an error`() {
        mediaRecorderStub.failStop = true
        startRecorder()

        record()

        done.assertIsNotEnabled()
        assertThat(
            "error message",
            ShadowToast.getTextOfLatestToast(),
            equalTo(targetContext.getString(CommonString.multimedia_editor_audio_view_recording_failed)),
        )
    }

    @Test
    fun `a saved recording can still be played after the screen is recreated`() {
        val controller = startRecorder()
        record()

        controller.recreate()
        advanceRobolectricLooper()

        compose.onNodeWithContentDescription(getResourceString(CommonString.play_recording)).assertIsDisplayed()
        done.assertIsEnabled()
    }

    @Test
    fun `discarding a saved recording deletes it`() {
        startRecorder()
        record()
        val recording = mediaRecorderStub.outputFiles.single()

        click(CommonString.discard)

        assertThat("the discarded recording is deleted", recording.exists(), equalTo(false))
    }

    @Test
    fun `a recording continues when the screen is recreated`() {
        val controller = startRecorder()
        pressRecord()

        controller.recreate()
        advanceRobolectricLooper()

        verify(exactly = 0) { mediaRecorderStub.mediaRecorder.stop() }
        click(CommonString.save_voice)
        val result = controller.finishWithDone()
        val recording = mediaRecorderStub.outputFiles.single()
        assertThat("the recording started before recreation is added", result.field.mediaFile, equalTo(recording))
    }

    @Test
    fun `leaving the screen while recording keeps the recording`() {
        val controller = startRecorder()
        pressRecord()

        controller.pause().stop()
        verify { mediaRecorderStub.mediaRecorder.stop() }
        controller.start().resume()
        advanceRobolectricLooper()

        compose.onNodeWithContentDescription(getResourceString(CommonString.play_recording)).assertIsDisplayed()
        done.assertIsEnabled()
    }

    @Test
    fun `the screen can turn off once leaving the screen ends a recording`() {
        val controller = startRecorder()
        pressRecord()
        assertThat("the screen stays on while recording", controller.fragment.keepsScreenOn, equalTo(true))

        controller.pause().stop()

        assertThat("the screen can turn off", controller.fragment.keepsScreenOn, equalTo(false))
    }

    @Test
    fun `the screen can turn off while a recording is paused`() {
        val fragment = startRecorder().fragment
        pressRecord()

        pressRecord()

        verify { mediaRecorderStub.mediaRecorder.pause() }
        assertThat("the screen can turn off", fragment.keepsScreenOn, equalTo(false))
    }

    private fun withAudioRecordingFragment(block: (AudioRecordingFragment) -> Unit) {
        block(startRecorder().fragment)
    }

    private fun startRecorder(): ActivityController<MultimediaActivity> {
        grantPermissions(RECORD_AUDIO)
        val note =
            MultimediaEditableNote().apply {
                setNumFields(1)
                setField(0, TextField().apply { text = "Front of the card" })
                freezeInitialFieldValues()
            }
        val extra = MultimediaActivityExtra(index = 0, field = AudioRecordingField(), note = note)
        return startActivityControllerNormallyOpenCollectionWithIntent(
            MultimediaActivity::class.java,
            AudioRecordingFragment.getIntent(targetContext, extra),
        )
    }

    private val ActivityController<MultimediaActivity>.fragment: AudioRecordingFragment
        get() = get().fragment!!.childFragmentManager.findFragmentById(R.id.multimedia_editor_container) as AudioRecordingFragment

    private fun ActivityController<MultimediaActivity>.finishWithDone(): MultimediaResult.Success {
        done.performClick()
        val shadow = shadowOf(get())
        assertThat("result code", shadow.resultCode, equalTo(Activity.RESULT_OK))
        return assertIs<MultimediaResult.Success>(MultimediaResultContract().parseResult(shadow.resultCode, shadow.resultIntent))
    }

    private fun record() {
        pressRecord()
        click(CommonString.save_voice)
    }

    private fun pressRecord() {
        val control =
            hasContentDescription(getResourceString(CommonString.record_voice)) or
                hasContentDescription(getResourceString(R.string.note_editor_audio_pause_recording)) or
                hasContentDescription(getResourceString(R.string.note_editor_audio_resume_recording))
        compose.onNode(control).performTouchInput { click() }
        advanceRobolectricLooper()
    }

    private fun click(
        @StringRes label: Int,
    ) {
        compose.onNodeWithContentDescription(getResourceString(label)).performClick()
        advanceRobolectricLooper()
    }

    private val done get() = compose.onNodeWithText(getResourceString(CommonString.multimedia_editor_field_editing_done))

    private val AudioRecordingFragment.keepsScreenOn: Boolean
        get() = requireView().keepScreenOn
}
