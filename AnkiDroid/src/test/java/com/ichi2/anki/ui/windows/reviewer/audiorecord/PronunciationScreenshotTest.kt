// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer.audiorecord

import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.ComposeScreenshotTest
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Test

@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
@CheckAccessibility
class PronunciationScreenshotTest : ComposeScreenshotTest() {
    @Test fun idle() = captureRecorder("idle", false, false)

    @Test fun recording() = captureRecorder("recording", true, false)

    @Test fun playback() = captureRecorder("playback", false, true)

    private fun captureRecorder(
        name: String,
        recording: Boolean,
        playing: Boolean,
    ) = capture(name) {
        PronunciationControls(
            recording,
            12,
            playing,
            playing,
            .4f,
            0,
            onStart = { true },
            onFinish = {},
            onCancelRecording = {},
            onPlay = {},
            onClear = {},
        )
    }
}
