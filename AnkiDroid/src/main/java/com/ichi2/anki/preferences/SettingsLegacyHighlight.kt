// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.common.android.R as CommonR

/** Highlights a row in a settings page that still uses Views. */
internal fun SettingsFragment.highlightSearchPreference(key: String) {
    if (view == null) return
    val list = listView
    val adapter = list.adapter as? PreferenceGroup.PreferencePositionCallback ?: return
    val position = adapter.getPreferenceAdapterPosition(key)
    if (position == RecyclerView.NO_POSITION) return
    list.scrollToPosition(position)
    list.doOnPreDraw { list.findViewHolderForAdapterPosition(position)?.itemView?.markSettingsSearchTarget() }
}

internal fun View.markSettingsSearchTarget() {
    val mark =
        GradientDrawable().apply {
            cornerRadius = resources.getDimension(CommonR.dimen.foundation_shape_button)
            setStroke(
                resources.getDimensionPixelSize(CommonR.dimen.foundation_shape_hairline),
                MaterialColors.getColor(this@markSettingsSearchTarget, CommonR.attr.foundationEmphasis),
            )
            setBounds(0, 0, width, height)
        }
    overlay.add(mark)
    postDelayed({ overlay.remove(mark) }, 2_000)
}

internal fun View.revealSettingsSearchTarget() {
    doOnPreDraw {
        requestRectangleOnScreen(Rect(0, 0, width, height), true)
        markSettingsSearchTarget()
    }
}
