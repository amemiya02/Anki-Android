// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.speech.tts.TextToSpeech
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.testutils.EmptyApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
@Config(application = EmptyApplication::class)
class TtsPlaybackErrorDialogTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `no engines explains how to install one instead of offering voice options`() {
        buildActivity(FragmentActivity::class.java).use { controller ->
            controller.get().setTheme(CommonR.style.Theme_Light)
            val activity = controller.setup().get()
            TtsPlaybackErrorDialog.ttsPlaybackErrorDialog(activity, activity.supportFragmentManager, null)
            composeRule.onNodeWithText("No text-to-speech engine installed").assertIsDisplayed()
            composeRule.onNodeWithText(activity.getString(CommonString.tts_no_engine_message)).assertIsDisplayed()
            composeRule.onNodeWithText(activity.getString(CommonString.dialog_ok)).assertIsDisplayed()
            composeRule.onNodeWithText(activity.getString(CommonString.tts_error_dialog_change_button_text)).assertDoesNotExist()
            composeRule.onNodeWithText(activity.getString(CommonString.help)).assertIsDisplayed()
        }
    }

    @Test
    fun `an installed engine still offers language troubleshooting`() {
        buildActivity(FragmentActivity::class.java).use { controller ->
            controller.get().setTheme(CommonR.style.Theme_Light)
            val activity = controller.setup().get()
            shadowOf(activity.packageManager).apply {
                val engine = ComponentName("test.tts", "TestTtsService")
                addServiceIfNotPresent(engine)
                addIntentFilterForService(
                    engine,
                    IntentFilter(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).apply { addCategory(Intent.CATEGORY_DEFAULT) },
                )
            }
            TtsPlaybackErrorDialog.ttsPlaybackErrorDialog(activity, activity.supportFragmentManager, null)
            composeRule.onNodeWithText(activity.getString(CommonString.tts_error_dialog_title)).assertIsDisplayed()
            composeRule.onNodeWithText(activity.getString(CommonString.tts_error_dialog_change_button_text)).assertIsDisplayed()
            composeRule.onNodeWithText(activity.getString(CommonString.tts_error_dialog_supported_voices_button_text)).assertIsDisplayed()
        }
    }
}
