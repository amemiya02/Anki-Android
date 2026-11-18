// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki

import android.graphics.Typeface
import android.os.Build
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.browser.CardBrowserFragment
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.android.themes.interfaceTypeface
import com.ichi2.anki.common.annotations.NeedsTest
import com.google.android.material.R as MaterialR

/**
 * Manages the bottom navigation bar for the home screen.
 *
 * On tablets (fragmented), this is a no-op because tablets use the navigation
 * drawer with a split pane.
 */
@NeedsTest("tab switches show/hide correct fragments")
@NeedsTest("back press returns to Home tab before exiting")
context(deckPicker: DeckPicker)
fun setupBottomNavigation() {
    if (!deckPicker.bottomNavigationEnabled) return

    val binding = deckPicker.binding
    val bottomNav = checkNotNull(binding.bottomNavigation)
    bottomNav.isVisible = true
    val scrollBehavior = HideViewOnScrollBehavior.from(bottomNav)

    fun updateFragmentBottomMargin() {
        val margin =
            if (bottomNav.isVisible && scrollBehavior.isScrolledIn) {
                bottomNav.height
            } else {
                ViewCompat.getRootWindowInsets(bottomNav)?.getInsets(navigationBars())?.bottom ?: 0
            }
        deckPicker.bottomNavContentClearance.intValue = margin
    }

    // Never hide the bar while explore-by-touch (e.g. TalkBack) is on, so it stays reachable.
    scrollBehavior.disableOnTouchExploration(true)
    scrollBehavior.addOnScrollStateChangedListener { _, _ ->
        updateFragmentBottomMargin()
        // Re-apply insets so the Decks FAB and list padding follow the bar's new position.
        ViewCompat.requestApplyInsets(deckPicker.deckPickerBinding.root)
    }

    NavigationItem.populateMenu(bottomNav, deckPicker)
    bottomNav.useInterfaceTypeface()

    // Handle system navigation bar insets for the bottom nav
    ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
        val navBars = insets.getInsets(navigationBars())
        view.updatePadding(bottom = navBars.bottom)
        insets
    }

    bottomNav.setOnItemSelectedListener { item ->
        val navItem = NavigationItem.fromId(item.itemId) ?: return@setOnItemSelectedListener false
        if (item.itemId != bottomNav.selectedItemId) {
            Analytics.sendAnalyticsScreenView(navItem.analyticsScreenName)
            scrollBehavior.slideIn(bottomNav)
        }
        deckPicker.selectNavigationTab(navItem)
        true
    }

    // A second tap on Browse opens search in the current browser.
    bottomNav.setOnItemReselectedListener { item ->
        if (item.itemId == NavigationItem.BROWSER.id) {
            val browser = deckPicker.currentScreenFragment as? CardBrowserFragment
            browser?.openSearch()
        }
    }

    bottomNav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateFragmentBottomMargin()
    }
}

/**
 * Draws the bar's labels in the interface typeface of the app's language, the current tab's at
 * weight 600. Material builds the labels itself, so they miss the typeface every inflated text gets,
 * and its text appearances alone leave Chinese and Japanese labels at the regular weight.
 */
private fun BottomNavigationView.useInterfaceTypeface() {
    val regular = context.interfaceTypeface(opticalSize = 12f)
    val current =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(regular, 600, false)
        } else {
            Typeface.create(regular, Typeface.BOLD)
        }
    val items = getChildAt(0) as? ViewGroup ?: return
    items.children.forEach { item ->
        item.findViewById<TextView>(MaterialR.id.navigation_bar_item_small_label_view)?.typeface = regular
        item.findViewById<TextView>(MaterialR.id.navigation_bar_item_large_label_view)?.typeface = current
    }
}

internal fun BottomNavigationView.selectTab(item: NavigationItem) {
    if (selectedItemId != item.id) {
        selectedItemId = item.id
    }
}

context(deckPicker: DeckPicker)
fun selectedBottomNavItem(): NavigationItem? {
    if (!deckPicker.bottomNavigationEnabled) return null
    val bottomNav = deckPicker.binding.bottomNavigation ?: return null
    return NavigationItem.fromId(bottomNav.selectedItemId)
}

internal fun DeckPicker.updateTabChrome(tab: NavigationItem?) {
    if (tab == NavigationItem.HOME) title = getString(R.string.app_name)
    val bar = binding.bottomNavigation ?: return
    binding.deckPickerContentWrapper?.isVisible = tab == NavigationItem.HOME
    if (tab == null) {
        bar.isVisible = false
        return
    }

    bar.menu.findItem(tab.id).isChecked = true
    setBrowserFullScreen(false)
    if (tab == NavigationItem.HOME) floatingActionMenu.showFloatingActionButton()
}

/**
 * Hides the bottom bar on downward scrolls and shows it on upward scrolls.
 * For views which do not send nested-scroll events to the bar's [HideViewOnScrollBehavior].
 */
fun Fragment.updateBottomNavOnScroll(dy: Int) {
    val deckPicker = activity as? DeckPicker ?: return
    if (dy == 0 || !isVisible || !deckPicker.bottomNavigationEnabled) return
    val bottomNav = deckPicker.binding.bottomNavigation ?: return
    val behavior = HideViewOnScrollBehavior.from(bottomNav)
    if (dy > 0) behavior.slideOut(bottomNav) else behavior.slideIn(bottomNav)
}

/** Search and selection take over the Browse tab and replace the bottom navigation. */
internal fun DeckPicker.setBrowserFullScreen(fullScreen: Boolean) {
    if (!bottomNavigationEnabled) return
    val bar = binding.bottomNavigation ?: return
    if (bar.isVisible == !fullScreen) return
    bar.isVisible = !fullScreen
    if (!fullScreen) HideViewOnScrollBehavior.from(bar).slideIn(bar)
    bottomNavContentClearance.intValue = if (fullScreen) 0 else bar.height
}
