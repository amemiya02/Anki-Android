// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: 2026 Alok Srivastava <alok020505@gmail.com>

package com.ichi2.anki.ui.windows.reviewer.audiorecord

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.compose.theme.AnkiDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PronunciationControlsTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val recording = mutableStateOf(false)
    private val playVisible = mutableStateOf(false)
    private val playing = mutableStateOf(false)
    private val actions = mutableListOf<String>()

    private fun button(label: Int) = compose.onNodeWithContentDescription(context.getString(label))

    private fun show() {
        compose.setContent {
            AnkiDroidTheme {
                PronunciationControls(
                    recording = recording.value,
                    elapsedSeconds = 0,
                    playbackVisible = playVisible.value,
                    playing = playing.value,
                    progress = 0f,
                    replayCount = 0,
                    onStart = {
                        actions += "record"
                        recording.value = true
                        true
                    },
                    onFinish = {
                        actions += "finish"
                        recording.value = false
                        playVisible.value = true
                    },
                    onCancelRecording = {
                        actions += "discard"
                        recording.value = false
                    },
                    onPlay = {
                        actions += "play"
                        playing.value = true
                    },
                    onClear = {
                        actions += "clear"
                        playVisible.value = false
                    },
                )
            }
        }
    }

    @Test
    fun accessibleControlsRecordPlayReplayAndClear() {
        show()
        button(CommonString.record_voice).performClick()
        button(CommonString.stop_recording).performClick()
        button(CommonString.play_recording).performClick()
        button(CommonString.replay_voice).assertIsDisplayed().performClick()
        button(CommonString.dialog_cancel).performClick()
        assertEquals(listOf("record", "finish", "play", "play", "clear"), actions)
    }

    @Test
    fun holdingAndReleasingFinishesTheTake() {
        show()
        button(CommonString.record_voice).performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(0f, 1f))
            up()
        }
        button(CommonString.play_recording).assertIsDisplayed()
        assertEquals(listOf("record", "finish"), actions)
    }

    @Test
    fun slidingLeftDiscardsAndSlidingUpLocks() {
        show()
        button(CommonString.record_voice).performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(-1000f, 0f))
            up()
        }
        button(CommonString.record_voice).assertIsDisplayed().performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(0f, -1800f))
            up()
        }
        button(CommonString.stop_recording).assertIsDisplayed().performClick()
        assertEquals(listOf("record", "discard", "record", "finish"), actions)
    }

    @Test
    fun tappingLocksTheRecordingUntilStopped() {
        show()
        button(CommonString.record_voice).performTouchInput {
            down(center)
            up()
        }
        button(CommonString.stop_recording).assertIsDisplayed().performClick()
        assertEquals(listOf("record", "finish"), actions)
    }
}
