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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.DrawingFragment
import com.ichi2.anki.NoteEditorActivity
import com.ichi2.anki.NoteEditorPaneFragment
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.AccountFlowFragment
import com.ichi2.anki.filtered.FilteredDeckOptionsFragment
import com.ichi2.anki.introduction.WelcomeFragment
import com.ichi2.anki.mediacheck.MediaCheckFragment
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
import com.ichi2.anki.study.FinishedFragment
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
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
            fragmentScreens.values.forEach { fragmentClass ->
                composable(fragmentClass.name) { entry ->
                    HostedScreen(entry, fragmentClass)
                }
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
    ) {
        val manager = deckPicker.supportFragmentManager
        val tag = "navigation:${entry.id}"
        val containerId = rememberSaveable { allocateContainerId() }
        val container =
            remember(containerId) {
                FragmentContainerView(deckPicker).apply { id = containerId }
            }
        AndroidView(factory = { container }, modifier = Modifier.fillMaxSize())

        DisposableEffect(container) {
            val existing = manager.findFragmentByTag(tag)
            val fragment =
                existing ?: manager.fragmentFactory.instantiate(deckPicker.classLoader, fragmentClass.name).apply {
                    arguments = entry.arguments?.getBundle(SCREEN_ARGUMENTS)
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
        val result = completedResults.remove(entry.id) ?: ActivityResult(Activity.RESULT_CANCELED, null)
        val requestCode = entry.arguments?.getInt(RESULT_REQUEST_CODE, NO_RESULT) ?: NO_RESULT
        if (requestCode != NO_RESULT) deckPicker.activityResultRegistry.dispatchResult(requestCode, result.resultCode, result.data)
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
                NoteEditorActivity::class.java.name -> NoteEditorPaneFragment::class.java
                else -> fragmentScreens[intent.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME)] ?: return false
            }
        val screenArguments =
            if (component == NoteEditorActivity::class.java.name) NoteEditorPaneFragment.arguments(intent)
            else intent.getBundleExtra(SingleFragmentActivity.EXTRA_FRAGMENT_ARGS)
        val arguments = bundleOf(SCREEN_ARGUMENTS to screenArguments, RESULT_REQUEST_CODE to requestCode)
        navigateWhenReady { it.navigate(it.graph[fragment.name].id, arguments) }
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

        val hostedComponents =
            setOf(
                AccountActivity::class.java.name,
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
                CardInfoFragment::class.java,
                CsvImporter::class.java,
                DeckOptions::class.java,
                DrawingFragment::class.java,
                FilteredDeckOptionsFragment::class.java,
                FinishedFragment::class.java,
                ImageCropper::class.java,
                ImageOcclusion::class.java,
                MediaCheckFragment::class.java,
                NoteEditorPaneFragment::class.java,
                PreferencesFragment::class.java,
                PreviewerFragment::class.java,
                ReviewerFragment::class.java,
                ScheduleRemindersFragment::class.java,
                Statistics::class.java,
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
