// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.ComposeScreenshotTest
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import org.junit.Test
import androidx.media3.ui.R as MediaR

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class AttachmentPlaybackScreenshotTest : ComposeScreenshotTest() {
    @Test
    fun playbackAndSaveRemainReachable() =
        capture(
            "playback",
            beforeCapture = {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                composeRule.onNodeWithContentDescription(context.getString(CommonString.play_recording)).performClick()
                composeRule.onNodeWithContentDescription(context.getString(R.string.note_editor_audio_pause_playback)).assertIsDisplayed()
                composeRule.onNodeWithText(context.getString(CommonString.multimedia_editor_field_editing_done)).assertIsDisplayed()
                val settings = composeRule.onNodeWithContentDescription(context.getString(MediaR.string.exo_controls_settings_description))
                val speed = composeRule.onNodeWithText(context.getString(MediaR.string.exo_controls_playback_speed))
                val faster = context.resources.getStringArray(MediaR.array.exo_controls_playback_speeds)[4]
                settings.performClick()
                speed.performClick()
                composeRule.onNodeWithText(faster).performClick()
                settings.performClick()
                composeRule.onNodeWithText(faster).assertIsDisplayed()
                composeRule.onNodeWithText(context.getString(CommonString.dialog_cancel)).performClick()
            },
        ) {
            var isPlaying by remember { mutableStateOf(false) }
            var speed by remember { mutableStateOf(1f) }
            MediaPreview(
                fileSize = "256 kB",
                preview = { Box(Modifier.fillMaxSize()) },
            ) {
                AttachmentPlaybackControls(
                    AttachmentPlaybackState(position = 4_000, duration = 18_000, playing = isPlaying, speed = speed),
                    onPlayPause = { isPlaying = !isPlaying },
                    onBack = {},
                    onForward = {},
                    onSeek = {},
                    onSpeed = { speed = it },
                    onAudioTrack = {},
                )
                MediaDoneButton(enabled = true, onClick = {})
            }
        }
}
