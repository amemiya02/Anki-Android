// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.ichi2.anki.common.android.R
import com.ichi2.compose.theme.toAnkiDroidDimensions
import com.ichi2.compose.theme.toFoundation
import com.ichi2.compose.theme.toMaterialTypography
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.google.android.material.R as MaterialR

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

    @Test
    fun `every type style in XML matches the design value`() {
        val mismatches =
            typeRoles().flatMap { role ->
                val name = role.getString("role")
                val style = styleNamed("TextAppearance_Foundation_" + name.replaceFirstChar { it.uppercase() })
                if (style == null) return@flatMap listOf("$name: no style")
                val size = role.getDouble("size").toFloat()
                val expected =
                    mapOf(
                        "size" to "${size}sp",
                        "weight" to role.getInt("weight").toString(),
                        "lineHeight" to "%.2fsp".format(size * role.getDouble("lineHeight")),
                        "materialLineHeight" to "%.2fsp".format(size * role.getDouble("lineHeight")),
                        "font" to "foundation_interface",
                        "figures" to if (role.optString("figures") == "tabular") "tnum" else "null",
                    )
                val actual =
                    mapOf(
                        "size" to
                            styleValue(style, android.R.attr.textSize) {
                                "${TypedValue.complexToFloat(data)}sp".takeIf {
                                    complexUnit ==
                                        TypedValue.COMPLEX_UNIT_SP
                                }
                            },
                        "weight" to styleValue(style, android.R.attr.textFontWeight) { data.toString() },
                        "lineHeight" to
                            styleValue(style, android.R.attr.lineHeight) {
                                "%.2fsp".format(TypedValue.complexToFloat(data)).takeIf {
                                    complexUnit ==
                                        TypedValue.COMPLEX_UNIT_SP
                                }
                            },
                        "materialLineHeight" to
                            styleValue(style, MaterialR.attr.lineHeight) {
                                "%.2fsp".format(TypedValue.complexToFloat(data)).takeIf {
                                    complexUnit ==
                                        TypedValue.COMPLEX_UNIT_SP
                                }
                            },
                        "font" to styleValue(style, android.R.attr.fontFamily) { appContext.resources.getResourceEntryName(resourceId) },
                        "figures" to (styleValue(style, android.R.attr.fontFeatureSettings) { string.toString() } ?: "null"),
                    )
                expected.keys.mapNotNull { key ->
                    if (expected[key] ==
                        actual[key]
                    ) {
                        null
                    } else {
                        "$name.$key: expected ${expected[key]}, was ${actual[key]}"
                    }
                }
            }
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun `Compose reads the same type scale`() {
        val type = appContext.toFoundation().type
        val styles =
            mapOf(
                "screenTitle" to type.screenTitle,
                "pageTitle" to type.pageTitle,
                "deckName" to type.deckName,
                "deckNameNested" to type.deckNameNested,
                "summary" to type.summary,
                "count" to type.count,
                "studyTitle" to type.studyTitle,
                "button" to type.button,
                "rowTitle" to type.rowTitle,
                "lead" to type.lead,
                "answer" to type.answer,
                "body" to type.body,
                "studyCount" to type.studyCount,
                "caption" to type.caption,
                "label" to type.label,
            )
        assertEquals(typeRoles().map { it.getString("role") }.sorted(), styles.keys.sorted(), "Compose type roles")
        val mismatches =
            typeRoles().flatMap { role ->
                val name = role.getString("role")
                val style = styles.getValue(name)
                val size = role.getDouble("size").toFloat()
                val expected =
                    listOf(
                        "${size}sp",
                        FontWeight(role.getInt("weight")).toString(),
                        "%.2fsp".format(size * role.getDouble("lineHeight")),
                        if (role.optString("figures") == "tabular") "tnum" else "null",
                    )
                val actual =
                    listOf(
                        "${style.fontSize.value}sp",
                        style.fontWeight.toString(),
                        "%.2fsp".format(style.lineHeight.value),
                        style.fontFeatureSettings.toString(),
                    )
                if (expected == actual) emptyList() else listOf("$name: expected $expected, was $actual")
            }
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun `no type style relies on italics, capitals or letter-spacing`() {
        val foundation = appContext.toFoundation()
        val compose = foundation.type.all + foundation.toMaterialTypography().all()
        for (style in compose) {
            assertEquals(FontStyle.Normal, style.fontStyle, "$style")
            assertEquals(0f, style.letterSpacing.value, "$style")
            assertTrue(style.fontFamily != null, "$style has no typeface")
        }
        for (name in materialTextAppearances) {
            for ((theme, styleRes) in themes) {
                val value = TypedValue()
                assertTrue(
                    ContextThemeWrapper(appContext, styleRes).theme.resolveAttribute(attrNamedMaterial(name), value, true),
                    "$theme.$name",
                )
                val style = value.resourceId
                val styleName = appContext.resources.getResourceEntryName(style)
                assertTrue(styleName.startsWith("TextAppearance.Foundation."), "$theme.$name is $styleName, not a Foundation style")
                assertEquals(
                    "false",
                    styleValue(style, android.R.attr.textAllCaps) { coerceToString().toString() } ?: "false",
                    "$theme.$name",
                )
                assertEquals(0f, styleValue(style, android.R.attr.letterSpacing) { float } ?: 0f, "$theme.$name")
                assertEquals(0, (styleValue(style, android.R.attr.textStyle) { data } ?: 0) and Typeface.ITALIC, "$theme.$name")
            }
        }
    }

    /** Android 7 loads fonts from resources through AndroidX, which can fail; the app must still start */
    @Test
    @Config(sdk = [24])
    fun `Compose reads the type scale on Android 7`() {
        val type = appContext.toFoundation().type
        assertTrue(type.all.all { it.fontFamily != null })
    }

    @Test
    fun `interface text falls back to the type scale's typeface in every theme`() {
        for ((theme, styleRes) in themes) {
            val value = TypedValue()
            ContextThemeWrapper(appContext, styleRes).theme.resolveAttribute(android.R.attr.fontFamily, value, true)
            assertEquals(R.font.foundation_interface, value.resourceId, theme)
        }
    }

    private fun typeRoles(): List<JSONObject> {
        val type = values.getJSONArray("type")
        return (0 until type.length()).map { type.getJSONObject(it) }
    }

    private fun styleNamed(name: String): Int? = runCatching { R.style::class.java.getField(name).getInt(null) }.getOrNull()

    private fun attrNamedMaterial(name: String): Int = MaterialR.attr::class.java.getField(name).getInt(null)

    /** The value of [attr] in [style], or null when the style doesn't set it */
    private fun <T> styleValue(
        style: Int,
        attr: Int,
        read: TypedValue.() -> T?,
    ): T? =
        appContext.obtainStyledAttributes(style, intArrayOf(attr)).use { array ->
            val value = TypedValue()
            if (!array.getValue(0, value)) null else value.read()
        }

    private fun Typography.all(): List<TextStyle> =
        listOf(
            displayLarge,
            displayMedium,
            displaySmall,
            headlineLarge,
            headlineMedium,
            headlineSmall,
            titleLarge,
            titleMedium,
            titleSmall,
            bodyLarge,
            bodyMedium,
            bodySmall,
            labelLarge,
            labelMedium,
            labelSmall,
        )

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

        /** Material 3's type slots, each of which a theme maps onto a Foundation style */
        val materialTextAppearances =
            listOf(
                "textAppearanceDisplayLarge",
                "textAppearanceDisplayMedium",
                "textAppearanceDisplaySmall",
                "textAppearanceHeadlineLarge",
                "textAppearanceHeadlineMedium",
                "textAppearanceHeadlineSmall",
                "textAppearanceTitleLarge",
                "textAppearanceTitleMedium",
                "textAppearanceTitleSmall",
                "textAppearanceBodyLarge",
                "textAppearanceBodyMedium",
                "textAppearanceBodySmall",
                "textAppearanceLabelLarge",
                "textAppearanceLabelMedium",
                "textAppearanceLabelSmall",
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
