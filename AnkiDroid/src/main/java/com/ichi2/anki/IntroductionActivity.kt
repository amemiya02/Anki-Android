// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Shridhar Goel <shridhar.goel@gmail.com>

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.commit
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.introduction.WelcomeFragment
import com.ichi2.themes.Themes

/** The first-run choices before opening the collection on Decks. */
class IntroductionActivity :
    AnkiActivity(),
    WelcomeFragment.Host {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        if (savedInstanceState == null) supportFragmentManager.commit { add(android.R.id.content, WelcomeFragment()) }
    }

    override fun onWelcomeCompleted(nextAction: String?) {
        if (isFinishing) return
        val deckPicker = DeckPicker.getIntent(this)
        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (nextAction != null) deckPicker.putExtra(nextAction, true)
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
