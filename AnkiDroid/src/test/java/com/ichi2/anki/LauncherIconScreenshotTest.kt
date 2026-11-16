// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Renders the launcher icon as launchers show it: its adaptive layers under a circle and a
 * rounded-square mask, and its themed (monochrome) form on a light and a dark wallpaper colour.
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light --tests "com.ichi2.anki.LauncherIconScreenshotTest"`
 */
class LauncherIconScreenshotTest : ComposeScreenshotTest() {
    private val icon: AdaptiveIconDrawable
        get() = ContextCompat.getDrawable(ApplicationProvider.getApplicationContext(), R.mipmap.ic_launcher) as AdaptiveIconDrawable

    @Test
    fun launcherIcon() {
        val icon = icon
        val monochrome = checkNotNull(icon.monochrome) { "no themed icon" }
        capture("launcher_icon") {
            Column(
                modifier = Modifier.background(Color(0xFF7A8A99)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AdaptiveIcon(CircleShape, listOf(icon.background, icon.foreground))
                    AdaptiveIcon(RoundedCornerShape(28), listOf(icon.background, icon.foreground))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // the colours a launcher picks from the wallpaper for themed icons, light and dark
                    AdaptiveIcon(CircleShape, listOf(monochrome), ground = Color(0xFFD3E3FD), tint = Color(0xFF041E49))
                    AdaptiveIcon(CircleShape, listOf(monochrome), ground = Color(0xFF0B305F), tint = Color(0xFFD3E3FD))
                }
            }
        }
    }

    /** Draws [layers] of a 108dp adaptive icon at 96dp, showing only the central 72dp as launchers do */
    @androidx.compose.runtime.Composable
    private fun AdaptiveIcon(
        mask: Shape,
        layers: List<Drawable>,
        ground: Color = Color.Transparent,
        tint: Color? = null,
    ) {
        Box(Modifier.size(96.dp).clip(mask).background(ground)) {
            Canvas(Modifier.size(96.dp)) {
                val visible = size.width
                val inset = (visible * 18 / 72).roundToInt()
                drawIntoCanvas { canvas ->
                    for (layer in layers) {
                        val drawable = layer.mutate()
                        drawable.setBounds(-inset, -inset, visible.roundToInt() + inset, visible.roundToInt() + inset)
                        tint?.let { drawable.colorFilter = PorterDuffColorFilter(it.toArgb(), PorterDuff.Mode.SRC_IN) }
                        drawable.draw(canvas.nativeCanvas)
                    }
                }
            }
        }
    }
}
