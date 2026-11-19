// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.ClosableDrawerLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.NoteEditorFragment.Companion.NoteEditorCaller
import com.ichi2.anki.common.android.animationEnabled
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.common.destinations.PreferencesDestination
import com.ichi2.anki.common.destinations.StatisticsDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.grantedStoragePermissions
import com.ichi2.anki.common.utils.android.HandlerUtils
import com.ichi2.anki.dialogs.help.HelpDialog
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.workarounds.FullDraggableContainerFix
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

abstract class NavigationDrawerActivity(
    @LayoutRes contentLayoutId: Int? = null,
) : AnkiActivity(contentLayoutId) {
    /**
     * Navigation Drawer
     */
    open var fragmented = false
        protected set

    /**
     * Whether this screen has the side drawer. Only large screens have it: phones navigate with
     * the bottom bar, and their layout holds no drawer at all.
     */
    val hasNavigationDrawer: Boolean
        get() = !resources.getBoolean(R.bool.bottom_navigation_available)
    private var navButtonGoesBack = false

    // Navigation drawer list item entries
    lateinit var drawerLayout: DrawerLayout
    private var navigationView: TabletDrawer? = null
    lateinit var drawerToggle: ActionBarDrawerToggle
        private set

    /**
     * runnable that will be executed after the drawer has been closed.
     */
    private var pendingRunnable: Runnable? = null

    private val drawerBackCallback =
        object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                Timber.d("drawerBackCallback")
                closeDrawer()
            }
        }

    override fun setViewBinding(binding: ViewBinding) = setDrawerContent(binding.root)

    override fun setContentView(
        @LayoutRes layoutResID: Int,
    ) = setDrawerContent(layoutInflater.inflate(layoutResID, null))

    protected fun setDrawerContent(view: View) {
        val drawer =
            ClosableDrawerLayout(this).apply {
                id = R.id.drawer_layout
                fitsSystemWindows = this@NavigationDrawerActivity.fitsSystemWindows()
            }

        val fullScreenDrawer = hasNavigationDrawer && baseContext.sharedPrefs().getBoolean(PREF_FULL_SCREEN_NAVIGATION_DRAWER, false)
        val content =
            if (fullScreenDrawer) {
                FullDraggableContainerFix(this).apply { addView(view, MATCH_PARENT, MATCH_PARENT) }
            } else {
                view
            }
        drawer.addView(content, DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        if (hasNavigationDrawer) {
            val navigation = createNavigationView()
            val width = resources.getDimensionPixelSize(R.dimen.nav_drawer_width)
            drawer.addView(navigation, DrawerLayout.LayoutParams(width, MATCH_PARENT).apply { gravity = GravityCompat.START })
        }
        setContentView(drawer)
    }

    private fun createNavigationView() = TabletDrawer(this, ::onNavigationItemSelected)

    /** Whether android:fitsSystemWindows="true" should be applied to the navigation drawer  */
    protected open fun fitsSystemWindows(): Boolean = true

    fun navDrawerIsReady(): Boolean = ::drawerLayout.isInitialized

    // Navigation drawer initialisation
    @Suppress("DEPRECATION", "API35 properly handle edge-to-edge")
    protected fun initNavigationDrawer(mainView: View = findViewById(android.R.id.content)) {
        // Create inherited navigation drawer layout here so that it can be used by parent class
        drawerLayout = mainView.findViewById(R.id.drawer_layout)
        // Force transparent status bar with primary dark color underlaid so that the drawer displays under status bar
        window.statusBarColor = getColor(CommonR.color.transparent)
        drawerLayout.setStatusBarBackgroundColor(
            MaterialColors.getColor(
                this,
                CommonR.attr.appBarColor,
                0,
            ),
        )
        // Setup toolbar and hamburger
        // Phones' layout holds no drawer (see hasNavigationDrawer)
        navigationView = drawerLayout.findViewById(R.id.navdrawer_items_container)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            applyDrawerInsets(insets)
            insets
        }
        val toolbar: Toolbar? = mainView.findViewById(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            // enable ActionBar app icon to behave as action to toggle nav drawer
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)

            // Decide which action to take when the navigation button is tapped.
            toolbar.setNavigationOnClickListener { onNavigationPressed() }
        }
        setupBackPressedCallbacks()
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle =
            object : ActionBarDrawerToggle(
                this,
                drawerLayout,
                CommonString.drawer_open,
                CommonString.drawer_close,
            ) {
                override fun onDrawerClosed(drawerView: View) {
                    super.onDrawerClosed(drawerView)
                    invalidateOptionsMenu()
                    drawerBackCallback.isEnabled = false
                    // If animations are disabled, this is executed before onNavigationItemSelected is called
                    // PERF: May be able to reduce this delay
                    HandlerUtils.postDelayedOnNewHandler({
                        if (pendingRunnable != null) {
                            HandlerUtils.postOnNewHandler(pendingRunnable!!) // TODO: See if we can use the same handler here
                            pendingRunnable = null
                        }
                    }, 100)
                }

                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    invalidateOptionsMenu()
                    drawerBackCallback.isEnabled = true
                }
            }
        if (drawerLayout is ClosableDrawerLayout) {
            (drawerLayout as ClosableDrawerLayout).setAnimationEnabled(animationEnabled())
        } else {
            Timber.w("Unexpected Drawer layout - could not modify navigation animation")
        }
        drawerToggle.isDrawerSlideAnimationEnabled = animationEnabled()
        drawerToggle.drawerArrowDrawable = MenuSymbolDrawable(supportActionBar?.themedContext ?: this)
        drawerLayout.addDrawerListener(drawerToggle)
        if (!hasNavigationDrawer) {
            // Nothing to open: the navigation button goes back, and screens without one hide it
            showBackIcon()
        }

        enablePostShortcut(this)
    }

    /**
     * Edge to edge: the header image and rows extend to the window edge. The text labels
     * are not drawn underneath the system UI.
     */
    protected fun applyDrawerInsets(insets: WindowInsetsCompat) {
        val drawer = navigationView ?: return
        val contentWidth = resources.getDimensionPixelSize(R.dimen.nav_drawer_width)
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val rtl = drawer.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val start = if (rtl) bars.right else bars.left
        drawer.updatePadding(
            left = if (rtl) 0 else start,
            top = bars.top,
            right = if (rtl) start else 0,
            bottom = bars.bottom,
        )
        if (drawer.layoutParams.width != contentWidth + start) drawer.updateLayoutParams { width = contentWidth + start }
    }

    /**
     * A method which allows control of the ordering of adding the [OnBackPressedCallback]
     * for the drawer.
     *
     * Typically, call super after initialization code, so the drawer takes priority
     */
    @CallSuper
    protected open fun setupBackPressedCallbacks() {
        onBackPressedDispatcher.addCallback(this, drawerBackCallback)
    }

    /**
     * Sets selected navigation drawer item
     */
    protected fun selectNavigationItem(itemId: Int) {
        navigationView?.select(itemId)
    }

    override fun setTitle(title: CharSequence) {
        if (supportActionBar != null) {
            supportActionBar!!.title = title
        }
        super.setTitle(title)
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState()
    }

    private val preferences: SharedPreferences
        get() = this@NavigationDrawerActivity.sharedPrefs()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass any configuration change to the drawer toggles
        if (::drawerToggle.isInitialized) {
            drawerToggle.onConfigurationChanged(newConfig)
        }
    }

    /**
     * This function locks the navigation drawer closed in regards to swipes,
     * but continues to allowed it to be opened via it's indicator button. This
     * function in a noop if the drawer hasn't been initialized.
     */
    protected fun disableDrawerSwipe() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    /**
     * This function allows swipes to open the navigation drawer. This
     * function in a noop if the drawer hasn't been initialized.
     */
    protected fun enableDrawerSwipe() {
        if (!hasNavigationDrawer) return
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private val preferencesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Timber.i(
                "Handling Activity Result: %d. Result: %d",
                REQUEST_PREFERENCES_UPDATE,
                result.resultCode,
            )

            // We trigger a notifications channel set-up since the user may have changed the locale set
            // from within the app, which should cause the notification channel names to be reloaded to
            // match the new locale
            setupNotificationChannels(applicationContext)
            ActivityCompat.recreate(this)
        }

    /**
     * Called, when navigation button of the action bar is pressed.
     * Design pattern: template method. Subclasses can override this to define their own behaviour.
     */
    open fun onNavigationPressed() {
        if (navButtonGoesBack) {
            finish()
        } else {
            openDrawer()
        }
    }

    fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Don't do anything if user selects already selected position
        if (item.isChecked) {
            closeDrawer()
            return true
        }

        /*
         * This runnable will be executed in onDrawerClosed(...)
         * to make the animation more fluid on older devices.
         */
        pendingRunnable =
            Runnable {
                // Take action if a different item selected
                when (item.itemId) {
                    R.id.nav_decks -> {
                        Timber.i("Navigating to decks")
                        val deckPicker = Intent(this@NavigationDrawerActivity, DeckPicker::class.java)
                        // opening DeckPicker should use the instance on the back stack & clear back history
                        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(deckPicker)
                    }

                    R.id.nav_browser -> {
                        Timber.i("Navigating to card browser")
                        openCardBrowser()
                    }

                    R.id.nav_stats -> {
                        Timber.i("Navigating to stats")
                        openStatistics()
                    }

                    R.id.nav_settings -> {
                        Timber.i("Navigating to settings")
                        openSettings()
                    }

                    R.id.nav_help -> {
                        Timber.i("Navigating to help")
                        openHelp()
                    }

                    R.id.nav_about -> {
                        Timber.i("Navigating to About")
                        openAbout()
                    }
                }
            }
        closeDrawer()
        return true
    }

    protected fun openCardBrowser() {
        navigate(BrowserDestination.Open)
    }

    /**
     * Opens the Statistics Screen.
     */
    protected fun openStatistics() {
        Timber.i("launching statistics")
        navigate(StatisticsDestination)
    }

    /**
     * Opens AnkiDroid's Settings Screen.
     * Reached from the drawer on large screens and from More on phones.
     */
    fun openSettings() {
        preferencesLauncher.navigate(PreferencesDestination.Root)
    }

    /** Opens the help menu. Reached from the drawer on large screens and from More on phones. */
    fun openHelp() {
        showDialogFragment(HelpDialog.newHelpInstance())
    }

    fun openAbout() {
        preferencesLauncher.navigate(PreferencesDestination.About)
    }

    /**
     * Hides the navigation drawer indicator (hamburger icon) and any back arrows
     * from the toolbar. Used when bottom navigation is active.
     */
    protected fun disableDrawerIndicator() {
        drawerToggle.isDrawerIndicatorEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        navButtonGoesBack = false
    }

    protected fun showBackIcon() {
        drawerToggle.isDrawerIndicatorEnabled = false
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        navButtonGoesBack = true
    }

    protected fun restoreDrawerIcon() {
        if (!hasNavigationDrawer) {
            showBackIcon()
            return
        }
        drawerToggle.isDrawerIndicatorEnabled = true
        navButtonGoesBack = false
    }

    @VisibleForTesting
    open val isDrawerOpen: Boolean
        get() = drawerLayout.isDrawerOpen(GravityCompat.START)

    /** Opens the drawer if closed, closes it if open */
    @Suppress("unused")
    fun toggleDrawer() {
        if (!isDrawerOpen) {
            openDrawer()
        } else {
            closeDrawer()
        }
    }

    private fun openDrawer() {
        if (!hasNavigationDrawer) return
        drawerLayout.openDrawer(GravityCompat.START, animationEnabled())
    }

    private fun closeDrawer() {
        if (!hasNavigationDrawer) return
        drawerLayout.closeDrawer(GravityCompat.START, animationEnabled())
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (!isDrawerOpen) {
            return super.onKeyDown(keyCode, event)
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            closeDrawer()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        // Intent request codes
        const val REQUEST_PREFERENCES_UPDATE = 100
        const val PREF_FULL_SCREEN_NAVIGATION_DRAWER = "gestureFullScreenNavigationDrawer"

        const val EXTRA_STARTED_WITH_SHORTCUT = "com.ichi2.anki.StartedWithShortcut"

        // The main reason to use dynamic shortcut is that it ensures that they are only available when permission is granted to access the collection.
        // Even if we were going to ensure that all shortcut can deal nicely with missing permission, we probably would still need to use dynamic shortcuts because:
        // * this ensure we don't offer shortcut until we have permission to access the collection
        // * to use static shortcut, the package name must be given in res/shortcuts.xml. See https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts#static
        // * AnkiDroid package name is not constant. For example in debug variant, it is com.ichi2.anki.debug
        // * having variables in shortcuts used to be doable with https://plugins.gradle.org/plugin/de.timfreiheit.resourceplaceholders, however
        // * after manually testing it, and looking at open issue https://github.com/timfreiheit/ResourcePlaceholdersPlugin/issues/13 , it seems this was broken with recent version of gradle
        fun enablePostShortcut(context: Context) {
            if (runCatching { grantedStoragePermissions(context, showToast = false) }.getOrNull() != true) {
                Timber.w("No storage access, not enabling shortcuts")
                return
            }
            // Review Cards Shortcut
            val reviewCardsShortcut =
                ShortcutInfoCompat
                    .Builder(context, "reviewCardsShortcutId")
                    .setShortLabel(context.getString(CommonString.studyoptions_start))
                    .setLongLabel(context.getString(CommonString.studyoptions_start))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.review_shortcut))
                    .setIntent(IntentHandler.getReviewShortcutIntent(context))
                    .build()

            // Add Shortcut
            val intentAddNote = Intent(context, IntentHandler2::class.java)
            intentAddNote.action = Intent.ACTION_VIEW
            intentAddNote.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            intentAddNote.putExtra(NoteEditorFragment.EXTRA_CALLER, NoteEditorCaller.DECKPICKER.value)
            val noteEditorShortcut =
                ShortcutInfoCompat
                    .Builder(context, "noteEditorShortcutId")
                    .setShortLabel(context.getString(CommonString.menu_add))
                    .setLongLabel(context.getString(CommonString.menu_add))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.add_shortcut))
                    .setIntent(intentAddNote)
                    .build()

            // CardBrowser Shortcut
            val cardBrowserShortcut =
                ShortcutInfoCompat
                    .Builder(context, "cardBrowserShortcutId")
                    .setShortLabel(context.getString(CommonString.card_browser))
                    .setLongLabel(context.getString(CommonString.card_browser))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.browse_shortcut))
                    .setIntent(IntentHandler.getBrowseShortcutIntent(context))
                    .build()
            ShortcutManagerCompat.addDynamicShortcuts(
                context,
                listOf(
                    reviewCardsShortcut,
                    noteEditorShortcut,
                    cardBrowserShortcut,
                ),
            )
        }
    }
}

fun Fragment.requireNavigationDrawerActivity() = (requireActivity() as NavigationDrawerActivity)

/**
 * The drawer button's icon: the menu Material Symbol, in place of the hamburger
 * [DrawerArrowDrawable] draws in code. It does not turn into an arrow while the drawer slides.
 */
private class MenuSymbolDrawable(
    context: Context,
) : DrawerArrowDrawable(context) {
    private val symbol = AppCompatResources.getDrawable(context, R.drawable.symbol_menu)!!.mutate()

    override fun draw(canvas: Canvas) {
        symbol.bounds = bounds
        symbol.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        symbol.alpha = alpha
    }
}
