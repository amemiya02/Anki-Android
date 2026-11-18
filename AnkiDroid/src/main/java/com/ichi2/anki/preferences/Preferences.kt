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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.R
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.android.Animations
import com.ichi2.anki.navigation.SubscreenNavigation
import com.ichi2.anki.utils.isWindowCompact
import com.ichi2.utils.FragmentFactoryUtils
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import com.ichi2.anki.common.android.R as CommonR

class PreferencesFragment : Fragment() {
    internal val settingsIsSplit get() = !resources.isWindowCompact()
    private val navigation = SubscreenNavigation(this)

    internal val currentPage: Fragment?
        get() = if (settingsIsSplit) childFragmentManager.findFragmentById(R.id.settings_container) else navigation.currentFragment

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = inflater.context
        if (!settingsIsSplit) {
            return ComposeView(context).apply {
                id = R.id.settings_container
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                val first = initialPage()
                setContent { navigation.Content(first) }
            }
        }
        val detail = FragmentContainerView(context).apply { id = R.id.settings_container }

        val density = resources.displayMetrics.density
        val headerWidth = (resources.configuration.screenWidthDp / 3).coerceIn(320, 400)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(FragmentContainerView(context).apply { id = R.id.lateral_nav_container }, (headerWidth * density).toInt(), MATCH_PARENT)
            addView(
                View(context).apply { setBackgroundColor(MaterialColors.getColor(this, CommonR.attr.foundationOutline)) },
                density.toInt().coerceAtLeast(1),
                MATCH_PARENT,
            )
            addView(detail, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        ViewGroupCompat.installCompatInsetsDispatch(view)
        if (!settingsIsSplit) return
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, childBack)
        childFragmentManager.addOnBackStackChangedListener(childBackStackListener)
        childFragmentManager.registerFragmentLifecycleCallbacks(childLifecycleListener, false)
        if (settingsIsSplit && childFragmentManager.findFragmentById(R.id.lateral_nav_container) == null) {
            childFragmentManager.commit { replace(R.id.lateral_nav_container, HeaderFragment()) }
        }
        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            showPage(initialPage())
        }
        updateActivePage()
    }

    private fun initialPage(): Fragment {
        val initialClass = arguments?.getString(EXTRA_INITIAL_FRAGMENT)
        return when {
            initialClass != null && !(settingsIsSplit && initialClass == HeaderFragment::class.jvmName) ->
                FragmentFactoryUtils.instantiate<Fragment>(requireActivity(), initialClass)
            settingsIsSplit -> GeneralSettingsFragment()
            else -> HeaderFragment()
        }
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

    internal fun popPage(): Boolean {
        if (!settingsIsSplit) return navigation.pop()
        if (childFragmentManager.backStackEntryCount == 0) return false
        childFragmentManager.popBackStack()
        return true
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
        if (!settingsIsSplit) {
            navigation.open(fragment)
            return
        }
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
