// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget.cardanalysis

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.widget.AppWidgetId
import com.ichi2.widget.cardanalysis.CardAnalysisWidgetConfig
import com.ichi2.widget.cardanalysis.CardAnalysisWidgetPreferences
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class CardAnalysisWidgetSetupScreenshotTest : ScreenshotTest() {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun configuredDeck() {
        ensureNonEmptyCollection()
        val name = "日本語"
        val widget = AppWidgetId(46)
        CardAnalysisWidgetPreferences(targetContext).saveSelectedDeck(widget, addDeck(name))
        startActivityNormallyOpenCollectionWithIntent(
            CardAnalysisWidgetConfig::class.java,
            Intent(targetContext, CardAnalysisWidgetConfig::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.id),
        )
        advanceRobolectricLooperUntil {
            runCatching { compose.onNodeWithText(name).assertIsDisplayed() }.isSuccess
        }
        captureScreen("selected_deck")
    }
}
