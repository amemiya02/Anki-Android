// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki

import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ichi2.anki.CollectionManager.TR

private const val DECK_PICKER_SCREEN_NAME = "DeckPicker"
private const val CARD_BROWSER_SCREEN_NAME = "CardBrowser"
private const val STATISTICS_SCREEN_NAME = "Statistics"
private const val MORE_SCREEN_NAME = "MoreFragment"

/** The four top-level destinations shown in the phone's bottom bar. */
object BottomNavController {
    enum class NavigationItem(
        @IdRes val id: Int,
        /** The destination identifier in the application navigation graph. */
        val tag: String,
        /** Describes the tab in the keyboard shortcuts list; null: its title */
        @StringRes private val shortcutLabelRes: Int?,
        val analyticsScreenName: String,
    ) {
        HOME(R.id.nav_home, "home", CommonString.deck_picker_group, DECK_PICKER_SCREEN_NAME),
        BROWSER(R.id.nav_browser, "browser", CommonString.card_browser_context_menu, CARD_BROWSER_SCREEN_NAME),
        STATS(R.id.nav_stats, "stats", CommonString.open_statistics, STATISTICS_SCREEN_NAME),
        MORE(R.id.nav_more, "more", null, MORE_SCREEN_NAME),
        ;

        fun title(context: Context): String =
            when (this) {
                HOME -> TR.actionsDecks()
                BROWSER -> TR.qtMiscBrowse()
                STATS -> TR.qtMiscStats()
                MORE -> TR.studyingMore()
            }

        fun shortcutLabel(context: Context): String = shortcutLabelRes?.let(context::getString) ?: title(context)

        companion object {
            fun populateMenu(
                bottomNavigationView: BottomNavigationView,
                context: Context,
            ) {
                entries.forEach { item ->
                    bottomNavigationView.menu.findItem(item.id)?.title = item.title(context)
                }
            }

            fun fromId(
                @IdRes id: Int,
            ): NavigationItem? =
                when (id) {
                    R.id.nav_home -> HOME
                    R.id.nav_browser -> BROWSER
                    R.id.nav_stats -> STATS
                    R.id.nav_more -> MORE
                    else -> null
                }
        }
    }
}
