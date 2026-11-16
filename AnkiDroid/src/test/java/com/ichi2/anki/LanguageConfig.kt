// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider

/**
 * A language that screenshot tests can be run in.
 *
 * @param qualifier the Android resource qualifier, for example `zh-rCN`
 * @param option the value which selects this language with `-Planguage`, and the prefix of its
 *  screenshot filenames
 */
enum class LanguageConfig(
    val qualifier: String,
    val option: String,
    val isRightToLeft: Boolean = false,
) {
    ENGLISH("en-rUS", "en"),
    SIMPLIFIED_CHINESE("zh-rCN", "zh-cn"),
    JAPANESE("ja", "ja"),

    /** The right-to-left language */
    ARABIC("ar", "ar", isRightToLeft = true),
    ;

    /** Resource qualifiers which select this language and its layout direction */
    val qualifiers get() = if (isRightToLeft) "$qualifier-ldrtl" else "$qualifier-ldltr"
}

/**
 * Languages always covered by the annotated class, in addition to the `-Planguage` selection.
 *
 * ```kt
 * @IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
 * class TestClass : ScreenshotTest() {
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IncludeLanguages(
    vararg val value: LanguageConfig,
)

/** Provides the languages selected with `-Planguage` (default `en`) and [IncludeLanguages] */
class LanguageProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<LanguageConfig> {
        val requested = requestedOptions("screenshot.language", default = LanguageConfig.ENGLISH.option)
        if ("all" in requested) return LanguageConfig.entries
        val included =
            context
                ?.testClass()
                ?.getAnnotation(IncludeLanguages::class.java)
                ?.value
                .orEmpty()
        return LanguageConfig.entries.filter { it in included || it.option in requested }
    }
}

/** The comma-separated values of a `-P` screenshot option, passed to tests as a system property */
internal fun requestedOptions(
    property: String,
    default: String,
): List<String> = (System.getProperty(property) ?: default).split(",").map { it.trim().lowercase() }
