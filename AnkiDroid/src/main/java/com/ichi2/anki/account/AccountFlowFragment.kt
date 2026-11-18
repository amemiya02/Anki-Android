// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.ichi2.anki.R
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.navigation.SubscreenNavigation
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.preferences.CustomSyncServerSettingsFragment
import com.ichi2.anki.utils.bottomCornerClearance

/** Keeps the server and credential steps on one route, including their shared result. */
class AccountFlowFragment : Fragment() {
    private val navigation = SubscreenNavigation(this)

    internal var bottomCornerClearancePx by mutableIntStateOf(0)
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(inflater.context).apply {
            id = R.id.account_flow_step
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                bottomCornerClearancePx = insets.bottomCornerClearance(view)
                insets
            }
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val first = if (requireArguments().getBoolean(START_WITH_ADDRESS)) CustomSyncServerSettingsFragment() else LoginFragment()
            setContent { navigation.Content(first) }
        }

    internal fun showSignIn() {
        if (isLoggedIn()) completeSignIn() else navigation.open(LoginFragment())
    }

    internal fun changeServer() {
        if (!navigation.pop()) navigation.open(CustomSyncServerSettingsFragment(), replaceAll = true)
    }

    internal fun completeSignIn() {
        finishScreen(if (requireArguments().getBoolean(FOR_RESULT)) Activity.RESULT_OK else Activity.RESULT_CANCELED)
    }

    companion object {
        internal const val START_WITH_ADDRESS = "startWithAddress"
        private const val FOR_RESULT = "START_FOR_RESULT"

        internal fun arguments(
            startWithAddress: Boolean,
            forResult: Boolean,
        ) = Bundle().apply {
            putBoolean(START_WITH_ADDRESS, startWithAddress)
            putBoolean(FOR_RESULT, forResult)
        }
    }
}
