// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.ichi2.anki.R
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.preferences.CustomSyncServerSettingsFragment
import com.ichi2.anki.utils.bottomCornerClearance

/** Keeps the server and credential steps on one route, including their shared result. */
class AccountFlowFragment : Fragment() {
    internal var bottomCornerClearancePx by mutableIntStateOf(0)
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        FragmentContainerView(inflater.context).apply {
            id = R.id.account_flow_step
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                bottomCornerClearancePx = insets.bottomCornerClearance(view)
                insets
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val manager = childFragmentManager
        val back =
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, manager.backStackEntryCount > 0) {
                manager.popBackStack()
            }
        val listener = FragmentManager.OnBackStackChangedListener { back.isEnabled = manager.backStackEntryCount > 0 }
        manager.addOnBackStackChangedListener(listener)
        viewLifecycleOwner.lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                    manager.removeOnBackStackChangedListener(listener)
                }
            },
        )
        if (manager.findFragmentById(R.id.account_flow_step) == null) {
            open(if (requireArguments().getBoolean(START_WITH_ADDRESS)) CustomSyncServerSettingsFragment() else LoginFragment())
        }
    }

    internal fun showSignIn() {
        if (isLoggedIn()) completeSignIn() else open(LoginFragment(), rememberAddress = true)
    }

    internal fun changeServer() {
        if (childFragmentManager.backStackEntryCount > 0) childFragmentManager.popBackStack() else open(CustomSyncServerSettingsFragment())
    }

    internal fun completeSignIn() {
        finishScreen(if (requireArguments().getBoolean(FOR_RESULT)) Activity.RESULT_OK else Activity.RESULT_CANCELED)
    }

    private fun open(
        fragment: Fragment,
        rememberAddress: Boolean = false,
    ) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.account_flow_step, fragment)
            if (rememberAddress) addToBackStack(null)
        }
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
