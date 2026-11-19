// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A theme's `viewInflaterClass` names its class only as text, and AppCompat creates it by reflection.
 * R8 sees no use of the class and removes it from a release build unless `proguard-rules.pro` keeps
 * it. AppCompat then falls back to its own inflater, and inflated text loses its Foundation weight.
 */
class ViewInflaterIsKeptTest {
    @Test
    fun `every theme's view inflater is kept for release builds`() {
        val root = repositoryRoot()
        val inflaters = themeViewInflaters(root)
        assertTrue(inflaters.isNotEmpty(), "No theme sets viewInflaterClass: the search is looking in the wrong place")

        val keepRules =
            File(root, "AnkiDroid/proguard-rules.pro")
                .readLines()
                .map { it.trim() }
                .filter { it.startsWith("-keep class ") }
        val unkept = inflaters.filterNot { name -> keepRules.any { it == "-keep class $name" || it.startsWith("-keep class $name ") } }

        assertEquals(
            emptySet<String>(),
            unkept.toSet(),
            "AnkiDroid/proguard-rules.pro must keep each theme's view inflater, or release builds fall back to AppCompat's",
        )
    }

    /** The classes named by `<item name="viewInflaterClass">` in the modules' resources */
    private fun themeViewInflaters(root: File): Set<String> =
        modules
            .map { File(root, "$it/src/main/res") }
            .filter { it.isDirectory }
            .flatMap { res -> res.listFiles { dir -> dir.isDirectory && dir.name.startsWith("values") }.orEmpty().toList() }
            .flatMap { it.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .flatMap { file -> viewInflaterItem.findAll(file.readText()).map { it.groupValues[1] } }
            .toSet()

    private fun repositoryRoot(): File =
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .first { File(it, "AnkiDroid/proguard-rules.pro").isFile }

    private companion object {
        val modules = listOf("AnkiDroid", "common/android", "widgets", "anki-common", "note-editor", "api")
        val viewInflaterItem = Regex("""<item\s+name="viewInflaterClass"\s*>\s*([\w.$]+)\s*</item>""")
    }
}
