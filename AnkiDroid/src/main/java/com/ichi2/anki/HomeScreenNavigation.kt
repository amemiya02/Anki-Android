// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.browser.CardBrowserFragment
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.annotations.NeedsTest

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

    // Handle system navigation bar insets for the bottom nav
    ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
        val navBars = insets.getInsets(navigationBars())
        view.updatePadding(bottom = navBars.bottom)
        insets
    }

    bottomNav.onTabClick = { tab ->
        if (tab != bottomNav.selectedTab) {
            Analytics.sendAnalyticsScreenView(tab.analyticsScreenName)
            scrollBehavior.slideIn(bottomNav)
            deckPicker.selectNavigationTab(tab)
        } else if (tab == NavigationItem.BROWSER) {
            (deckPicker.currentScreenFragment as? CardBrowserFragment)?.openSearch()
        }
    }

    bottomNav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateFragmentBottomMargin()
    }
}

internal fun DeckBottomBar.selectTab(item: NavigationItem) {
    if (selectedTab != item) onTabClick(item)
}

context(deckPicker: DeckPicker)
fun selectedBottomNavItem(): NavigationItem? {
    if (!deckPicker.bottomNavigationEnabled) return null
    val bottomNav = deckPicker.binding.bottomNavigation ?: return null
    return bottomNav.selectedTab
}

internal fun DeckPicker.updateTabChrome(tab: NavigationItem?) {
    if (tab == NavigationItem.HOME) title = getString(R.string.app_name)
    val bar = binding.bottomNavigation ?: return
    binding.deckPickerContentWrapper?.isVisible = tab == NavigationItem.HOME
    if (tab == null) {
        floatingActionMenu.resetMenuAfterAdding()
        bar.isVisible = false
        return
    }

    bar.selectedTab = tab
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
