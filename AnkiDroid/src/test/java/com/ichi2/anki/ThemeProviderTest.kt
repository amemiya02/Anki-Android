// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.ichi2.anki.ThemeProvider.Companion.selectedThemes
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

/** The rendered-screen harness checks the three themes the app offers: Light, Dark and Black */
class ThemeProviderTest {
    @Test
    fun `the harness runs Light, Dark and Black by default`() {
        assertThat(selectedThemes(null), equalTo(listOf(ThemeConfig.LIGHT, ThemeConfig.DARK, ThemeConfig.BLACK)))
    }

    @Test
    fun `the harness offers only Light, Dark and Black`() {
        assertThat(selectedThemes("all").map { it.name }, equalTo(listOf("LIGHT", "DARK", "BLACK")))
    }

    @Test
    fun `themes can be selected by name`() {
        assertThat(selectedThemes("dark, Black"), equalTo(listOf(ThemeConfig.DARK, ThemeConfig.BLACK)))
    }
}
