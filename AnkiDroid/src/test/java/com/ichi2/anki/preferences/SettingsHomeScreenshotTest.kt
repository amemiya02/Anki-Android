// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.util.TypedValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.ThemeConfig
import com.ichi2.testutils.ext.reopenWithLanguage
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals
import com.ichi2.anki.common.android.R as CommonR

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class SettingsHomeScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun searchShowsSettingsWhileTypingAndOpensAppearance() {
        setPhoneQualifiers()
        openSettings().use { scenario ->
            val search = composeRule.onNodeWithContentDescription(searchPrompt)
            search.assertIsDisplayed()
            captureScreen("home")

            search.performTextInput(themeQuery)
            val result = composeRule.onNode(hasText(themeTitle) and hasClickAction() and !hasSetTextAction())
            result.assertIsDisplayed()
            captureScreen("search")
            result.performClick()
            settle()
            composeRule.onNodeWithText(targetContext.getString(R.string.settings_appearance)).assertIsDisplayed()
            val lightTheme = composeRule.onNodeWithText(targetContext.getString(R.string.settings_theme_light))
            lightTheme.assertIsDisplayed()
            captureScreen("appearance")

            // Night/Black stored before the redesign must still show as Black in the selector.
            val initialTile =
                when (theme) {
                    ThemeConfig.LIGHT -> R.string.settings_theme_light
                    ThemeConfig.DARK -> R.string.settings_theme_dark
                    ThemeConfig.BLACK -> R.string.settings_theme_black
                }
            val before = composeRule.onAllNodes(hasText(targetContext.getString(initialTile)) and hasClickAction())[0]
            before.assertIsSelected()

            val changedTheme = if (theme == ThemeConfig.BLACK) R.string.settings_theme_light else R.string.settings_theme_black
            composeRule
                .onAllNodes(hasText(targetContext.getString(changedTheme)) and hasClickAction())[0]
                .performScrollTo()
                .performClick()
            settle()
            composeRule.onAllNodes(hasText(targetContext.getString(changedTheme)) and hasClickAction())[0].assertIsSelected()
            val expected =
                if (theme ==
                    ThemeConfig.BLACK
                ) {
                    CommonR.color.foundation_light_background
                } else {
                    CommonR.color.foundation_black_background
                }
            scenario.onActivity { activity ->
                val value = TypedValue()
                activity.theme.resolveAttribute(CommonR.attr.foundationBackground, value, true)
                assertEquals(activity.getColor(expected), value.data)
            }
        }
    }

    @Test
    fun tabletKeepsTheSearchAndSelectedSettingsPaneSideBySide() {
        setTabletQualifiers()
        openSettings().use {
            composeRule.onNodeWithContentDescription(searchPrompt).assertIsDisplayed()
            val appearanceTitle = targetContext.getString(R.string.settings_appearance)
            composeRule.onNodeWithText(appearanceTitle).assertIsDisplayed().performClick()
            settle()
            composeRule.onNodeWithContentDescription(searchPrompt).assertIsDisplayed()

            composeRule.onNodeWithText(targetContext.getString(R.string.settings_theme_light)).assertIsDisplayed()
            captureScreen("tablet")
        }
    }

    private val searchPrompt: String
        get() =
            when (language) {
                SIMPLIFIED_CHINESE -> "搜索设置"
                JAPANESE -> "設定を検索"
                ARABIC -> "البحث في الإعدادات"
                else -> "Search settings"
            }

    private val themeTitle: String
        get() =
            when (language) {
                SIMPLIFIED_CHINESE -> "主题"
                JAPANESE -> "テーマ"
                ARABIC -> "السمة"
                else -> "Theme"
            }

    private val themeQuery: String
        get() =
            when (language) {
                SIMPLIFIED_CHINESE -> "主"
                JAPANESE -> "テーマ"
                ARABIC -> "السمة"
                else -> "theme"
            }

    private fun openSettings(): ActivityScenario<PreferencesActivity> {
        runTest { col.reopenWithLanguage(language.option) }
        return ActivityScenario.launch(PreferencesActivity.getIntent(targetContext))
    }

    private fun settle() {
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()
    }
}
