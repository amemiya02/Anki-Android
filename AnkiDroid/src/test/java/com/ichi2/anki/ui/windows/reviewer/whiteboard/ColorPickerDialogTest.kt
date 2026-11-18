// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.ui.windows.reviewer.whiteboard

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.themes.Themes
import com.skydoves.colorpickerview.ColorPickerView
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import java.time.Duration
import kotlin.test.assertNotNull

/** Tests for [showColorPickerDialog] */
@RunWith(AndroidJUnit4::class)
class ColorPickerDialogTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var context: Context
    private var pickedColor: Int? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Themes.setTheme(context)
    }

    @Test
    fun `picking a hue on a black-seeded picker keeps the hue visible`() {
        assertPickedHueIsVisible(seed = Color.BLACK)
    }

    @Test
    fun `picking a hue on a near-black-seeded picker keeps the hue visible`() {
        assertPickedHueIsVisible(seed = Color.rgb(3, 0, 0))
    }

    @Test
    fun `confirming without touching anything returns the initial color`() {
        showPicker(initialColor = Color.BLACK)

        confirm()

        assertThat(pickedColor, equalTo(Color.BLACK))
    }

    @Test
    fun `picking a hue on a bright seed keeps its brightness`() {
        val seed = Color.rgb(128, 0, 0)
        val dialog = showPicker(initialColor = seed)

        dialog.wheel().tapRightEdge()
        idleMainLooper()
        confirm()

        val color = assertNotNull(pickedColor, "a color should be returned on OK")
        val pickedBrightness = FloatArray(3).also { Color.colorToHSV(color, it) }[2]
        val seedBrightness = FloatArray(3).also { Color.colorToHSV(seed, it) }[2]
        assertThat(
            "brightness must be left untouched for a non-near-black seed",
            pickedBrightness.toDouble(),
            closeTo(seedBrightness.toDouble(), 0.02),
        )
    }

    /** Seeds the picker with [seed], taps a bright hue, and asserts the result is not swallowed to black. */
    private fun assertPickedHueIsVisible(seed: Int) {
        val dialog = showPicker(initialColor = seed)

        dialog.wheel().tapRightEdge() // pure red at full saturation
        idleMainLooper()
        confirm()

        val color = assertNotNull(pickedColor, "a color should be returned on OK")
        val hsv = FloatArray(3).also { Color.colorToHSV(color, it) }
        assertThat("picked color is not black", color, not(equalTo(Color.BLACK)))
        assertThat("picked hue keeps full brightness", hsv[2], equalTo(1f))
    }

    /** Shows the color picker dialog and waits for the picker to finish initializing. */
    private fun showPicker(initialColor: Int): Dialog {
        context.showColorPickerDialog(initialColor) { pickedColor = it }
        val dialog =
            assertNotNull(
                ShadowDialog.getLatestDialog(),
                "color picker dialog should be shown",
            )
        repeat(3) { idleMainLooper() }
        return dialog
    }

    private fun Dialog.wheel(): ColorPickerView = assertNotNull(findViewById(com.skydoves.colorpickerview.R.id.colorPickerView))

    private fun confirm() {
        composeRule.onNodeWithText(context.getString(CommonString.dialog_ok)).performClick()
        idleMainLooper()
    }

    private fun ColorPickerView.tapRightEdge() {
        assertThat("the wheel should be laid out", width, greaterThan(0))
        val x = width - 5f
        val y = height / 2f
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, x, y, 0)
        dispatchTouchEvent(down)
        dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    /** Let the wheel and its sliders finish layout and apply the seed before a touch. */
    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
}
