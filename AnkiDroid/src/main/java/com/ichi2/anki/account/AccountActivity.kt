// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ichi2.anki.R
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.preferences.CustomSyncServerSettingsFragment
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.preferences.SyncSettingsFragment
import com.ichi2.anki.syncServerAddress
import kotlin.reflect.KClass

/** Hosts both steps so the caller receives the sign-in result even when setup starts without a server. */
class AccountActivity : SingleFragmentActivity() {
    private val forResult: Boolean
        get() = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS)?.getBoolean(ARG_START_FOR_RESULT) == true

    internal fun showSignIn() {
        if (isLoggedIn()) {
            completeSignIn()
        } else {
            open(LoginFragment(), ADDRESS)
        }
    }

    internal fun changeServer() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            open(CustomSyncServerSettingsFragment())
        }
    }

    internal fun completeSignIn() {
        if (forResult) setResult(RESULT_OK)
        finish()
    }

    private fun open(
        fragment: Fragment,
        backStackName: String? = null,
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, FRAGMENT_TAG)
            if (backStackName != null) addToBackStack(backStackName)
        }
    }

    companion object {
        private const val ARG_START_FOR_RESULT = "START_FOR_RESULT"
        private const val ADDRESS = "sync_server_address"

        fun getIntent(
            context: Context,
            forResult: Boolean = false,
        ): Intent =
            when {
                syncServerAddress() == null -> getSetupIntent(context, forResult)
                isLoggedIn() && !forResult -> PreferencesActivity.getIntent(context, SyncSettingsFragment::class)
                isLoggedIn() -> getSetupIntent(context, forResult)
                else -> accountIntent(context, LoginFragment::class, forResult)
            }

        fun getSetupIntent(
            context: Context,
            forResult: Boolean = false,
        ): Intent = accountIntent(context, CustomSyncServerSettingsFragment::class, forResult)

        private fun accountIntent(
            context: Context,
            fragment: KClass<out Fragment>,
            forResult: Boolean,
        ): Intent =
            getIntent(
                context,
                fragment,
                Bundle().apply { putBoolean(ARG_START_FOR_RESULT, forResult) },
            ).setClass(context, AccountActivity::class.java)
    }
}
