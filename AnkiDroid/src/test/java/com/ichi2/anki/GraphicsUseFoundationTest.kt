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
 *   and its shortcuts), and then only a Foundation colour.
 * - A white or black that only says where to paint is a mask, not a colour: a ripple's mask, a
 *   vector tinted by a theme attribute, or a graphic the system or its user tints ([MASKS]).
 * - No graphic is a bitmap: its colours could not follow the theme.
 * - Kotlin draws with theme attributes too. A colour that is content (whiteboard ink the person
 *   picks) is listed in [CONTENT_COLOURS].
 */
class GraphicsUseFoundationTest {
    @Test
    fun `no graphic fixes a colour outside the Foundation`() {
        val offending = colourProblems().filter { it.isSwept() }
        assertEquals(emptyList(), offending, "${offending.size} hard-coded colours in graphics")
    }

    @Test
    fun `no graphic is a bitmap`() {
        val offending = bitmapProblems().filter { it.isSwept() }
        assertEquals(emptyList(), offending, "${offending.size} bitmaps")
    }

    @Test
    fun `no code draws with a colour outside the Foundation`() {
        val offending = codeProblems().filter { it.isSwept() }
        assertEquals(emptyList(), offending, "${offending.size} hard-coded colours in code")
    }

    @Test
    fun `the exemptions are still needed`() {
        val unusedMasks = MASKS.keys.filter { name -> drawables().none { it.nameWithoutExtension == name } }
        val unusedOutside = DRAWN_WITHOUT_THE_APP_THEME.keys.filter { name -> drawables().none { it.nameWithoutExtension == name } }
        val all = colourProblems() + bitmapProblems() + codeProblems()
        val swept = NOT_YET_SWEPT.filter { part -> all.none { part in it } }.map { "$it is swept: remove it from NOT_YET_SWEPT" }
        assertEquals(emptyList(), unusedMasks + unusedOutside + swept, "exemptions no longer needed")
    }

    private fun String.isSwept() = NOT_YET_SWEPT.none { it in this }

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
                        .filter { !isFoundationResource(it) }
                        .filter { token -> CONTENT_COLOURS.keys.none { it.covers(path, token) } }
                        .map { "$path:${index + 1}: $it" }
                        .toList()
                }
            }

    private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    private val foundationValues: Set<String> by lazy {
        val colour = JSONObject(File(ROOT, "docs/redesign/quiet-paper.values.json").readText()).getJSONObject("colour")
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
            val opaque = value.uppercase().let { if (it.length == 9 && it.startsWith("#FF")) "#" + it.substring(3) else it }
            return if (opaque in foundationValues) FIXED_FOUNDATION_COLOUR else ""
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

    private fun String.isWhite() = uppercase() in setOf("#FFF", "#FFFFFF", "#FFFFFFFF", "@ANDROID:COLOR/WHITE", "@COLOR/WHITE")

    private fun isFoundationResource(token: String) = FOUNDATION_RESOURCE.matches(token)

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

        /** Graphics and code the sweep has not reached yet (part of the reported location) */
        private val NOT_YET_SWEPT =
            setOf(
                "shadow_left.xml",
                "ic_gesture_swipe.xml",
                "ic_gesture_tap_regular.xml",
                "add_shortcut.xml",
                "browse_shortcut.xml",
                "review_shortcut.xml",
                "badge_drawable.xml",
                "bg_preference_widget_text.xml",
                "bg_rounded_drop_shadow.xml",
                "brush_preview_background.xml",
                "color_picker_circle_indicator.xml",
                "divider_handle_background.xml",
                "launch_screen.xml",
                "pin_shape.xml",
                "tab_layout_indicator_floating_pill.xml",
                "drawer_item_background_dark.xml",
                "drawer_item_background_light.xml",
                "footer_button.xml",
                "footer_button_ripple.xml",
                "widgets:",
                "/java/",
            )

        /**
         * Graphics drawn by the launcher, not inside the app, so a theme attribute cannot reach them.
         * They use the Light theme's Foundation colours, as the launcher icon does.
         */
        private val DRAWN_WITHOUT_THE_APP_THEME =
            mapOf(
                "ic_launcher_background" to "the launcher icon",
                "ic_launcher_foreground" to "the launcher icon",
                "add_shortcut" to "a launcher shortcut, drawn with the launcher icon",
                "browse_shortcut" to "a launcher shortcut, drawn with the launcher icon",
                "review_shortcut" to "a launcher shortcut, drawn with the launcher icon",
            )

        /** Graphics whose colour only says where to paint: the system or the view that shows them tints them */
        private val MASKS =
            mapOf(
                "ic_launcher_monochrome" to "the launcher's themed icon: the launcher tints it",
                "ic_star_notify" to "the notification's small icon: the system draws only its shape (ticket 47)",
            )

        /**
         * Colours in code that are content, not interface: `<path suffix>#<token>`, or a whole file
         * when no token is given.
         */
        private val CONTENT_COLOURS =
            mapOf(
                "com/ichi2/anki/Whiteboard.kt" to "the old study screen's whiteboard: pen and ink colours the person picks",
                "com/ichi2/anki/ui/windows/reviewer/whiteboard/" to "the whiteboard: brush and ink colours the person picks",
            )
    }
}
