// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.compose.theme

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.ichi2.anki.common.android.R
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Verifies that the three AnkiDroid XML themes (Light, Dark and Black) are
 * correctly bridged into a Material3 [androidx.compose.material3.ColorScheme]
 * by [toMaterial3ColorScheme].
 *
 * The colours expected here are the Material 3 scheme of ADR-0006
 * (docs/redesign/material-3.values.json); FoundationTest checks every role against that file.
 */
@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    private val appContext: Context get() = ApplicationProvider.getApplicationContext()

    private fun themed(themeRes: Int) = ContextThemeWrapper(appContext, themeRes)

    @Test
    @Config(sdk = [24, Config.TARGET_SDK])
    fun `surface containers are opaque in every app theme`() {
        for (theme in DayTheme.entries + NightTheme.entries) {
            val scheme = themed(theme.styleResId).toMaterial3ColorScheme()
            for (color in listOf(
                scheme.surfaceContainer,
                scheme.surfaceContainerHigh,
                scheme.surfaceContainerHighest,
                scheme.surfaceContainerLow,
                scheme.surfaceContainerLowest,
            )) {
                assertEquals(1f, color.alpha, "${theme.name}: popup surfaces must hide underlying content")
            }
        }
    }

    @Test
    fun `light theme takes the Material 3 primary`() {
        val scheme = themed(R.style.Theme_Light).toMaterial3ColorScheme()
        assertEquals(0xFF6750A4.toInt(), scheme.primary.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), scheme.onPrimary.toArgb())
    }

    @Test
    fun `dark theme takes the Material 3 dark primary, with its on-primary`() {
        val scheme = themed(R.style.Theme_Dark).toMaterial3ColorScheme()
        assertEquals(0xFFD0BCFF.toInt(), scheme.primary.toArgb())
        assertEquals(0xFF381E72.toInt(), scheme.onPrimary.toArgb())
    }

    @Test
    fun `surfaces are tonal, stepping up from the background`() {
        val scheme = themed(R.style.Theme_Light).toMaterial3ColorScheme()
        assertEquals(0xFFFEF7FF.toInt(), scheme.background.toArgb())
        assertEquals(0xFFFEF7FF.toInt(), scheme.surface.toArgb())
        assertEquals(0xFFF7F2FA.toInt(), scheme.surfaceContainerLow.toArgb())
        assertEquals(0xFFECE6F0.toInt(), scheme.surfaceContainerHigh.toArgb())
        assertEquals(0xFF1D1B20.toInt(), scheme.onSurface.toArgb())
    }

    @Test
    fun `black theme produces a different surface than dark theme`() {
        val dark = themed(R.style.Theme_Dark).toMaterial3ColorScheme()
        val black = themed(R.style.Theme_Dark_Black).toMaterial3ColorScheme()
        assertNotEquals(
            dark.surface.toArgb(),
            black.surface.toArgb(),
            "Black theme should be visually distinct from Dark theme on surface color",
        )
        assertEquals(0xFF000000.toInt(), black.surface.toArgb())
    }

    @Test
    fun `elevation does not tint surfaces`() {
        for (theme in DayTheme.entries + NightTheme.entries) {
            val scheme = themed(theme.styleResId).toMaterial3ColorScheme()
            assertEquals(scheme.surface, scheme.surfaceTint, theme.name)
        }
    }

    @Test
    fun `dark theme bridges the inverse colors used by snackbars`() {
        val scheme = themed(R.style.Theme_Dark).toMaterial3ColorScheme()
        assertEquals(0xFFE6E0E9.toInt(), scheme.inverseSurface.toArgb())
        assertEquals(0xFF322F35.toInt(), scheme.inverseOnSurface.toArgb())
        assertEquals(0xFF6750A4.toInt(), scheme.inversePrimary.toArgb())
    }

    @Test
    fun `the add button is the primary container in every theme`() {
        val expected =
            mapOf(
                R.style.Theme_Light to (0xFFEADDFF.toInt() to 0xFF4F378B.toInt()),
                R.style.Theme_Dark to (0xFF4F378B.toInt() to 0xFFEADDFF.toInt()),
                R.style.Theme_Dark_Black to (0xFF4F378B.toInt() to 0xFFEADDFF.toInt()),
            )
        for ((style, colors) in expected) {
            val context = themed(style)
            val ankiColors = context.toAnkiDroidColors(context.toMaterial3ColorScheme())
            assertEquals(colors.first, ankiColors.fabContainer.toArgb())
            assertEquals(colors.second, ankiColors.onFab.toArgb())
        }
    }

    @Test
    fun `answer buttons are tonal pills labelled in their answer colour`() {
        val context = themed(R.style.Theme_Light)
        val colors = context.toAnkiDroidColors(context.toMaterial3ColorScheme())
        assertEquals(0xFFECE6F0.toInt(), colors.goodButton.toArgb())
        assertEquals(0xFF1B6E3A.toInt(), colors.onGoodButton.toArgb())
        assertEquals(0xFFB3261E.toInt(), colors.onAgainButton.toArgb())
        assertEquals(0xFF6750A4.toInt(), colors.showAnswerButton.toArgb())
    }

    @Test
    fun `the study screen is flat`() {
        for (theme in DayTheme.entries + NightTheme.entries) {
            assertEquals(0.dp, themed(theme.styleResId).toAnkiDroidDimensions().studyScreenElevation, theme.name)
        }
    }
}
