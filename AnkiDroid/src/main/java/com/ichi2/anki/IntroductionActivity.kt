// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Shridhar Goel <shridhar.goel@gmail.com>

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.os.BundleCompat
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.introduction.CollectionPermissionScreenLauncher
import com.ichi2.anki.introduction.SetupCollectionFragment
import com.ichi2.anki.introduction.SetupCollectionFragment.CollectionSetupOption
import com.ichi2.anki.introduction.SetupCollectionFragment.Companion.REQUEST_COLLECTION_SETUP
import com.ichi2.anki.introduction.SetupCollectionFragment.Companion.RESULT_KEY
import com.ichi2.anki.introduction.hasCollectionStoragePermissions
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.themes.Themes
import timber.log.Timber

/**
 * App introduction for new users.
 *
 * Links to [DeckPicker], either to import a collection or deck package ("Import") or to start
 * with a new collection ("Get Started"). Upstream's "Sync from AnkiWeb" is not offered (ADR-0001).
 *
 * @see SetupCollectionFragment
 */
// TODO: Background of introduction_layout does not display on API 25 emulator: https://github.com/ankidroid/Anki-Android/pull/12033#issuecomment-1228429130
@NeedsTest("Ensure that we can get here on first run without an exception dialog shown")
class IntroductionActivity :
    AnkiActivity(R.layout.activity_introduction),
    CollectionPermissionScreenLauncher {
    override val permissionScreenLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (hasCollectionStoragePermissions()) {
                openImport()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme })

        setFragmentResultListener(REQUEST_COLLECTION_SETUP) { _, bundle ->
            val option =
                BundleCompat.getParcelable(bundle, RESULT_KEY, CollectionSetupOption::class.java) ?: error("Missing introduction option!")
            when (option) {
                CollectionSetupOption.DeckPickerWithNewCollection -> startDeckPicker()
                CollectionSetupOption.ImportPackage -> openImport()
            }
        }
    }

    private fun openImport() {
        if (collectionPermissionScreenWasOpened()) return
        Timber.i("Opening deck picker to import")
        startDeckPicker(RESULT_IMPORT)
    }

    private fun startDeckPicker(result: Int = RESULT_START_NEW) {
        Timber.i("Opening deck picker, import: %b", result == RESULT_IMPORT)
        this.sharedPrefs().edit { putBoolean(PREF_INTRODUCTION_SLIDES_SHOWN, true) }
        val deckPicker = Intent(this, DeckPicker::class.java)
        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (result == RESULT_IMPORT) {
            deckPicker.putExtra(DeckPicker.EXTRA_IMPORT, true)
        }

        startActivity(deckPicker)
        finish()
    }

    companion object {
        const val RESULT_START_NEW = 1
        const val RESULT_IMPORT = 2

        /**
         * Key for the preference recording that the slide "Study less/ Remember more" offering to
         * get started or import, was displayed. If so don't display it again.
         */
        const val PREF_INTRODUCTION_SLIDES_SHOWN = "IntroductionSlidesShown"
    }
}

internal fun Context.hasShownAppIntro(): Boolean = sharedPrefs().getBoolean(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN, false)
