// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.assertEquals

/**
 * Interface text is the system sans-serif in every interface language (ADR-0006): no interface font
 * is bundled, and the system chooses the CJK font for the text's language.
 */
@RunWith(RobolectricTestRunner::class)
class InterfaceTypefaceTest {
    private val appContext: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `interface text is the system sans-serif in English, Chinese and Japanese`() {
        for (locale in listOf(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE, Locale.JAPANESE)) {
            val configuration = Configuration(appContext.resources.configuration).apply { setLocale(locale) }
            assertEquals(Typeface.SANS_SERIF, appContext.createConfigurationContext(configuration).interfaceTypeface(), "$locale")
        }
    }
}
