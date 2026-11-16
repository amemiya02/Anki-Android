// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck

/**
 * Every capture in the annotated class runs the accessibility check and fails the test on a
 * text-contrast (WCAG AA) or touch-target (48dp) problem.
 *
 * ```kt
 * @CheckAccessibility
 * class TestClass : ScreenshotTest() {
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckAccessibility

/** Whether the test class is held to [CheckAccessibility] */
internal val Any.checksAccessibility: Boolean
    get() = javaClass.isAnnotationPresent(CheckAccessibility::class.java)

/**
 * The accessibility checks of the Quality Bar; other checks are not run.
 *
 * ATF reports contrast measured from the rendered picture as a warning, so text contrast fails
 * on a warning. Touch-target warnings mean ATF could not measure a target, so only its errors fail.
 */
@OptIn(ExperimentalRoborazziApi::class)
internal fun qualityBarAccessibilityChecks() =
    listOf(
        RoborazziATFAccessibilityCheckOptions(
            checker = RoborazziATFAccessibilityChecker(checks = setOf(TextContrastCheck())),
            failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
        ),
        RoborazziATFAccessibilityCheckOptions(
            checker = RoborazziATFAccessibilityChecker(checks = setOf(TouchTargetSizeCheck())),
            failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
        ),
    )
