// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.preferences.CustomSyncServerSettingsFragment
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.syncServerAddress

/**
 * Hosts the sync account screens: [LoginFragment] or [LoggedInFragment].
 *
 * Its own activity so edge-to-edge applies to these screens alone. Each fragment insets its own root.
 */
class AccountActivity : SingleFragmentActivity() {
    companion object {
        /** Sees if we want to go back to the DeckPicker after login*/
        const val ARG_START_FOR_RESULT = "START_FOR_RESULT"

        /**
         * Returns an [Intent] to launch either [LoggedInFragment] or [LoginFragment]
         * based on the current login state.
         *
         * While no sync server is set up, it opens sync setup instead: there is nothing to sign in to,
         * as the Fork never signs in to AnkiWeb (ADR-0001).
         *
         * @param context The context used to create the intent.
         * @param forResult Indicates whether the calling component expects a result.
         * This is used to distinguish if the screen was launched from DeckPicker
         * or any other screen that needs a result back.
         *
         * @return An [Intent] to start the appropriate fragment.
         */
        fun getIntent(
            context: Context,
            forResult: Boolean = false,
        ): Intent {
            if (syncServerAddress() == null) {
                return PreferencesActivity.getIntent(context, CustomSyncServerSettingsFragment::class)
            }
            return getIntent(
                context = context,
                fragmentClass = if (isLoggedIn()) LoggedInFragment::class else LoginFragment::class,
                arguments =
                    Bundle().apply {
                        putBoolean(ARG_START_FOR_RESULT, forResult)
                    },
            ).setClass(context, AccountActivity::class.java)
        }
    }
}
