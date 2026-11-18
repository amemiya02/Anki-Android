// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Rule

/**
 * Screenshot tests for [DrawingFragment]
 *
 * `./gradlew :AnkiDroid:verifyRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.DrawingFragmentScreenshotTest"`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class DrawingFragmentScreenshotTest : SingleFragmentScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun buildIntent(): Intent = DrawingFragment.getIntent(targetContext)

    override fun withScreen(block: (SingleFragmentActivity) -> Unit) =
        super.withScreen { activity ->
            composeRule.onNodeWithText(activity.getString(CommonString.drawing)).assertIsDisplayed()
            composeRule.onNodeWithText(activity.getString(CommonString.save)).assertIsDisplayed()
            block(activity)
        }
}
