// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import org.json.JSONObject
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

/**
 * Illustrations, empty states, backgrounds and other graphics follow the Foundation in Light, Dark
 * and Black: they take their colours from the theme, so none keeps the old look's blue.
 *
 * - A drawable (other than an interface icon, which `InterfaceIconsAreMaterialSymbolsTest` checks)
 *   colours itself with theme attributes, as layouts do. Transparent is no colour. A hex value or a
 *   fixed `@color/` is allowed only where the graphic is drawn without the app's theme (the launcher
 *   and its shortcuts), and then only a Foundation colour or one of the launcher icon's own colours
 *   (`docs/redesign/quiet-paper/launcher-icon.svg`).
 * - A white or black that only says where to paint is a mask, not a colour: a ripple's mask, a
 *   vector tinted by a theme attribute, or a graphic the system or its user tints ([MASKS]).
 * - No graphic is a bitmap: its colours could not follow the theme.
 * - Kotlin draws with theme attributes too. A colour that is content (whiteboard ink the person
 *   picks) is listed in [CONTENT_COLOURS].
 */
class GraphicsUseFoundationTest {
    @Test
    fun `no graphic fixes a colour outside the Foundation`() {
        val offending = colourProblems()
        assertEquals(emptyList(), offending, "${offending.size} hard-coded colours in graphics")
    }

    @Test
    fun `no graphic is a bitmap`() {
        val offending = bitmapProblems()
        assertEquals(emptyList(), offending, "${offending.size} bitmaps")
    }

    @Test
    fun `no code draws with a colour outside the Foundation`() {
        val offending = codeProblems()
        assertEquals(emptyList(), offending, "${offending.size} hard-coded colours in code")
    }

    @Test
    fun `the exemptions are still needed`() {
        val unusedMasks = MASKS.keys.filter { name -> drawables().none { it.nameWithoutExtension == name } }
        val unusedOutside = DRAWN_WITHOUT_THE_APP_THEME.keys.filter { name -> drawables().none { it.nameWithoutExtension == name } }
        assertEquals(emptyList(), unusedMasks + unusedOutside, "exemptions no longer needed")
    }

    private fun colourProblems(): List<String> = drawables().flatMap { drawableProblems(it) }

    private fun bitmapProblems(): List<String> =
        drawableDirs()
            .flatMap { dir -> dir.listFiles().orEmpty().toList() }
            .filter { it.extension in setOf("png", "jpg", "jpeg", "webp", "gif") }
            .map { "${where(it)}: a bitmap cannot follow the theme" }
            .sorted()

    private fun codeProblems(): List<String> =
        sourceDirs
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.toList() }
            .sortedBy { it.path }
            .flatMap { file ->
                val path = file.relativeTo(ROOT).path
                file.readLines().flatMapIndexed { index, line ->
                    KOTLIN_COLOUR
                        .findAll(line)
                        .map { it.value }
                        .filter { !followsTheFoundation(it) }
                        .filter { token -> CONTENT_COLOURS.keys.none { it.covers(path, token) } }
                        .map { "$path:${index + 1}: $it" }
                        .toList()
                }
            }

    private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    private val foundationValues: Set<String> by lazy {
        val colour = JSONObject(File(ROOT, "docs/redesign/material-3.values.json").readText()).getJSONObject("colour")
        colour
            .keys()
            .asSequence()
            .flatMap { theme ->
                val roles = colour.getJSONObject(theme)
                // the card sheet roles stand for Card Content, which the app does not draw
                roles
                    .keys()
                    .asSequence()
                    .filter { !it.contains("ardSheet") }
                    .map { roles.getString(it).uppercase() }
            }.toSet()
    }

    /**
     * The launcher icon's own colours. Its paper and ink were the Light theme's until the Light
     * palette changed to Plain paper (2026-11-18); the icon keeps them.
     */
    private val launcherIconColours: Set<String> by lazy {
        HEX_COLOUR
            .findAll(File(ROOT, "docs/redesign/quiet-paper/launcher-icon.svg").readText())
            .map { it.value.opaqueHex() }
            .toSet()
    }

    /** Colour state lists (`res/color/`) by name */
    private val colourStateLists: Map<String, File> by lazy {
        resourceDirs
            .flatMap { res -> res.listFiles { dir -> dir.name.startsWith("color") }.orEmpty().toList() }
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .associateBy { it.nameWithoutExtension }
    }

    private fun drawableDirs(): List<File> =
        resourceDirs.flatMap { res -> res.listFiles { dir -> dir.name.startsWith("drawable") }.orEmpty().toList() }

    /** Every XML drawable that is not an interface icon */
    private fun drawables(): List<File> =
        drawableDirs()
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .filter { !it.name.startsWith("symbol_") }
            .sortedBy { it.path }
            .also { check(it.size > 30) { "only ${it.size} drawables found: is the working directory the module?" } }

    private fun where(file: File) =
        "${file.path.substringBefore("/src/main/res").removePrefix(ROOT.path + "/")}:${file.parentFile?.name}/${file.name}"

    private fun drawableProblems(file: File): List<String> {
        val name = file.nameWithoutExtension
        val root = builder.parse(file).documentElement
        val tintedByTheme = THEME_ATTRIBUTE.matches(root.getAttribute("android:tint"))
        return root
            .colourAttributes()
            .mapNotNull { (attribute, value) ->
                val problem = colourProblem(value) ?: return@mapNotNull null
                when {
                    name in MASKS -> null
                    tintedByTheme && value.isWhite() -> null
                    problem == FIXED_FOUNDATION_COLOUR && name in DRAWN_WITHOUT_THE_APP_THEME -> null
                    name in DRAWN_WITHOUT_THE_APP_THEME && value.opaqueHex() in launcherIconColours -> null
                    else -> "${where(file)}: $attribute=\"$value\"$problem"
                }
            }.distinct()
            .toList()
    }

    /** The colour attributes of an element and its descendants, skipping a ripple's mask */
    private fun Element.colourAttributes(): Sequence<Pair<String, String>> {
        if (getAttribute("android:id") == "@android:id/mask") return emptySequence()
        val own =
            (0 until attributes.length)
                .asSequence()
                .map { attributes.item(it) }
                .map { it.nodeName to it.nodeValue }
                .filter { (name, _) -> !name.startsWith("tools:") }
        return own + childElements().flatMap { it.colourAttributes() }
    }

    /**
     * Why [value] is a colour fixed in place: [FIXED_FOUNDATION_COLOUR] for a Foundation colour that
     * does not follow the theme, another explanation for any other colour; null if it follows the
     * theme or is no colour at all.
     */
    private fun colourProblem(
        value: String,
        seen: Set<String> = emptySet(),
    ): String? {
        if (HEX_COLOUR.matches(value)) {
            if (value.length == 9 && value.startsWith("#00")) return null // fully transparent
            return if (value.opaqueHex() in foundationValues) FIXED_FOUNDATION_COLOUR else ""
        }
        val match = COLOUR_RESOURCE.matchEntire(value) ?: return null
        val (namespace, colour) = match.destructured
        if (colour == "transparent") return null
        if (namespace.isEmpty() && colour.startsWith("foundation_")) return FIXED_FOUNDATION_COLOUR
        if (namespace.isNotEmpty()) return ""
        val stateList = colourStateLists[colour] ?: return ""
        if (colour in seen) return null
        val inner =
            builder
                .parse(stateList)
                .documentElement
                .colourAttributes()
                .filter { (attr, _) -> attr == "android:color" }
                .mapNotNull { (_, itemValue) -> colourProblem(itemValue, seen + colour)?.let { itemValue } }
                .toList()
        return if (inner.isEmpty()) null else " (its colour state list holds ${inner.joinToString()})"
    }

    /** An uppercase hex colour without a fully opaque alpha */
    private fun String.opaqueHex() = uppercase().let { if (it.length == 9 && it.startsWith("#FF")) "#" + it.substring(3) else it }

    private fun String.isWhite() = uppercase() in setOf("#FFF", "#FFFFFF", "#FFFFFFFF", "@ANDROID:COLOR/WHITE", "@COLOR/WHITE")

    /**
     * Whether a colour named in code follows the Foundation: a Foundation colour or a Flag's colour
     * (code also draws outside the app's theme, as notifications are), or one of the app's colour
     * state lists built from theme attributes.
     */
    private fun followsTheFoundation(token: String): Boolean {
        if (FOUNDATION_RESOURCE.matches(token)) return true
        val app = APP_COLOUR_RESOURCE.matchEntire(token) ?: return false
        return colourProblem("@color/${app.groupValues[1]}") == null
    }

    /** Whether a [CONTENT_COLOURS] key (`<path part>` or `<path part>#<token>`) covers [token] in the file at [path] */
    private fun String.covers(
        path: String,
        token: String,
    ): Boolean = path.contains(substringBefore('#')) && (!contains('#') || substringAfter('#') == token)

    private fun Element.childElements(): Sequence<Element> =
        (0 until childNodes.length).asSequence().map { childNodes.item(it) }.filterIsInstance<Element>()

    companion object {
        private const val FIXED_FOUNDATION_COLOUR = " (a Foundation colour, but fixed: use its theme attribute so it follows the theme)"

        private val ROOT = File("..").canonicalFile

        private val modules = listOf("AnkiDroid", "common/android", "widgets", "anki-common", "note-editor", "api")

        // test working directory is the module directory: `AnkiDroid/`
        private val resourceDirs = modules.map { File(ROOT, "$it/src/main/res") }

        private val sourceDirs = modules.map { File(ROOT, "$it/src/main") }

        private val HEX_COLOUR = Regex("#[0-9a-fA-F]{3,8}")
        private val COLOUR_RESOURCE = Regex("@(android:)?color/([A-Za-z0-9_.]+)")
        private val THEME_ATTRIBUTE = Regex("\\?(android:)?(attr/)?[A-Za-z0-9_]+")

        /** A colour fixed in code: a named platform or Compose colour, a colour built from numbers, or a colour resource */
        private val KOTLIN_COLOUR =
            Regex(
                "\\bColor\\.(BLACK|WHITE|RED|GREEN|BLUE|GRAY|GREY|LTGRAY|DKGRAY|YELLOW|CYAN|MAGENTA|" +
                    "Black|White|Red|Green|Blue|Gray|LightGray|DarkGray|Yellow|Cyan|Magenta)\\b|" +
                    "\\bColor\\.(rgb|argb|parseColor)\\(|\\bColor\\(0x[0-9a-fA-F]+|" +
                    "\\b(CommonR|MaterialR|android\\.R|R)\\.color\\.\\w+",
            )

        private val FOUNDATION_RESOURCE = Regex(".*\\.color\\.(foundation_\\w+|flag_\\w+|transparent)")

        /** A colour resource of the app's own modules (not the platform's or a library's) */
        private val APP_COLOUR_RESOURCE = Regex("(?:CommonR|R)\\.color\\.(\\w+)")

        /**
         * Graphics drawn by the launcher, not inside the app, so a theme attribute cannot reach them.
         * They use Foundation colours fixed in place, or the launcher icon's own colours.
         */
        private val DRAWN_WITHOUT_THE_APP_THEME =
            mapOf(
                "add_shortcut" to "a launcher shortcut, drawn with the launcher icon",
                "browse_shortcut" to "a launcher shortcut, drawn with the launcher icon",
                "review_shortcut" to "a launcher shortcut, drawn with the launcher icon",
            )

        /** Graphics whose colour only says where to paint: the system or the view that shows them tints them */
        private val MASKS =
            mapOf(
                "widget_action" to "a home-screen widget action mask: Glance tints it with the app's primary colour",
                "widget_sheet" to "a home-screen widget background mask: Glance tints it with the app's theme colours",
                "widget_sheet_compact" to "a compact home-screen widget background mask: Glance tints it with the app's theme colours",
                "ic_hayaki_notification" to "the notification's small icon: the system draws only its shape (ticket 47)",
            )

        /**
         * Colours in code that are content, not interface: `<path suffix>#<token>`, or a whole file
         * when no token is given.
         */
        private val CONTENT_COLOURS =
            mapOf(
                "com/ichi2/anki/ui/windows/reviewer/whiteboard/" to "the whiteboard: brush and ink colours the person picks",
                "com/ichi2/anki/DrawingFragment.kt" to "a drawing saved into a Note: its paper is Card Content",
            )
    }
}
