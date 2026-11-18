// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.navigation

import android.os.Bundle
import android.view.View
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get

/** Shares the Android fragment lifecycle adapter used by Compose navigation in the app. */
internal class FragmentDestinations(
    private val manager: FragmentManager,
) {
    private val fragments = mutableMapOf<String, Fragment>()

    fun find(entry: NavBackStackEntry?): Fragment? = entry?.id?.let(fragments::get)

    fun remove(entry: NavBackStackEntry) {
        val fragment = fragments.remove(entry.id) ?: manager.findFragmentByTag(entry.tag)
        if (fragment?.isAdded == true && !manager.isStateSaved) {
            manager.commit { remove(fragment) }
        }
    }

    @Composable
    fun Content(
        entry: NavBackStackEntry,
        fragmentClass: String,
        arguments: Bundle?,
        modifier: Modifier = Modifier,
    ) {
        val context = LocalContext.current
        val tag = entry.tag
        val containerId =
            remember(tag) {
                manager.findFragmentByTag(tag)?.id ?: generateSequence(View::generateViewId)
                    .first { manager.findFragmentById(it) == null }
            }
        val container =
            remember(containerId) {
                FragmentContainerView(context).apply {
                    id = containerId
                    isNestedScrollingEnabled = true
                }
            }
        AndroidView(factory = { container }, modifier = modifier.fillMaxSize())

        DisposableEffect(container) {
            val existing = manager.findFragmentByTag(tag)
            val fragment =
                existing ?: manager.fragmentFactory.instantiate(context.classLoader, fragmentClass).apply {
                    this.arguments = arguments
                }
            fragments[entry.id] = fragment
            if (!manager.isStateSaved) {
                manager.commitNow {
                    setReorderingAllowed(true)
                    if (existing == null) add(container, fragment, tag) else manager.onContainerAvailable(container)
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

    private val NavBackStackEntry.tag get() = "navigation:$id"
}

/** A Compose back stack for the steps or pages of one screen. */
internal class SubscreenNavigation(
    owner: Fragment,
) {
    private val destinations by lazy { FragmentDestinations(owner.childFragmentManager) }
    private var controller: NavHostController? = null
    private val pending = ArrayDeque<(NavHostController) -> Unit>()

    val currentFragment get() = destinations.find(controller?.currentBackStackEntry)

    fun pop(): Boolean = controller?.previousBackStackEntry != null && controller?.popBackStack() == true

    fun open(
        fragment: Fragment,
        replaceAll: Boolean = false,
    ) {
        val operation: (NavHostController) -> Unit = { navigation ->
            val arguments =
                Bundle().apply {
                    putString(CLASS, fragment.javaClass.name)
                    putBundle(ARGUMENTS, fragment.arguments)
                }
            navigation.navigate(
                navigation.graph[PAGE].id,
                arguments,
                androidx.navigation.navOptions {
                    if (replaceAll) popUpTo(navigation.graph.id)
                },
            )
        }
        val navigation = controller
        if (navigation == null) pending.add(operation) else operation(navigation)
    }

    @Composable
    fun Content(
        initial: Fragment,
        onPageChanged: (Fragment?) -> Unit = {},
    ) {
        val navigation = rememberNavController()
        NavHost(
            navController = navigation,
            startDestination = INITIAL,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(INITIAL) { destinations.Content(it, initial.javaClass.name, initial.arguments) }
            composable(PAGE) {
                val arguments = requireNotNull(it.arguments)
                destinations.Content(it, requireNotNull(arguments.getString(CLASS)), arguments.getBundle(ARGUMENTS))
            }
        }
        LaunchedEffect(navigation) {
            var previousEntries = emptyList<NavBackStackEntry>()
            navigation.currentBackStack.collect { entries ->
                val popped = previousEntries.filterNot { entry -> entries.any { it.id == entry.id } }
                previousEntries = entries
                popped.forEach(destinations::remove)
            }
        }
        SideEffect {
            controller = navigation
            if (pending.isNotEmpty()) {
                while (pending.isNotEmpty()) pending.removeFirst()(navigation)
            } else {
                onPageChanged(currentFragment)
            }
        }
        DisposableEffect(navigation) {
            onDispose { if (controller === navigation) controller = null }
        }
    }

    private companion object {
        const val INITIAL = "initial"
        const val PAGE = "page"
        const val CLASS = "fragmentClass"
        const val ARGUMENTS = "arguments"
    }
}
