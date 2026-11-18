// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.compat.setTooltipTextCompat
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.ui.RtlCompliantActionProvider.Companion.unwrapContext
import com.ichi2.anki.common.android.R as CommonR

class SyncActionProvider(
    context: Context,
) : ActionProviderCompat(context) {
    val activity = unwrapContext(context)

    var isProgressShown by mutableStateOf(false)
    var icon: Drawable? by mutableStateOf(
        context.getDrawable(R.drawable.symbol_sync)?.mutate()?.apply {
            setTint(MaterialColors.getColor(context, CommonR.attr.foundationOnBackground, "Sync"))
        },
    )
    private var tooltip: CharSequence = context.getString(CommonString.button_sync)
    private var action: View? = null

    override fun onCreateActionView(forItem: MenuItem): View =
        object : FrameLayout(context) {
            private val target = resources.getDimensionPixelSize(CommonR.dimen.foundation_spacing_touch_target)

            override fun onMeasure(
                widthMeasureSpec: Int,
                heightMeasureSpec: Int,
            ) {
                // The toolbar measures candidate actions before attaching them to a window.
                if (isAttachedToWindow) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                } else {
                    setMeasuredDimension(resolveSize(target, widthMeasureSpec), resolveSize(target, heightMeasureSpec))
                }
            }
        }.apply {
            action = this
            setTooltipTextCompat(tooltip)
            addView(createContent(forItem))
        }

    private fun createContent(forItem: MenuItem): ComposeView =
        ComposeView(context).apply {
            setContent {
                AnkiDroidTheme {
                    val paper = MaterialTheme.foundation
                    Box(Modifier.size(paper.spacing.touchTarget), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { if (forItem.isEnabled) activity.onOptionsItemSelected(forItem) }) {
                            val drawable = icon
                            val bitmap = remember(drawable) { drawable?.toBitmap()?.asImageBitmap() }
                            if (bitmap != null) Image(bitmap, stringResource(CommonString.button_sync), Modifier.size(24.dp))
                        }
                        if (isProgressShown) {
                            LinearProgressIndicator(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).width(28.dp),
                                color = paper.colors.primary,
                                trackColor = paper.colors.outline,
                            )
                        }
                    }
                }
            }
        }

    fun setTooltipText(value: CharSequence) {
        tooltip = value
        action?.setTooltipTextCompat(value)
    }
}
