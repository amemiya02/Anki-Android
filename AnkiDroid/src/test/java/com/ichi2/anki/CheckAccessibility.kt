// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult
import com.google.android.apps.common.testing.accessibility.framework.Parameters
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchy
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement

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
            checker = RoborazziATFAccessibilityChecker(checks = setOf(RenderedTextContrastCheck())),
            failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
        ),
        RoborazziATFAccessibilityCheckOptions(
            checker = RoborazziATFAccessibilityChecker(checks = setOf(TouchTargetSizeCheck())),
            failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
        ),
    )

/**
 * Robolectric can report just the rightmost pixel of a Compose label as its only character.
 * ATF normally samples the entire label when character locations are unavailable.
 */
private class RenderedTextContrastCheck : TextContrastCheck() {
    override fun runCheckOnHierarchy(
        hierarchy: AccessibilityHierarchy,
        fromRoot: ViewHierarchyElement?,
        parameters: Parameters?,
    ): List<AccessibilityHierarchyCheckResult> {
        val clippedLabels =
            hierarchy.allWindows.associate { window ->
                window.id to
                    window.allViews
                        .filter { it.hasClippedCharacterBounds() }
                        .map { it.id }
                        .toSet()
            }
        if (clippedLabels.values.all { it.isEmpty() }) return super.runCheckOnHierarchy(hierarchy, fromRoot, parameters)

        val locations = hierarchy.toProto().toBuilder()
        locations.windowsList.forEachIndexed { index, window ->
            val labels = clippedLabels.getValue(window.id)
            if (labels.isNotEmpty()) {
                val updated = window.toBuilder()
                window.viewsList.forEachIndexed { viewIndex, view ->
                    if (view.id in labels) updated.setViews(viewIndex, view.toBuilder().clearTextCharacterLocations())
                }
                locations.setWindows(index, updated)
            }
        }
        val correctedHierarchy = AccessibilityHierarchy.newBuilder(locations.build()).build()
        val root = fromRoot?.let { correctedHierarchy.getViewById(it.condensedUniqueId) }
        return super.runCheckOnHierarchy(correctedHierarchy, root, parameters)
    }

    private fun ViewHierarchyElement.hasClippedCharacterBounds(): Boolean {
        val glyph = textCharacterLocations.singleOrNull() ?: return false
        val bounds = boundsInScreen
        return (text?.length ?: 0) > 1 && glyph.width == 1 &&
            glyph.right == bounds.right && glyph.top == bounds.top && glyph.bottom == bounds.bottom
    }
}
