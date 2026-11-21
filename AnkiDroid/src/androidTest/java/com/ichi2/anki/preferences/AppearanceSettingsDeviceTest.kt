// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.android.systemIsInNightMode
import com.ichi2.anki.settings.PrefsRepository
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.themes.HayakiTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AppearanceSettingsDeviceTest : InstrumentedTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun followSystemRestoresBlackFromForcedLightUnderSystemNight() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("hayakiReleaseDevice") == "true")
        assertTrue(systemIsInNightMode(testContext.applicationContext), "This device profile should start in system night mode")

        val prefs = PrefsRepository(testContext)
        val original = Triple(prefs.appTheme, prefs.nightTheme, prefs.hayakiTheme)
        val intent = PreferencesActivity.getIntent(testContext, AppearanceSettingsFragment::class)
        try {
            prefs.appTheme = AppTheme.FOLLOW_SYSTEM
            prefs.nightTheme = NightTheme.BLACK
            prefs.hayakiTheme = HayakiTheme.HAYAKI

            ActivityScenario.launch<PreferencesActivity>(intent).use {
                tile(R.string.settings_theme_system).assertIsSelected()
                assertBackground(CommonR.color.foundation_black_background)

                tile(R.string.settings_theme_light).performScrollTo().performClick()
                tile(R.string.settings_theme_light).assertIsSelected()
                assertBackground(CommonR.color.foundation_light_background)
                assertTrue(systemIsInNightMode(testContext.applicationContext), "Forced Light should leave the system in night mode")

                tile(R.string.settings_theme_system).performScrollTo().performClick()
                tile(R.string.settings_theme_system).assertIsSelected()
                assertBackground(CommonR.color.foundation_black_background)
            }
            ActivityScenario.launch<PreferencesActivity>(intent).use {
                tile(R.string.settings_theme_system).assertIsSelected()
                assertBackground(CommonR.color.foundation_black_background)
            }
        } finally {
            prefs.appTheme = original.first
            prefs.nightTheme = original.second
            prefs.hayakiTheme = original.third
        }
    }

    private fun tile(label: Int) = compose.onNode(hasText(testContext.getString(label)) and isSelectable())

    private fun assertBackground(
        @ColorRes color: Int,
    ) {
        val screen = compose.onRoot().captureToImage().toPixelMap()
        // The side margin stays the page background past the controls and system bars.
        assertEquals(testContext.getColor(color), screen[2, screen.height / 2].toArgb())
    }
}
