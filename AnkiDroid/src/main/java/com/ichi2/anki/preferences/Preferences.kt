// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.ichi2.anki.R
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.android.Animations
import com.ichi2.anki.utils.isWindowCompact
import com.ichi2.utils.FragmentFactoryUtils
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class PreferencesFragment : Fragment(R.layout.fragment_preferences) {
    internal val settingsIsSplit get() = !resources.isWindowCompact()

    private val childBack =
        object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                childFragmentManager.popBackStack()
            }
        }

    private val childBackStackListener = FragmentManager.OnBackStackChangedListener { updateActivePage() }

    private val childLifecycleListener =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager,
                fragment: Fragment,
                view: View,
                savedInstanceState: Bundle?,
            ) {
                if (fragment.id == R.id.settings_container) updateActivePage()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        ViewGroupCompat.installCompatInsetsDispatch(view)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, childBack)
        childFragmentManager.addOnBackStackChangedListener(childBackStackListener)
        childFragmentManager.registerFragmentLifecycleCallbacks(childLifecycleListener, false)
        if (savedInstanceState == null) {
            val initialClass = arguments?.getString(EXTRA_INITIAL_FRAGMENT)
            val initial =
                when {
                    initialClass != null && !(settingsIsSplit && initialClass == HeaderFragment::class.jvmName) ->
                        FragmentFactoryUtils.instantiate<Fragment>(requireActivity(), initialClass)
                    settingsIsSplit -> GeneralSettingsFragment()
                    else -> HeaderFragment()
                }
            showPage(initial)
        }
        updateActivePage()
    }

    override fun onDestroyView() {
        childFragmentManager.unregisterFragmentLifecycleCallbacks(childLifecycleListener)
        childFragmentManager.removeOnBackStackChangedListener(childBackStackListener)
        super.onDestroyView()
    }

    private fun updateActivePage() {
        childBack.isEnabled = childFragmentManager.backStackEntryCount > 0
        val current = childFragmentManager.findFragmentById(R.id.settings_container)
        (childFragmentManager.findFragmentById(R.id.lateral_nav_container) as? HeaderFragment)
            ?.select(current?.let(SettingsGroup::forFragment))
    }

    internal fun openGroup(group: SettingsGroup) {
        val fragment = FragmentFactoryUtils.instantiate<Fragment>(requireActivity(), group.destination.jvmName)
        openPrimaryPage(fragment)
    }

    internal fun openSearchResult(result: SettingsSearchEntry) {
        val fragment = FragmentFactoryUtils.instantiate<Fragment>(requireActivity(), result.destination.jvmName)
        fragment.arguments = Bundle().apply { putString(SETTINGS_SEARCH_TARGET, result.key) }
        val group = SettingsGroup.forFragment(fragment)
        if (settingsIsSplit && group != null && group.destination != result.destination) {
            openGroup(group)
            openSubpage(fragment)
        } else {
            openPrimaryPage(fragment)
        }
    }

    internal fun openSubpage(fragment: Fragment) {
        fragment.arguments = Bundle(fragment.arguments ?: Bundle.EMPTY).apply { putBoolean(SETTINGS_IS_SUBPAGE, true) }
        showPage(fragment, addToBackStack = true)
    }

    internal fun refreshSearchIndex() {
        (childFragmentManager.findFragmentById(R.id.lateral_nav_container) as? HeaderFragment)?.refreshSearchIndex()
    }

    private fun openPrimaryPage(fragment: Fragment) {
        if (settingsIsSplit) childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showPage(fragment, addToBackStack = !settingsIsSplit)
    }

    private fun showPage(
        fragment: Fragment,
        addToBackStack: Boolean = false,
    ) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            if (Animations.areAnimationsEnabled(requireContext())) setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(R.id.settings_container, fragment, fragment.javaClass.name)
            if (addToBackStack) addToBackStack(null)
        }
    }
}

class PreferencesActivity : SingleFragmentActivity() {
    companion object {
        /** Builds an [Intent] which opens [PreferencesFragment], optionally at [initialFragment]. */
        fun getIntent(
            context: Context,
            initialFragment: KClass<out Fragment>? = null,
        ): Intent =
            Intent(context, PreferencesActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_NAME, PreferencesFragment::class.jvmName)
                putExtra(EXTRA_FRAGMENT_ARGS, Bundle().apply { putString(EXTRA_INITIAL_FRAGMENT, initialFragment?.jvmName) })
            }
    }
}

// Only enable AnkiDroid notifications unrelated to due reminders

const val EXTRA_INITIAL_FRAGMENT = "initial_fragment"
