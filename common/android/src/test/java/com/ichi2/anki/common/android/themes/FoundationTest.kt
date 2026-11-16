// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.ichi2.anki.common.android.R
import com.ichi2.compose.theme.toAnkiDroidDimensions
import com.ichi2.compose.theme.toFoundation
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Foundation must not drift from the chosen Direction's design values.
 *
 * `docs/redesign/quiet-paper.values.json` is the single source: this test reads it and compares
 * every colour role, corner shape and spacing value with what the app's themes and resources hold.
 */
@RunWith(RobolectricTestRunner::class)
class FoundationTest {
    private val appContext: Context get() = ApplicationProvider.getApplicationContext()

    private val values: JSONObject by lazy { JSONObject(designValuesFile().readText()) }

    @Test
    fun `every colour role resolves to the design value in each theme`() {
        val mismatches =
            themes.flatMap { (name, style) ->
                val expected = values.getJSONObject("colour").getJSONObject(name)
                val theme = ContextThemeWrapper(appContext, style).theme
                expected.keys().asSequence().filter { it !in cardSheetRoles }.mapNotNull { role ->
                    val attribute = attrNamed("foundation" + role.replaceFirstChar { it.uppercase() })
                    val value = TypedValue()
                    val actual =
                        when {
                            attribute == null -> "no attribute"
                            !theme.resolveAttribute(attribute, value, true) -> "unset"
                            else -> "#%06X".format(value.data and 0xFFFFFF)
                        }
                    val wanted = expected.getString(role).uppercase()
                    if (actual == wanted) null else "$name.$role: expected $wanted, was $actual"
                }
            }
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun `Compose reads the same colour roles in each theme`() {
        val mismatches =
            themes.flatMap { (name, style) ->
                val expected = values.getJSONObject("colour").getJSONObject(name)
                val colors = ContextThemeWrapper(appContext, style).toFoundation().colors
                val actual =
                    mapOf(
                        "background" to colors.background,
                        "onBackground" to colors.onBackground,
                        "onBackgroundMuted" to colors.onBackgroundMuted,
                        "outline" to colors.outline,
                        "primary" to colors.primary,
                        "onPrimary" to colors.onPrimary,
                        "emphasis" to colors.emphasis,
                        "countNew" to colors.countNew,
                        "countLearning" to colors.countLearning,
                        "countDue" to colors.countDue,
                        "answerAgain" to colors.answerAgain,
                        "answerHard" to colors.answerHard,
                        "answerGood" to colors.answerGood,
                        "answerEasy" to colors.answerEasy,
                        "progressTrack" to colors.progressTrack,
                        "progressFill" to colors.progressFill,
                    ).mapValues { (_, color) -> "#%06X".format(color.toArgb() and 0xFFFFFF) }
                val roles =
                    expected
                        .keys()
                        .asSequence()
                        .filter { it !in cardSheetRoles }
                        .toList()
                assertEquals(roles.sorted(), actual.keys.sorted(), "$name: Compose colour roles")
                roles.mapNotNull { role ->
                    val wanted = expected.getString(role).uppercase()
                    if (actual[role] == wanted) null else "$name.$role: expected $wanted, was ${actual[role]}"
                }
            }
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun `Compose reads the same shapes and spacing`() {
        val foundation = appContext.toFoundation()
        val shape = values.getJSONObject("shape")
        assertEquals(shape.getDouble("hairline").toFloat(), foundation.shapes.hairline.value)
        assertEquals(shape.getDouble("currentTabRule").toFloat(), foundation.shapes.currentTabRule.value)
        assertEquals(shape.getDouble("answerRule").toFloat(), foundation.shapes.answerRule.value)
        assertEquals("full", shape.getString("iconButton"))
        assertEquals(CircleShape, foundation.shapes.iconButton)
        val size = Size(100f, 100f)
        val density = Density(1f)
        for ((key, actual) in listOf(
            "cardSheet" to foundation.shapes.cardSheet,
            "button" to foundation.shapes.button,
            "addButton" to foundation.shapes.addButton,
        )) {
            assertEquals(shape.getDouble(key).toFloat(), actual.topStart.toPx(size, density), key)
        }
        val spacing = values.getJSONObject("spacing")
        val scale = spacing.getJSONArray("scale")
        assertEquals((0 until scale.length()).map { scale.getInt(it).toFloat() }, foundation.spacing.scale.map { it.value })
        assertEquals(spacing.getDouble("screenMargin").toFloat(), foundation.spacing.screenMargin.value)
        assertEquals(spacing.getDouble("touchTarget").toFloat(), foundation.spacing.touchTarget.value)
        assertEquals(spacing.getDouble("answerButton").toFloat(), foundation.spacing.answerButton.value)
    }

    @Test
    fun `nothing casts a shadow except the add button`() {
        for ((name, style) in themes) {
            val context = ContextThemeWrapper(appContext, style)
            val foundation = context.toFoundation()
            assertEquals(0f, foundation.elevation.flat.value, name)
            assertEquals(0f, context.toAnkiDroidDimensions().studyScreenElevation.value, name)
            assertEquals(foundation.colors.primary.copy(alpha = 0.55f), foundation.elevation.addButtonShadow, name)
            assertTrue(foundation.elevation.addButton.value > 0f, name)
        }
    }

    @Test
    fun `every corner shape and rule width matches the design value`() {
        val shape = values.getJSONObject("shape")
        val mismatches =
            shape.keys().asSequence().filter { shape.get(it) is Number }.mapNotNull { key ->
                val dimen = "foundation_shape_" + key.toSnakeCase()
                val actual = dimenDp(dimen)
                val wanted = shape.getDouble(key).toFloat()
                if (actual == wanted) null else "$dimen: expected $wanted, was $actual"
            }
        assertEquals(emptyList(), mismatches.toList())
    }

    @Test
    fun `every spacing value matches the design value`() {
        val spacing = values.getJSONObject("spacing")
        val scale = spacing.getJSONArray("scale")
        val named =
            spacing.keys().asSequence().filter { it != "scale" }.map { key ->
                "foundation_spacing_" + key.toSnakeCase() to spacing.getDouble(key).toFloat()
            }
        val stepped = (0 until scale.length()).map { i -> "foundation_space_${scale.getInt(i)}" to scale.getInt(i).toFloat() }
        val mismatches =
            (named + stepped).mapNotNull { (dimen, wanted) ->
                val actual = dimenDp(dimen)
                if (actual == wanted) null else "$dimen: expected $wanted, was $actual"
            }
        assertEquals(emptyList(), mismatches.toList())
    }

    private fun attrNamed(name: String): Int? = runCatching { R.attr::class.java.getField(name).getInt(null) }.getOrNull()

    private fun dimenDp(name: String): Float? {
        val id = runCatching { R.dimen::class.java.getField(name).getInt(null) }.getOrNull() ?: return null
        return appContext.resources.getDimension(id) / appContext.resources.displayMetrics.density
    }

    private fun String.toSnakeCase() = replace(Regex("([A-Z])")) { "_" + it.value.lowercase() }

    companion object {
        /** The three themes, by the names the design values use */
        val themes =
            listOf(
                "light" to R.style.Theme_Light,
                "dark" to R.style.Theme_Dark,
                "black" to R.style.Theme_Dark_Black,
            )

        /** Stand-ins for how Card Content is drawn today; the Redesign does not restyle Card Content */
        val cardSheetRoles = setOf("cardSheet", "onCardSheet", "cardSheetRule")

        fun designValuesFile(): File {
            var dir: File? = File(System.getProperty("user.dir")!!).absoluteFile
            while (dir != null) {
                val candidate = File(dir, "docs/redesign/quiet-paper.values.json")
                if (candidate.exists()) return candidate
                dir = dir.parentFile
            }
            error("docs/redesign/quiet-paper.values.json not found above ${System.getProperty("user.dir")}")
        }
    }
}
