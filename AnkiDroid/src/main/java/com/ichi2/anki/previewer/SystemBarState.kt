// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsControllerCompat

/** Restores the host window's system bars after a card viewer changes them. */
@Suppress("DEPRECATION")
internal class SystemBarState(
    private val window: Window,
) {
    private val decor = window.decorView
    private val controller = WindowInsetsControllerCompat(window, decor)
    private val behavior = controller.systemBarsBehavior
    private val legacyUiFlags = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) decor.systemUiVisibility and LEGACY_UI_FLAGS else 0
    private val legacyWindowFlags = window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN
    private val visibleBars =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = ViewCompat.getRootWindowInsets(decor)
            (if (insets?.isVisible(statusBars()) != false) statusBars() else 0) or
                (if (insets?.isVisible(navigationBars()) != false) navigationBars() else 0)
        } else {
            0
        }

    fun restore() {
        controller.systemBarsBehavior = behavior
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            decor.systemUiVisibility = (decor.systemUiVisibility and LEGACY_UI_FLAGS.inv()) or legacyUiFlags
            window.setFlags(legacyWindowFlags, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            controller.show(visibleBars)
            controller.hide((statusBars() or navigationBars()) and visibleBars.inv())
        }
    }

    private companion object {
        const val LEGACY_UI_FLAGS =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
