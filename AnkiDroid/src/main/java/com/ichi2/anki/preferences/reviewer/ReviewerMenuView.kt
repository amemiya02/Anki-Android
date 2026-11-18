// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences.reviewer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.size
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.utils.ext.setIconRes
import com.ichi2.utils.increaseHorizontalPaddingOfMenuIcons
import kotlinx.coroutines.launch

/**
 * View for displaying the reviewer menu actions.
 *
 * It works like an [ActionMenuView], but the visible action items are horizontally scrollable by
 * using an [ActionMenuView] inside a [HorizontalScrollView], and another one for the overflown
 * actions.
 */
class ReviewerMenuView : LinearLayout {
    private val repository = ReviewerMenuRepository(context.sharedPrefs())
    private val frontMenuView = ActionMenuView(context).apply { id = R.id.front_menu_view }
    private val overflowMenuView = ActionMenuView(context).apply { id = R.id.overflow_menu_view }
    private val frontMenu: Menu = frontMenuView.menu
    private val overflowMenu: Menu =
        overflowMenuView.menu.apply {
            (this as? MenuBuilder)?.setOptionalIconsVisible(true)
        }
    private var onMenuItemClickListener: ActionMenuView.OnMenuItemClickListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val scroll =
            HorizontalScrollView(context).apply {
                id = R.id.horizontalScrollView
                isHorizontalScrollBarEnabled = false
                addView(
                    frontMenuView,
                    android.widget.FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END),
                )
            }
        addView(scroll, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f))
        addView(overflowMenuView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        setupMenus()
    }

    fun clear() {
        frontMenu.clear()
        overflowMenu.clear()
    }

    fun isEmpty() = frontMenu.size == 0 && overflowMenu.size == 0

    fun findItem(id: Int): MenuItemImpl? = (frontMenu.findItem(id) ?: overflowMenu.findItem(id)) as? MenuItemImpl

    fun setOnMenuItemClickListener(listener: ActionMenuView.OnMenuItemClickListener) {
        onMenuItemClickListener = listener
        frontMenuView.setOnMenuItemClickListener(listener)
        overflowMenuView.setOnMenuItemClickListener(listener)
    }

    /** Opens the configured Card actions even when a template has hidden the toolbar. */
    fun showOptionsMenu(anchor: View): Boolean {
        if (isShown && overflowMenuView.showOverflowMenu()) return true
        val popup = PopupMenu(context, anchor, Gravity.END)
        copyVisibleItems(frontMenu, popup.menu)
        copyVisibleItems(overflowMenu, popup.menu)
        if (!popup.menu.hasVisibleItems()) return false
        popup.setOnMenuItemClickListener { onMenuItemClickListener?.onMenuItemClick(it) ?: false }
        popup.show()
        return true
    }

    private fun copyVisibleItems(
        from: Menu,
        to: Menu,
    ) {
        for (item in from.children.filter { it.isVisible }) {
            val submenu = item.subMenu
            val copy =
                if (submenu == null) {
                    to.add(item.groupId, item.itemId, item.order, item.title)
                } else {
                    to
                        .addSubMenu(item.groupId, item.itemId, item.order, item.title)
                        .also {
                            copyVisibleItems(submenu, it)
                        }.item
                }
            copy.isEnabled = item.isEnabled
            copy.isCheckable = item.isCheckable
            copy.isChecked = item.isChecked
            copy.icon = item.icon
        }
    }

    fun addActions(
        alwaysShow: List<ViewerAction>,
        menuOnly: List<ViewerAction>,
    ) {
        addActionsToMenu(frontMenu, alwaysShow, MenuItem.SHOW_AS_ACTION_ALWAYS)
        addActionsToMenu(overflowMenu, menuOnly, MenuItem.SHOW_AS_ACTION_NEVER)

        val submenuActions = ViewerAction.entries.filter { it.parentMenu != null }
        for (action in submenuActions) {
            val subMenu = findItem(action.parentMenu!!.menuId)?.subMenu ?: continue
            subMenu.add(Menu.NONE, action.menuId, Menu.NONE, action.title(context))?.apply {
                action.drawableRes?.let { setIconRes(context, it) }
            }
        }
    }

    suspend fun setFlagTitles() {
        val submenu = findItem(R.id.action_flag)?.subMenu ?: return
        for ((flag, name) in Flag.queryDisplayNames(context)) {
            submenu.findItem(flag.id)?.title = name
        }
    }

    private fun addActionsToMenu(
        menu: Menu,
        actions: List<ViewerAction>,
        menuActionType: Int,
    ) {
        val subMenus = ViewerAction.getSubMenus()
        for (action in actions) {
            val title = action.title(context)
            val menuItem =
                if (action in subMenus) {
                    menu.addSubMenu(Menu.NONE, action.menuId, Menu.NONE, title).item
                } else {
                    menu.add(Menu.NONE, action.menuId, Menu.NONE, title)
                }
            with(menuItem) {
                action.drawableRes?.let { setIconRes(context, it) }
                setShowAsAction(menuActionType)
            }
        }
    }

    private fun setupMenus() {
        val menuItems = repository.getActionsByMenuDisplayTypes(MenuDisplayType.ALWAYS, MenuDisplayType.MENU_ONLY)
        addActions(menuItems.getValue(MenuDisplayType.ALWAYS), menuItems.getValue(MenuDisplayType.MENU_ONLY))

        context.increaseHorizontalPaddingOfMenuIcons(overflowMenu)

        // wait until attached to a fragment or activity to launch the coroutine to setup flags
        viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                        setFlagTitles()
                    }
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            },
        )
    }
}
