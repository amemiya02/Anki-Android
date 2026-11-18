// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Divyansh Kushwaha <thedroiddiv@gmail.com>

package com.ichi2.anki.ui.windows.reviewer.whiteboard

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.skydoves.colorpickerview.AlphaTileView
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.databinding.ColorpickerviewDialogColorpickerBinding
import com.skydoves.colorpickerview.flag.FlagView
import com.ichi2.anki.common.utils.dp as nativeDp

/**
 * Shows a customizable color picker dialog with alpha and brightness controls.
 *
 * @receiver The Android context used to create the dialog.
 * @param initialColor The initial color to display in the picker as an `AARRGGBB` integer.
 * @param onColorSelected Callback invoked when the user confirms their color selection.
 *                        Receives the selected color as an `AARRGGBB` integer.
 */
fun Context.showColorPickerDialog(
    @ColorInt initialColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    val binding = ColorpickerviewDialogColorpickerBinding.inflate(LayoutInflater.from(this))
    val picker = binding.colorPickerView
    picker.attachAlphaSlider(binding.alphaSlideBar)
    picker.attachBrightnessSlider(binding.brightnessSlideBar)

    val bubbleFlag = BubbleFlag(this)
    picker.flagView = bubbleFlag
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        bubbleFlag.findViewById<FrameLayout>(R.id.flag_color_layout_wrapper).updatePadding(
            10.nativeDp.toPx(this),
            7.nativeDp.toPx(this),
            10.nativeDp.toPx(this),
        )
        bubbleFlag.alphaTileView.updateLayoutParams {
            width = 12.nativeDp.toPx(this@showColorPickerDialog)
            height = 12.nativeDp.toPx(this@showColorPickerDialog)
        }
    }
    // Supply the starting colour after the wheel and its brightness slider have been laid out.
    picker.post {
        picker.setInitialColor(initialColor)
        if (initialColor.isNearBlack()) {
            // Make the first hue pick visible. The colour remains the seed until the user edits it.
            picker.post { picker.brightnessSlider?.setSelectorByHalfSelectorPosition(1f) }
        }
    }

    picker.doOnAttach {
        val observer = picker.viewTreeObserver
        val listener = ViewTreeObserver.OnPreDrawListener {
            bubbleFlag.x = picker.selector.x + (picker.selector.width - bubbleFlag.width) / 2
            val y = picker.selector.y - bubbleFlag.height
            if (y > 0f) {
                bubbleFlag.y = y
                bubbleFlag.rotation = 0f
            } else {
                bubbleFlag.y = picker.selector.y + bubbleFlag.height
                bubbleFlag.rotation = 180f
            }
            true
        }
        observer.addOnPreDrawListener(listener)
        picker.doOnDetach {
            if (observer.isAlive) observer.removeOnPreDrawListener(listener)
        }
    }
    createPaperDialog(this) { dialog ->
        PaperDialog(
            title = getString(CommonString.choose_color),
            onDismissRequest = dialog::dismiss,
            scrollContent = false,
            actions =
                listOf(
                    DialogAction(getString(CommonString.dialog_cancel), dialog::dismiss),
                    DialogAction(getString(CommonString.dialog_ok), {
                        onColorSelected(picker.color)
                        dialog.dismiss()
                    }, DialogActionStyle.Filled),
                ),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                val diameter = minOf(maxWidth, 300.dp)
                val diameterPx = with(LocalDensity.current) { diameter.roundToPx() }
                AndroidView(
                    factory = {
                        picker.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, diameterPx)
                        binding.root
                    },
                    modifier = Modifier.width(diameter).heightIn(max = 420.dp),
                    update = {
                        if (picker.layoutParams.height != diameterPx) {
                            picker.updateLayoutParams { height = diameterPx }
                        }
                    },
                )
            }
        }
    }.show()
}

/**
 * Whether the color's brightness is low enough to read as black. The brightness slider stores
 * exactly this value, so a near-black color means the slider would be parked near zero.
 */
private fun Int.isNearBlack(): Boolean {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    return hsv[2] <= NEAR_BLACK_BRIGHTNESS
}

/** Brightness (HSV value) at or below which a color is treated as black. */
private const val NEAR_BLACK_BRIGHTNESS = 0.05f

/**
 * Custom flag view that displays the selected color in a bubble above the color picker selector.
 */
private class BubbleFlag(
    context: Context?,
) : FlagView(context, R.layout.view_colorpicker_flag_bubble) {
    val alphaTileView: AlphaTileView = findViewById(R.id.flag_color_layout)

    override fun onRefresh(colorEnvelope: ColorEnvelope) {
        alphaTileView.setPaintColor(colorEnvelope.color)
    }

    override fun onFlipped(isFlipped: Boolean?) {
    }
}
