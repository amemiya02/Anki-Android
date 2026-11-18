// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class IntroductionScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun welcomeAndMigration() {
        ActivityScenario.launch(IntroductionActivity::class.java).use {
            text(CommonString.paper_welcome_title).assertIsDisplayed()
            text(CommonString.paper_welcome_unofficial).assertIsDisplayed()
            captureScreen("welcome")
            choice(CommonString.paper_welcome_start_empty).performScrollTo().assertIsDisplayed()
            choice(CommonString.paper_welcome_import).performScrollTo().assertIsDisplayed()
            choice(CommonString.paper_welcome_sync).performScrollTo().assertIsDisplayed()
            val fromAnkiWeb = choice(CommonString.paper_sync_from_ankiweb).performScrollTo().assertIsDisplayed()
            if (fontSize == PERCENT_200) captureScreen("welcome_actions")
            fromAnkiWeb.performClick()

            text(CommonString.paper_welcome_migration_title).assertIsDisplayed()
            choice(CommonString.paper_sync_choose_file).assertIsDisplayed()
            captureScreen("migration")
            text(CommonString.paper_welcome_migration_sync_hint).performScrollTo().assertIsDisplayed()
            text(CommonString.paper_welcome_migration_export_hint).performScrollTo().assertIsDisplayed()
            text(CommonString.paper_welcome_migration_import_hint).performScrollTo().assertIsDisplayed()
            if (fontSize == PERCENT_200) captureScreen("migration_steps")
        }
    }

    private fun choice(
        @StringRes label: Int,
    ) = compose.onNode(hasText(targetContext.getString(label)) and hasClickAction())

    private fun text(
        @StringRes label: Int,
    ) = compose.onNodeWithText(targetContext.getString(label))
}
