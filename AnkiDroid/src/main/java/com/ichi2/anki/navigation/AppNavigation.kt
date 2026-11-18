// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.navigation

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.introduction.WelcomeFragment
import com.ichi2.anki.mediacheck.MediaCheckFragment

/** Owns the in-app back stack and keeps platform entry intents at the Android boundary. */
internal class AppNavigation(
    private val deckPicker: DeckPicker,
    private val home: View,
    private val showWelcome: Boolean,
) {
    private var controller: NavHostController? = null
    private val pending = ArrayDeque<(NavHostController) -> Unit>()

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
            composable(MEDIA_CHECK) {
                AndroidFragment<MediaCheckFragment>(Modifier.fillMaxSize())
            }
        }
        SideEffect {
            controller = navigation
            while (pending.isNotEmpty()) pending.removeFirst()(navigation)
        }
    }

    fun completeWelcome() {
        navigateWhenReady { navigation ->
            navigation.navigate(HOME) {
                popUpTo(WELCOME) { inclusive = true }
            }
        }
    }

    fun launch(intent: Intent): Boolean {
        if (intent.component?.packageName != deckPicker.packageName) return false
        if (intent.component?.className != SingleFragmentActivity::class.java.name) return false
        val route =
            when (intent.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME)) {
                MediaCheckFragment::class.java.name -> MEDIA_CHECK
                else -> return false
            }
        navigateWhenReady { it.navigate(route) }
        return true
    }

    fun pop(): Boolean = controller?.popBackStack() == true

    private fun navigateWhenReady(block: (NavHostController) -> Unit) {
        val navigation = controller
        if (navigation == null) pending.add(block) else block(navigation)
    }

    private companion object {
        const val HOME = "home"
        const val WELCOME = "welcome"
        const val MEDIA_CHECK = "media-check"
    }
}

internal fun Fragment.closeScreen() {
    val host = requireActivity()
    if (host !is DeckPicker || !host.popScreen()) host.finish()
}
