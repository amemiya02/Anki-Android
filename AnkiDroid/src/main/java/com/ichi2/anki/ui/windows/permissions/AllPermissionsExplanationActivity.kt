// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.ui.windows.permissions

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.R

/**
 * When the user opens the Android settings app and navigates to AnkiDroid's permissions,
 * there will be a "more info" button which will launch this activity. See
 * [the docs](https://developer.android.com/training/permissions/explaining-access#privacy-dashboard).
 * This button in the Android settings app is only visible at or above API 31.
 *
 * This activity is used to host the [AllPermissionsExplanationFragment] fragment.
 */
@RequiresApi(Build.VERSION_CODES.S)
class AllPermissionsExplanationActivity : AnkiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(FragmentContainerView(this).apply { id = R.id.fragment_container })
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, AllPermissionsExplanationFragment())
            }
        }
    }
}
