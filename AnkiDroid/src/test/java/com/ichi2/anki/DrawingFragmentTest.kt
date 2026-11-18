// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki

import android.graphics.Path
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.ui.windows.reviewer.whiteboard.WhiteboardFragment
import com.ichi2.testutils.dispatchInsets
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Tests for [DrawingFragment] */
@RunWith(AndroidJUnit4::class)
class DrawingFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `back press finishes the activity when the whiteboard is empty`() {
        val (activity, _) = launchDrawingActivity()

        activity.onBackPressedDispatcher.onBackPressed()
        advanceRobolectricLooper()

        assertNull(ShadowDialog.getLatestDialog(), "no discard dialog should be shown for an empty whiteboard")
        assertThat("activity finishes immediately", activity.isFinishing, equalTo(true))
    }

    @Test
    fun `back press shows the discard dialog when the whiteboard has content`() {
        val (activity, whiteboard) = launchDrawingActivity()
        whiteboard.whiteboardView.onNewPath
            ?.invoke(samplePath())
        advanceRobolectricLooper()

        activity.onBackPressedDispatcher.onBackPressed()
        advanceRobolectricLooper()

        assertNotNull(ShadowDialog.getLatestDialog(), "discard dialog should be shown")
        assertThat("activity is not finishing while dialog is open", activity.isFinishing, equalTo(false))

        compose.onNodeWithText(targetContext.getString(CommonString.discard)).performClick()
        advanceRobolectricLooper()
        assertThat("activity finishes after discard is confirmed", activity.isFinishing, equalTo(true))
    }

    @Test
    fun `the hosted drawing controls clear system bars and the display cutout`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            deckPicker.startActivity(DrawingFragment.getIntent(deckPicker))
            compose.waitUntil(10_000) { deckPicker.currentScreenFragment is DrawingFragment }
            val drawing = assertNotNull(deckPicker.currentScreenFragment).requireView()

            deckPicker.dispatchInsets(navBarBottom = 48.dp, cutoutLeft = 32.dp)

            assertThat(drawing.paddingTop, equalTo(24.dp.toPx(deckPicker)))
            assertThat(drawing.paddingBottom, equalTo(48.dp.toPx(deckPicker)))
            assertThat(drawing.paddingLeft, equalTo(32.dp.toPx(deckPicker)))
        }

    private fun launchDrawingActivity(): Pair<SingleFragmentActivity, WhiteboardFragment> {
        val activity = startRegularActivity<SingleFragmentActivity>(DrawingFragment.getIntent(targetContext))
        val drawingFragment =
            activity.supportFragmentManager.findFragmentByTag(SingleFragmentActivity.FRAGMENT_TAG) as DrawingFragment
        val whiteboardFragment =
            drawingFragment.childFragmentManager.findFragmentById(R.id.fragment_container) as WhiteboardFragment
        return activity to whiteboardFragment
    }

    private fun samplePath(): Path =
        Path().apply {
            moveTo(0f, 0f)
            lineTo(10f, 10f)
        }
}
