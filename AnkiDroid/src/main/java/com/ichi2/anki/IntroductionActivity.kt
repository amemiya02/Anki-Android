// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Shridhar Goel <shridhar.goel@gmail.com>

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.introduction.CollectionPermissionScreenLauncher
import com.ichi2.anki.introduction.Welcome
import com.ichi2.anki.introduction.hasCollectionStoragePermissions
import com.ichi2.anki.utils.bottomCornerClearance
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.themes.Themes

/** The first-run choices before opening the collection on Decks. */
class IntroductionActivity :
    AnkiActivity(),
    CollectionPermissionScreenLauncher {
    private var bottomClearance by mutableIntStateOf(0)

    override val permissionScreenLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (hasCollectionStoragePermissions()) openImport()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            bottomClearance = insets.bottomCornerClearance(view)
            insets
        }
        setContent {
            AnkiDroidTheme {
                Welcome(
                    bottomClearance = bottomClearance,
                    onStartEmpty = { startDeckPicker() },
                    onImport = ::openImport,
                    onSetupSync = { startDeckPicker(DeckPicker.EXTRA_SYNC_SETUP) },
                )
            }
        }
    }

    private fun openImport() {
        if (!collectionPermissionScreenWasOpened()) startDeckPicker(DeckPicker.EXTRA_IMPORT)
    }

    private fun startDeckPicker(extra: String? = null) {
        if (isFinishing) return
        sharedPrefs().edit { putBoolean(PREF_INTRODUCTION_SLIDES_SHOWN, true) }
        val deckPicker = DeckPicker.getIntent(this)
        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (extra != null) deckPicker.putExtra(extra, true)
        startActivity(deckPicker)
        finish()
    }

    companion object {
        /** Retain the original preference so installed users keep going straight to Decks. */
        const val PREF_INTRODUCTION_SLIDES_SHOWN = "IntroductionSlidesShown"
    }
}

internal fun Context.hasShownAppIntro(): Boolean {
    val prefs = sharedPrefs()
    return prefs.getBoolean(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN, false) || !InitialActivity.wasFreshInstall(prefs)
}
