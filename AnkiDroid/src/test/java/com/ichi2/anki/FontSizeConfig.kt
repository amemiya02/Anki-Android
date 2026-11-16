// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

/**
 * A system font size that screenshot tests can be run with.
 *
 * @param scale the system font scale: 1 is the default text size
 * @param option the value which selects this size with `-PfontSize`
 */
enum class FontSizeConfig(
    val scale: Float,
    val option: String,
) {
    NORMAL(1f, "normal"),

    /** The largest text size the Quality Bar requires a layout to stay usable at */
    PERCENT_200(2f, "200"),
    ;

    /** The component of the screenshot filename, for example `font200` */
    val fileNameComponent get() = "font$option"
}

/**
 * Font sizes always covered by the annotated class, in addition to the `-PfontSize` selection.
 *
 * ```kt
 * @IncludeFontSizes(PERCENT_200)
 * class TestClass : ScreenshotTest() {
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IncludeFontSizes(
    vararg val value: FontSizeConfig,
)

/** Provides the font sizes selected with `-PfontSize` (default `normal`) and [IncludeFontSizes] */
class FontSizeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<FontSizeConfig> {
        val requested = requestedOptions("screenshot.fontSize", default = FontSizeConfig.NORMAL.option)
        if ("all" in requested) return FontSizeConfig.entries
        val included =
            context
                ?.testClass()
                ?.getAnnotation(IncludeFontSizes::class.java)
                ?.value
                .orEmpty()
        return FontSizeConfig.entries.filter { it in included || it.option in requested }
    }
}
