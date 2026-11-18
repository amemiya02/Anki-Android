// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.navigation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import androidx.navigation.navOptions
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.BrowserAppearanceFragment
import com.ichi2.anki.CardTemplateBrowserAppearanceEditor
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.Info
import com.ichi2.anki.InfoFragment
import com.ichi2.anki.MoreFragment
import com.ichi2.anki.DrawingFragment
import com.ichi2.anki.NoteEditorActivity
import com.ichi2.anki.NoteEditorPaneFragment
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.StudyOptionsActivity
import com.ichi2.anki.StudyOptionsFlowFragment
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.AccountFlowFragment
import com.ichi2.anki.browser.CardBrowserFragment
import com.ichi2.anki.browser.CardBrowserPaneFragment
import com.ichi2.anki.ensureBrowserViewModel
import com.ichi2.anki.filtered.FilteredDeckOptionsFragment
import com.ichi2.anki.introduction.WelcomeFragment
import com.ichi2.anki.mediacheck.MediaCheckFragment
import com.ichi2.anki.multimedia.MultimediaActivity
import com.ichi2.anki.multimedia.MultimediaFlowFragment
import com.ichi2.anki.notetype.ManageNoteTypesFragment
import com.ichi2.anki.notetype.ManageNotetypes
import com.ichi2.anki.ui.windows.managespace.ManageSpaceActivity
import com.ichi2.anki.ui.windows.managespace.ManageSpaceFragment
import com.ichi2.anki.pages.AnkiPackageImporterFragment
import com.ichi2.anki.pages.CardInfoFragment
import com.ichi2.anki.pages.CsvImporter
import com.ichi2.anki.pages.DeckOptions
import com.ichi2.anki.pages.ImageOcclusion
import com.ichi2.anki.pages.Statistics
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.preferences.PreferencesFragment
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.previewer.PreviewerFragment
import com.ichi2.anki.previewer.TemplatePreviewerFragment
import com.ichi2.anki.reviewreminders.ScheduleRemindersFragment
import com.ichi2.anki.shareddeck.SharedDecksActivity
import com.ichi2.anki.shareddeck.SharedDecksFragment
import com.ichi2.anki.study.FinishedFragment
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import com.ichi2.anki.updateTabChrome
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import com.ichi2.imagecropper.ImageCropper

/** Owns the in-app back stack and keeps platform entry intents at the Android boundary. */
internal class AppNavigation(
    private val deckPicker: DeckPicker,
    private val home: View,
    private val showWelcome: Boolean,
) {
    private var controller: NavHostController? = null
    private val pending = ArrayDeque<(NavHostController) -> Unit>()
    private val fragments = mutableMapOf<String, Fragment>()
    private val completedResults = mutableMapOf<String, ActivityResult>()
    private val forwardedResults = mutableSetOf<String>()

    val currentFragment: Fragment?
        get() = controller?.currentBackStackEntry?.id?.let(fragments::get)

    @Composable
    fun Content() {
        val navigation = rememberNavController()
        NavHost(
            navController = navigation,
            startDestination = if (showWelcome) WELCOME else HOME,
            modifier = Modifier.fillMaxSize(),
            // Fragment-backed screens own their view lifecycle and system insets.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(HOME) {
                AndroidView(
                    factory = {
                        (home.parent as? ViewGroup)?.removeView(home)
                        home
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable(WELCOME) {
                AndroidFragment<WelcomeFragment>(Modifier.fillMaxSize())
            }
            tabFragments.forEach { (tab, fragmentClass) ->
                composable(tab.tag) { entry ->
                    if (tab == NavigationItem.BROWSER) deckPicker.ensureBrowserViewModel()
                    val bottomClearance = with(LocalDensity.current) { deckPicker.bottomNavContentClearance.intValue.toDp() }
                    val arguments = if (tab == NavigationItem.STATS) bundleOf(Statistics.ARG_HIDE_BACK_BUTTON to true) else null
                    HostedScreen(entry, fragmentClass, Modifier.padding(bottom = bottomClearance), arguments)
                }
            }
            fragmentScreens.values.forEach { fragmentClass ->
                composable(fragmentClass.name) { entry ->
                    HostedScreen(entry, fragmentClass)
                }
            }
        }
        val route = navigation.currentBackStackEntryAsState().value?.destination?.route
        LaunchedEffect(route) {
            if (route in tabRouteNames && !deckPicker.bottomNavigationEnabled && route != HOME) {
                navigation.popBackStack(HOME, inclusive = false)
            } else {
                deckPicker.updateTabChrome(NavigationItem.entries.find { it.tag == route })
            }
        }
        LaunchedEffect(navigation) {
            var previousEntries = emptyList<NavBackStackEntry>()
            navigation.currentBackStack.collect { entries ->
                val popped = previousEntries.filterNot { entry -> entries.any { it.id == entry.id } }
                previousEntries = entries
                popped.forEach(::onEntryPopped)
            }
        }
        SideEffect {
            controller = navigation
            while (pending.isNotEmpty()) pending.removeFirst()(navigation)
        }
    }

    /**
     * Preserve a covered Fragment's ViewModel and registered Activity Result keys.
     * Its view and back callbacks are active only while its destination is presented.
     */
    @Composable
    private fun HostedScreen(
        entry: NavBackStackEntry,
        fragmentClass: Class<out Fragment>,
        modifier: Modifier = Modifier,
        screenArguments: Bundle? = entry.arguments?.getBundle(SCREEN_ARGUMENTS),
    ) {
        val manager = deckPicker.supportFragmentManager
        val tag = "navigation:${entry.id}"
        val containerId = rememberSaveable { allocateContainerId() }
        val container =
            remember(containerId) {
                FragmentContainerView(deckPicker).apply { id = containerId }
            }
        AndroidView(factory = { container }, modifier = modifier.fillMaxSize())

        DisposableEffect(container) {
            val existing = manager.findFragmentByTag(tag)
            val fragment =
                existing ?: manager.fragmentFactory.instantiate(deckPicker.classLoader, fragmentClass.name).apply {
                    arguments = screenArguments
                }
            fragments[entry.id] = fragment
            if (!manager.isStateSaved) {
                manager.commitNow {
                    setReorderingAllowed(true)
                    if (existing == null) {
                        add(container, fragment, tag)
                    } else {
                        manager.onContainerAvailable(container)
                    }
                    setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
                }
            }
            onDispose {
                if (!manager.isStateSaved && !manager.isDestroyed && fragment.isAdded) {
                    manager.commitNow { setMaxLifecycle(fragment, Lifecycle.State.CREATED) }
                }
            }
        }
    }

    private fun allocateContainerId(): Int {
        val manager = deckPicker.supportFragmentManager
        var id = View.generateViewId()
        while (manager.findFragmentById(id) != null) id = View.generateViewId()
        return id
    }

    private fun onEntryPopped(entry: NavBackStackEntry) {
        val fragment = fragments.remove(entry.id) ?: deckPicker.supportFragmentManager.findFragmentByTag("navigation:${entry.id}")
        if (fragment != null && fragment.isAdded && !deckPicker.supportFragmentManager.isStateSaved) {
            deckPicker.supportFragmentManager.commit { remove(fragment) }
        }
        if (forwardedResults.remove(entry.id)) return
        val defaultResult = entry.arguments?.getInt(DEFAULT_RESULT, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val result = completedResults.remove(entry.id) ?: ActivityResult(defaultResult, null)
        val requestCode = entry.arguments?.getInt(RESULT_REQUEST_CODE, NO_RESULT) ?: NO_RESULT
        if (requestCode != NO_RESULT) deckPicker.activityResultRegistry.dispatchResult(requestCode, result.resultCode, result.data)
    }

    fun selectTab(tab: NavigationItem) {
        navigateWhenReady { navigation ->
            if (navigation.currentDestination?.route == tab.tag) return@navigateWhenReady
            if (tab == NavigationItem.HOME) {
                navigation.popBackStack(HOME, inclusive = false)
            } else {
                navigation.navigate(tab.tag) {
                    popUpTo(HOME)
                    launchSingleTop = true
                }
            }
        }
    }

    fun completeWelcome() {
        navigateWhenReady { navigation ->
            navigation.navigate(HOME) {
                popUpTo(WELCOME) { inclusive = true }
            }
        }
    }

    fun launch(
        intent: Intent,
        requestCode: Int = NO_RESULT,
    ): Boolean {
        if (intent.component?.packageName != deckPicker.packageName) return false
        if (intent.component?.className !in hostedComponents) return false
        val component = intent.component?.className
        val fragment =
            when (component) {
                CardBrowser::class.java.name -> CardBrowserPaneFragment::class.java
                CardTemplateBrowserAppearanceEditor::class.java.name -> BrowserAppearanceFragment::class.java
                ManageNotetypes::class.java.name -> ManageNoteTypesFragment::class.java
                ManageSpaceActivity::class.java.name -> ManageSpaceFragment::class.java
                Info::class.java.name -> InfoFragment::class.java
                SharedDecksActivity::class.java.name -> SharedDecksFragment::class.java
                StudyOptionsActivity::class.java.name -> StudyOptionsFlowFragment::class.java
                MultimediaActivity::class.java.name -> MultimediaFlowFragment::class.java
                NoteEditorActivity::class.java.name -> NoteEditorPaneFragment::class.java
                else -> fragmentScreens[intent.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME)] ?: return false
            }
        val screenArguments =
            when (component) {
                CardBrowser::class.java.name -> CardBrowserPaneFragment.arguments(intent)
                NoteEditorActivity::class.java.name -> NoteEditorPaneFragment.arguments(intent)
                CardTemplateBrowserAppearanceEditor::class.java.name,
                MultimediaActivity::class.java.name,
                -> intent.extras
                else -> intent.getBundleExtra(SingleFragmentActivity.EXTRA_FRAGMENT_ARGS)
            }
        val defaultResult = if (component == CardBrowser::class.java.name) Activity.RESULT_OK else Activity.RESULT_CANCELED
        navigateWhenReady { navigation ->
            val forward = intent.flags and Intent.FLAG_ACTIVITY_FORWARD_RESULT != 0
            val caller = navigation.currentBackStackEntry?.takeIf { forward && navigation.previousBackStackEntry != null }
            val outgoingRequest = caller?.arguments?.getInt(RESULT_REQUEST_CODE, NO_RESULT) ?: requestCode
            val options =
                caller?.let {
                    forwardedResults.add(it.id)
                    navOptions { popUpTo(it.destination.id) { inclusive = true } }
                }
            val arguments =
                bundleOf(
                    SCREEN_ARGUMENTS to screenArguments,
                    RESULT_REQUEST_CODE to outgoingRequest,
                    DEFAULT_RESULT to defaultResult,
                )
            navigation.navigate(navigation.graph[fragment.name].id, arguments, options)
        }
        return true
    }

    fun finish(
        source: Fragment,
        result: ActivityResult,
    ) {
        val navigation = controller ?: return
        if (!isCurrentScreen(source)) return
        val entry = navigation.currentBackStackEntry ?: return
        completedResults[entry.id] = result
        navigation.popBackStack()
    }

    fun hasResultCaller(fragment: Fragment): Boolean =
        isCurrentScreen(fragment) &&
            (controller?.currentBackStackEntry?.arguments?.getInt(RESULT_REQUEST_CODE, NO_RESULT) ?: NO_RESULT) != NO_RESULT

    private fun isCurrentScreen(fragment: Fragment): Boolean {
        val root = currentFragment ?: return false
        return generateSequence(fragment) { it.parentFragment }.any { it === root }
    }

    private fun navigateWhenReady(block: (NavHostController) -> Unit) {
        val navigation = controller
        if (navigation == null) pending.add(block) else block(navigation)
    }

    private companion object {
        const val HOME = "home"
        const val WELCOME = "welcome"
        const val NO_RESULT = -1
        const val SCREEN_ARGUMENTS = "screenArguments"
        const val RESULT_REQUEST_CODE = "resultRequestCode"
        const val DEFAULT_RESULT = "defaultResultCode"

        val tabFragments =
            mapOf(
                NavigationItem.BROWSER to CardBrowserFragment::class.java,
                NavigationItem.STATS to Statistics::class.java,
                NavigationItem.MORE to MoreFragment::class.java,
            )
        val tabRouteNames = NavigationItem.entries.map { it.tag }

        val hostedComponents =
            setOf(
                AccountActivity::class.java.name,
                CardBrowser::class.java.name,
                CardTemplateBrowserAppearanceEditor::class.java.name,
                ManageNotetypes::class.java.name,
                ManageSpaceActivity::class.java.name,
                Info::class.java.name,
                StudyOptionsActivity::class.java.name,
                SharedDecksActivity::class.java.name,
                MultimediaActivity::class.java.name,
                NoteEditorActivity::class.java.name,
                SingleFragmentActivity::class.java.name,
                ConfigAwareSingleFragmentActivity::class.java.name,
                PreferencesActivity::class.java.name,
                CardViewerActivity::class.java.name,
            )

        val fragmentScreens =
            listOf(
                AccountFlowFragment::class.java,
                AnkiPackageImporterFragment::class.java,
                BrowserAppearanceFragment::class.java,
                CardBrowserPaneFragment::class.java,
                CardInfoFragment::class.java,
                CsvImporter::class.java,
                DeckOptions::class.java,
                DrawingFragment::class.java,
                FilteredDeckOptionsFragment::class.java,
                FinishedFragment::class.java,
                ImageCropper::class.java,
                ImageOcclusion::class.java,
                InfoFragment::class.java,
                ManageNoteTypesFragment::class.java,
                ManageSpaceFragment::class.java,
                MediaCheckFragment::class.java,
                MultimediaFlowFragment::class.java,
                NoteEditorPaneFragment::class.java,
                PreferencesFragment::class.java,
                PreviewerFragment::class.java,
                ReviewerFragment::class.java,
                ScheduleRemindersFragment::class.java,
                Statistics::class.java,
                StudyOptionsFlowFragment::class.java,
                SharedDecksFragment::class.java,
                TemplatePreviewerFragment::class.java,
            ).associateBy { it.name }
    }
}

internal fun Fragment.finishScreen(
    resultCode: Int = Activity.RESULT_CANCELED,
    data: Intent? = null,
) {
    val host = requireActivity()
    if (host is DeckPicker) {
        host.finishScreen(this, ActivityResult(resultCode, data))
    } else {
        host.setResult(resultCode, data)
        host.finish()
    }
}

internal fun Fragment.hasScreenResultCaller(): Boolean {
    val host = requireActivity()
    return if (host is DeckPicker) host.hasScreenResultCaller(this) else host.callingActivity != null
}
