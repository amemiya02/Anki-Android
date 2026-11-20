// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.themes

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.ichi2.anki.R
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.settings.enums.Theme
import com.ichi2.anki.common.android.R as CommonR

/** The learner's palette, stored independently of the Light, Dark or Black appearance. */
enum class HayakiTheme(
    val id: String,
    @StringRes val label: Int,
    @StyleRes private val lightStyle: Int,
    @StyleRes private val darkStyle: Int,
    @StyleRes private val blackStyle: Int,
) {
    HAYAKI(
        "hayaki",
        R.string.settings_brand_theme_hayaki,
        CommonR.style.Theme_Light,
        CommonR.style.Theme_Dark,
        CommonR.style.Theme_Dark_Black,
    ),
    SAKURA(
        "sakura",
        R.string.settings_brand_theme_sakura,
        CommonR.style.HayakiTheme_Sakura_Light,
        CommonR.style.HayakiTheme_Sakura_Dark,
        CommonR.style.HayakiTheme_Sakura_Black,
    ),
    MATCHA(
        "matcha",
        R.string.settings_brand_theme_matcha,
        CommonR.style.HayakiTheme_Matcha_Light,
        CommonR.style.HayakiTheme_Matcha_Dark,
        CommonR.style.HayakiTheme_Matcha_Black,
    ),
    OCEAN(
        "ocean",
        R.string.settings_brand_theme_ocean,
        CommonR.style.HayakiTheme_Ocean_Light,
        CommonR.style.HayakiTheme_Ocean_Dark,
        CommonR.style.HayakiTheme_Ocean_Black,
    ),
    INDIGO(
        "indigo",
        R.string.settings_brand_theme_indigo,
        CommonR.style.HayakiTheme_Indigo_Light,
        CommonR.style.HayakiTheme_Indigo_Dark,
        CommonR.style.HayakiTheme_Indigo_Black,
    ),
    WISTERIA(
        "wisteria",
        R.string.settings_brand_theme_wisteria,
        CommonR.style.HayakiTheme_Wisteria_Light,
        CommonR.style.HayakiTheme_Wisteria_Dark,
        CommonR.style.HayakiTheme_Wisteria_Black,
    ),
    AMBER(
        "amber",
        R.string.settings_brand_theme_amber,
        CommonR.style.HayakiTheme_Amber_Light,
        CommonR.style.HayakiTheme_Amber_Dark,
        CommonR.style.HayakiTheme_Amber_Black,
    ),
    SUMI(
        "sumi",
        R.string.settings_brand_theme_sumi,
        CommonR.style.HayakiTheme_Sumi_Light,
        CommonR.style.HayakiTheme_Sumi_Dark,
        CommonR.style.HayakiTheme_Sumi_Black,
    ),
    PAPER(
        "paper",
        R.string.settings_brand_theme_paper,
        CommonR.style.HayakiTheme_Paper_Light,
        CommonR.style.HayakiTheme_Paper_Dark,
        CommonR.style.HayakiTheme_Paper_Black,
    ),
    MIDNIGHT(
        "midnight",
        R.string.settings_brand_theme_midnight,
        CommonR.style.HayakiTheme_Midnight_Light,
        CommonR.style.HayakiTheme_Midnight_Dark,
        CommonR.style.HayakiTheme_Midnight_Black,
    ),
    ;

    @StyleRes
    fun styleFor(appearance: Theme): Int =
        when (appearance) {
            DayTheme.LIGHT -> lightStyle
            NightTheme.DARK -> darkStyle
            NightTheme.BLACK -> blackStyle
        }

    companion object {
        fun fromId(id: String?): HayakiTheme = entries.firstOrNull { it.id == id } ?: HAYAKI
    }
}
