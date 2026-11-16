// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.settings.enums

import androidx.annotation.StyleRes
import com.ichi2.anki.common.android.R

sealed interface Theme : PrefEnum {
    @get:StyleRes
    val styleResId: Int
}

/** The light side of the app. Light is the only day theme: Plain and E-ink were removed. */
enum class DayTheme(
    override val entryResId: Int,
    override val styleResId: Int,
) : Theme {
    LIGHT(R.string.theme_light_value, R.style.Theme_Light),
}
