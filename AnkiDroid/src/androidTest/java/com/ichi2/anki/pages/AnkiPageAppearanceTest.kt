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
import org.junit.Test

class AnkiPageAppearanceTest : InstrumentedTest() {
    @Test
    fun statisticsUsesTheLightPalette() {
        checkStatistics(AppTheme.DAY, NightTheme.DARK, "250, 250, 248", "31, 30, 28", "light")
    }

    @Test
    fun statisticsUsesTheDarkPalette() {
        checkStatistics(AppTheme.NIGHT, NightTheme.DARK, "28, 26, 23", "236, 229, 216", "dark")
    }

    @Test
    fun statisticsUsesTheBlackPalette() {
        checkStatistics(AppTheme.NIGHT, NightTheme.BLACK, "0, 0, 0", "236, 229, 216", "dark")
    }

    private fun checkStatistics(
        appTheme: AppTheme,
        nightTheme: NightTheme,
        paper: String,
        ink: String,
        browserScheme: String,
    ) {
        ensureWebViewIsSupported()
        val prefs = PrefsRepository(testContext)
        val originalAppTheme = prefs.appTheme
        val originalNightTheme = prefs.nightTheme
        try {
            prefs.nightTheme = nightTheme
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
                        return body.backgroundColor === 'rgb($paper)'
                            && body.color === 'rgb($ink)'
                            && getComputedStyle(document.documentElement).colorScheme === '$browserScheme'
                            && body.fontFamily.includes('Newsreader')
                            && [...document.fonts].some(face => face.family === 'Newsreader' && face.status === 'loaded');
                    })();
                    """.trimIndent(),
                    "Statistics did not render the selected Hayaki palette and bundled typeface",
                )
            }
        } finally {
            prefs.appTheme = originalAppTheme
            prefs.nightTheme = originalNightTheme
        }
    }
}
