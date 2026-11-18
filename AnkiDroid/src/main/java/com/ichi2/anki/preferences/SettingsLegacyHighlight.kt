// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.view.doOnPreDraw
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.common.android.R as CommonR

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
