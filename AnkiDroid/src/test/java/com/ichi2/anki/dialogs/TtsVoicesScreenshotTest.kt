// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.speech.tts.Voice
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.AndroidTtsVoice
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.ComposeScreenshotTest
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.dialogs.viewmodel.TtsVoicesViewModel.VoiceLoadingState
import org.junit.Test
import java.util.Locale

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class TtsVoicesScreenshotTest : ComposeScreenshotTest() {
    @Test
    fun selectableVoices() =
        capture(
            "voices",
            beforeCapture = { composeRule.onNodeWithText("en-us-voice").assertIsDisplayed() },
        ) {
            PreviewVoices()
        }

    @Test
    fun installFilterDisablesInternetFilter() =
        capture(
            "install",
            beforeCapture = {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                composeRule.onNodeWithText(context.getString(CommonString.tts_voices_chip_filter_to_installable_audio)).performClick()
                composeRule.onNodeWithText(context.getString(CommonString.tts_voices_chip_filter_to_internet)).assertIsNotEnabled()
                composeRule.onNodeWithText("ja-jp-voice").assertIsDisplayed()
            },
        ) {
            PreviewVoices()
        }

    @androidx.compose.runtime.Composable
    private fun PreviewVoices() {
        val sample = stringResource(CommonString.tts_voices_default_text)
        var text by remember { mutableStateOf(sample) }
        var showInternet by remember { mutableStateOf(false) }
        var showUninstalled by remember { mutableStateOf(false) }
        val voice = if (showUninstalled) voice("ja-jp-voice", Locale.JAPAN, setOf("notInstalled")) else voice("en-us-voice", Locale.US)
        TtsVoicesScreen(
            TtsVoicesScreenState(VoiceLoadingState.Success(listOf(voice)), text, showInternet, showUninstalled),
            onBack = {},
            onSettings = {},
            onText = { text = it },
            onShowInternet = { showInternet = it },
            onShowUninstalled = { showUninstalled = it },
            onPlay = {},
            onCopy = {},
            snackbar = remember { SnackbarHostState() },
        )
    }

    private fun voice(
        name: String,
        locale: Locale,
        features: Set<String> = emptySet(),
    ) = AndroidTtsVoice(Voice(name, locale, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, features), "engine")
}
