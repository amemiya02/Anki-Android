// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.destinations.StatisticsDestination
import com.ichi2.anki.settings.PrefsRepository
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.ensureWebViewIsSupported
import com.ichi2.anki.testutil.waitForPageCondition
import com.ichi2.themes.HayakiTheme
import org.junit.Test

class AnkiPageAppearanceTest : InstrumentedTest() {
    @Test
    fun statisticsUsesTheLightPalette() {
        checkStatistics(AppTheme.DAY, NightTheme.DARK, HayakiTheme.HAYAKI, "242, 245, 244", "29, 29, 29", "#2C776E", "light")
    }

    @Test
    fun statisticsUsesTheDarkPalette() {
        checkStatistics(AppTheme.NIGHT, NightTheme.DARK, HayakiTheme.HAYAKI, "24, 30, 29", "230, 230, 230", "#7ED6CA", "dark")
    }

    @Test
    fun statisticsUsesTheBlackPalette() {
        checkStatistics(AppTheme.NIGHT, NightTheme.BLACK, HayakiTheme.HAYAKI, "0, 0, 0", "230, 230, 230", "#7ED6CA", "dark")
    }

    @Test
    fun statisticsUsesTheSelectedTheme() {
        checkStatistics(AppTheme.DAY, NightTheme.DARK, HayakiTheme.SAKURA, "247, 243, 244", "29, 29, 29", "#B04663", "light")
    }

    private fun checkStatistics(
        appTheme: AppTheme,
        nightTheme: NightTheme,
        theme: HayakiTheme,
        paper: String,
        ink: String,
        accent: String,
        browserScheme: String,
    ) {
        ensureWebViewIsSupported()
        val prefs = PrefsRepository(testContext)
        val originalAppTheme = prefs.appTheme
        val originalNightTheme = prefs.nightTheme
        val originalTheme = prefs.hayakiTheme
        try {
            prefs.nightTheme = nightTheme
            prefs.hayakiTheme = theme
            prefs.appTheme = appTheme
            val intent = StatisticsDestination.toIntent(testContext)
            ActivityScenario.launch<SingleFragmentActivity>(intent).use { scenario ->
                lateinit var page: Statistics
                scenario.onActivity { page = it.fragment as Statistics }
                page.waitForPageCondition(
                    "document.querySelector('.graphs-container h1')?.textContent.trim().length > 0",
                    "Statistics did not render its section headings",
                )
                page.waitForPageCondition(
                    """
                    (() => {
                        const body = getComputedStyle(document.body);
                        const page = getComputedStyle(document.documentElement);
                        return body.backgroundColor === 'rgb($paper)'
                            && body.color === 'rgb($ink)'
                            && page.colorScheme === '$browserScheme'
                            && page.getPropertyValue('--hayaki-primary').trim().toUpperCase() === '$accent'
                            && body.fontFamily.includes('sans-serif');
                    })();
                    """.trimIndent(),
                    "Statistics did not render the selected Hayaki palette and the system sans-serif",
                )
            }
        } finally {
            prefs.appTheme = originalAppTheme
            prefs.hayakiTheme = originalTheme
            prefs.nightTheme = originalNightTheme
        }
    }
}
