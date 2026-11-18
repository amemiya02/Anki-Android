// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.testutils.ext.reopenWithLanguage
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class StudySettingsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun studyingAndToolbarFitTheViewport() {
        setPhoneQualifiers()
        runTest { col.reopenWithLanguage(language.option) }
        ActivityScenario.launch<PreferencesActivity>(PreferencesActivity.getIntent(targetContext)).use {
            compose.onNodeWithText(targetContext.getString(R.string.settings_studying)).performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(targetContext.getString(CommonString.day_offset_with_description)).assertIsDisplayed()
            captureScreen("studying")
            compose
                .onNode(
                    hasText(targetContext.getString(CommonString.toolbar_actions)) and hasClickAction(),
                ).performScrollTo()
                .performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(targetContext.getString(CommonString.custom_buttons_setting_always_show)).assertIsDisplayed()
            captureScreen("toolbar")
        }
    }

    @Test
    fun controlsTabsAndBindingEditorFitTheViewport() {
        setPhoneQualifiers()
        runTest { col.reopenWithLanguage(language.option) }
        val intent = PreferencesActivity.getIntent(targetContext, ControlsSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithText(targetContext.getString(CommonString.gestures_corner_touch)).assertIsDisplayed()
            captureScreen("controls")
            compose
                .onNodeWithText(targetContext.getString(R.string.settings_previewer))
                .performScrollTo()
                .performClick()
                .assertIsSelected()
            compose.onNodeWithText(targetContext.getString(CommonString.previewer_next)).performScrollTo().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(targetContext.getString(CommonString.binding_add_key)).performScrollTo().assertIsDisplayed()
            captureScreen("binding-editor")
        }
    }
}
