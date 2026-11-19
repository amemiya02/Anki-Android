// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.preferences.SyncSettingsFragment
import com.ichi2.anki.syncServerAddress
import com.ichi2.anki.usesAnkiWeb

/** Hosts both steps so the caller receives the sign-in result even when setup starts without a server. */
class AccountActivity : SingleFragmentActivity() {
    companion object {
        fun getIntent(
            context: Context,
            forResult: Boolean = false,
        ): Intent =
            when {
                !usesAnkiWeb() && syncServerAddress() == null -> getSetupIntent(context, forResult)
                isLoggedIn() && !forResult -> PreferencesActivity.getIntent(context, SyncSettingsFragment::class)
                isLoggedIn() -> getSetupIntent(context, forResult)
                else -> accountIntent(context, false, forResult)
            }

        fun getSetupIntent(
            context: Context,
            forResult: Boolean = false,
        ): Intent = accountIntent(context, true, forResult)

        private fun accountIntent(
            context: Context,
            startWithAddress: Boolean,
            forResult: Boolean,
        ): Intent =
            getIntent(
                context,
                AccountFlowFragment::class,
                AccountFlowFragment.arguments(startWithAddress, forResult),
            ).setClass(context, AccountActivity::class.java)
    }
}
