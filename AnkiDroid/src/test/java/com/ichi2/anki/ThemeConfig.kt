// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.settings.enums.Theme

/** A theme that screenshot tests can be run with: the three themes the app offers */
enum class ThemeConfig(
    val theme: Theme,
) {
    LIGHT(DayTheme.LIGHT),
    DARK(NightTheme.DARK),
    BLACK(NightTheme.BLACK),
    ;

    val isNight get() = theme is NightTheme
}

/**
 * Provides the themes selected with `-Ptheme`: a comma-separated list, or `all`.
 * Without a selection, every theme runs: the Quality Bar checks each surface in Light, Dark and Black.
 */
class ThemeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<ThemeConfig> = selectedThemes(System.getProperty("screenshot.theme"))

    companion object {
        /** The themes named by [requested] (the value of `-Ptheme`), or every theme when it is `null` */
        fun selectedThemes(requested: String?): List<ThemeConfig> {
            val requestedThemes = (requested ?: "all").split(",").map { it.trim().lowercase() }
            if ("all" in requestedThemes) {
                return ThemeConfig.entries
            }
            return ThemeConfig.entries.filter { requestedThemes.contains(it.name.lowercase()) }
        }
    }
}
