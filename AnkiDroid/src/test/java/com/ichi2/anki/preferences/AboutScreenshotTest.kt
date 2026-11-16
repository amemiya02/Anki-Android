// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import org.junit.Test

/**
 * The about screen, which names the Fork Hayaki and says it is an unofficial fork of AnkiDroid,
 * in each first-class language.
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light --tests "com.ichi2.anki.preferences.AboutScreenshotTest"`
 */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE)
class AboutScreenshotTest : ScreenshotTest() {
    @Test
    fun aboutScreen() {
        val intent = PreferencesActivity.getIntent(targetContext, AboutFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                // the build date and versions change from build to build
                activity.findViewById<TextView>(R.id.build_date).text = "18 May 2026"
                activity.findViewById<TextView>(R.id.version).text = "2.26.0"
                activity.findViewById<TextView>(R.id.backend_version).text = "(anki 25.09 / 00000000)"
                advanceRobolectricLooper()
                captureScreen("about")
            }
        }
    }
}
