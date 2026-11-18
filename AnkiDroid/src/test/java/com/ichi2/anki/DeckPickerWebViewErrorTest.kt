// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.webkit.CookieManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
@Config(application = DeckPickerWebViewErrorTest.MissingWebViewApplication::class, qualifiers = "normal")
class DeckPickerWebViewErrorTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @After
    fun resetWebViewMock() {
        unmockkStatic(CookieManager::class)
    }

    @Test
    fun `app opens DeckPicker with an error dialog when WebView is unavailable`() {
        ActivityScenario.launch(DeckPicker::class.java).use { scenario ->
            advanceRobolectricLooperUntil {
                ShadowDialog.getLatestDialog()?.isShowing == true
            }
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }
            compose.onNodeWithText(targetContext.getString(CommonString.ankidroid_init_failed_webview_title)).assertIsDisplayed()
            compose.onNodeWithText("WebView unavailable", substring = true).assertIsDisplayed()
        }
    }

    class MissingWebViewApplication : AnkiDroidApp() {
        override fun onCreate() {
            mockkStatic(CookieManager::class)
            every { CookieManager.getInstance() } throws IllegalStateException("WebView unavailable")
            super.onCreate()
        }
    }
}
