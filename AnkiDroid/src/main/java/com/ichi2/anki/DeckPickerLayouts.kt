// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentContainerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.ui.ResizablePaneLayout
import com.ichi2.anki.ui.ResizingDivider
import com.ichi2.anki.common.android.R as CommonR

/** Native containers for the Compose deck list and the adaptive study pane. */
internal class DeckPickerLayout(
    context: Context,
) {
    val root = CoordinatorLayout(context).apply { id = R.id.root_layout }
    val rootLayout: View get() = root
    val deckPickerPane = DeckListLayout(context)
    val toolbarContainer = ComposeView(context).apply { id = R.id.toolbar_container }
    val deckPickerContentWrapper: LinearLayout?
    val bottomNavigation: DeckBottomBar?
    val deckpickerXlView: ResizablePaneLayout?
    val resizingDivider: ResizingDivider?
    val studyoptionsFragment: FragmentContainerView?

    init {
        val content =
            LinearLayout(context).apply {
                id = R.id.deck_picker_content_wrapper
                orientation = LinearLayout.VERTICAL
                addView(toolbarContainer, MATCH_PARENT, WRAP_CONTENT)
            }
        if (context.resources.getBoolean(R.bool.bottom_navigation_available)) {
            deckPickerContentWrapper = content
            content.addView(deckPickerPane.root, MATCH_PARENT, MATCH_PARENT)
            root.addView(content, MATCH_PARENT, MATCH_PARENT)
            val bar =
                DeckBottomBar(context).apply {
                    id = R.id.bottom_navigation
                    // the system navigation inset below the bar takes the bar's container colour
                    setBackgroundColor(getColorFromAttr(context, com.google.android.material.R.attr.colorSurfaceContainer))
                    minimumHeight = resources.getDimensionPixelSize(R.dimen.bottom_bar_height)
                    visibility = View.GONE
                }
            root.addView(
                bar,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM
                    behavior = HideViewOnScrollBehavior<DeckBottomBar>()
                },
            )
            bottomNavigation = bar
            deckpickerXlView = null
            resizingDivider = null
            studyoptionsFragment = null
        } else {
            deckPickerContentWrapper = null
            val panes =
                ResizablePaneLayout(context).apply {
                    id = R.id.deckpicker_xl_view
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(getColorFromAttr(context, android.R.attr.colorBackground))
                }
            val divider = ResizingDivider(context).apply { id = R.id.resizing_divider }
            val overview = FragmentContainerView(context).apply { id = R.id.studyoptions_fragment }
            panes.addView(deckPickerPane.root, LinearLayout.LayoutParams(0, MATCH_PARENT, 3f))
            panes.addView(divider, context.layoutDp(8), MATCH_PARENT)
            panes.addView(overview, LinearLayout.LayoutParams(0, MATCH_PARENT, 2f))
            content.addView(panes, MATCH_PARENT, MATCH_PARENT)
            root.addView(content, MATCH_PARENT, MATCH_PARENT)
            bottomNavigation = null
            deckpickerXlView = panes
            resizingDivider = divider
            studyoptionsFragment = overview
        }
    }
}

internal class DeckListLayout(
    context: Context,
) {
    val root = CoordinatorLayout(context).apply { id = R.id.deck_picker_pane }
    val background =
        ImageView(context).apply {
            id = R.id.background
            scaleType = ImageView.ScaleType.CENTER_CROP
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    val deckpickerView =
        LinearLayout(context).apply {
            id = R.id.deckpicker_view
            orientation = LinearLayout.VERTICAL
        }
    val pullToSyncWrapper = SwipeRefreshLayout(context).apply { id = R.id.pull_to_sync_wrapper }
    val deckList = ComposeView(context).apply { id = R.id.deck_list }
    val progressBar = ProgressBar(context).apply { id = R.id.progress_bar }
    val floatingActionButton = AddMenuLayout(context)
    val deckPickerContent =
        FrameLayout(context).apply {
            id = R.id.deck_picker_content
            addView(deckList, MATCH_PARENT, MATCH_PARENT)
        }

    init {
        pullToSyncWrapper.addView(deckPickerContent, MATCH_PARENT, MATCH_PARENT)
        deckpickerView.addView(pullToSyncWrapper, MATCH_PARENT, MATCH_PARENT)
        root.addView(background, MATCH_PARENT, MATCH_PARENT)
        root.addView(deckpickerView, MATCH_PARENT, MATCH_PARENT)
        root.addView(
            progressBar,
            CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER },
        )
        root.addView(floatingActionButton.root, MATCH_PARENT, MATCH_PARENT)
    }
}

class AddMenuLayout internal constructor(
    context: Context,
) {
    val root = CoordinatorLayout(context).apply { id = R.id.floating_action_button }
    val fabBGLayout =
        View(context).apply {
            id = R.id.fabBGLayout
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
        }
    val fabLinearLayout =
        LinearLayout(context).apply {
            id = R.id.fabLinearLayout
            orientation = LinearLayout.VERTICAL
            val padding = context.layoutDp(12)
            setPadding(padding, padding, padding, padding)
        }
    val addSharedButton = addOption(context, R.id.add_shared_button, R.drawable.symbol_download, CommonString.menu_get_shared_decks)
    val addFilteredDeckButton =
        addOption(context, R.id.add_filtered_deck_button, R.drawable.symbol_filter_alt, CommonString.new_dynamic_deck)
    val addDeckButton = addOption(context, R.id.add_deck_button, R.drawable.symbol_create_new_folder)
    val fabMain =
        DeckPickerAddButton(context, CommonR.drawable.symbol_add, primary = true).apply {
            id = R.id.fab_main
        }

    init {
        // the add buttons' shadows fall outside their own bounds: let them draw there, not cut square
        fabLinearLayout.clipChildren = false
        fabLinearLayout.clipToPadding = false
        val scroll = ScrollView(context).apply { addView(fabLinearLayout, WRAP_CONTENT, WRAP_CONTENT) }
        fabLinearLayout.addView(fabMain, optionLayoutParams(context).apply { bottomMargin = context.layoutDp(16) })
        root.addView(fabBGLayout, MATCH_PARENT, MATCH_PARENT)
        root.addView(
            scroll,
            CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginEnd = context.layoutDp(8)
                gravity = Gravity.BOTTOM or Gravity.END
            },
        )
    }

    private fun addOption(
        context: Context,
        id: Int,
        icon: Int,
        label: Int? = null,
    ): DeckPickerAddButton =
        DeckPickerAddButton(context, icon, primary = false).apply {
            this.id = id
            text = label?.let(context::getText)
            visibility = View.GONE
            fabLinearLayout.addView(this, optionLayoutParams(context))
        }

    private fun optionLayoutParams(context: Context) =
        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.END
            val margin = context.layoutDp(8)
            setMargins(margin, margin, margin, margin)
        }
}

internal fun createAppToolbar(
    context: Context,
    theme: Int = CommonR.style.ActionBarStyle,
): FrameLayout =
    FrameLayout(context).apply {
        id = R.id.toolbar_container
        setBackgroundColor(getColorFromAttr(context, CommonR.attr.appBarColor))
        val toolbar =
            Toolbar(ContextThemeWrapper(context, theme)).apply {
                id = R.id.toolbar
                val size = TypedValue()
                if (context.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, size, true)) {
                    minimumHeight = TypedValue.complexToDimensionPixelSize(size.data, resources.displayMetrics)
                }
                setNavigationContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)
                if (this.context.theme.resolveAttribute(androidx.appcompat.R.attr.homeAsUpIndicator, size, true) && size.resourceId != 0) {
                    navigationIcon = AppCompatResources.getDrawable(this.context, size.resourceId)
                }
                setBackgroundColor(getColorFromAttr(context, CommonR.attr.appBarColor))
            }
        addView(toolbar, MATCH_PARENT, WRAP_CONTENT)
    }

internal fun Context.layoutDp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
