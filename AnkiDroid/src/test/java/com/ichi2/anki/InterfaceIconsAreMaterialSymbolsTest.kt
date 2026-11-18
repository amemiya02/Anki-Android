// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import org.json.JSONObject
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

/**
 * Every interface icon is a Material Symbol in the one style the Foundation names (the `icons`
 * entry of `docs/redesign/quiet-paper.values.json`), coloured by the theme, and mirrored in a
 * right-to-left language when it carries direction.
 *
 * An icon is a vector drawable `symbol_<name>.xml` made by `tools/material-symbols/symbol.py`, which
 * records each symbol it downloaded from Google in `tools/material-symbols/symbols.lock`: whether
 * Google mirrors it, and a digest of its path data. A drawable whose shape differs from the lock
 * was drawn or edited by hand.
 *
 * Vector drawables that are not interface icons (the launcher icon, illustrations, graphics) are
 * listed in [NOT_INTERFACE_ICONS] with the reason.
 */
class InterfaceIconsAreMaterialSymbolsTest {
    @Test
    fun `every interface icon is a Material Symbol in the Foundation's style`() {
        val offending = icons().mapNotNull { it.shapeProblem() } + lockStyleProblems() + themeIconProblems()
        assertEquals(emptyList(), offending, "${offending.size} icons are not Material Symbols in the Foundation's style")
    }

    @Test
    fun `no interface icon carries its own colour`() {
        val offending = icons().mapNotNull { it.colourProblem() }
        assertEquals(emptyList(), offending, "${offending.size} icons carry their own colour")
    }

    @Test
    fun `icons that carry direction mirror in a right-to-left language`() {
        val offending = icons().mapNotNull { it.mirrorProblem() }
        assertEquals(emptyList(), offending, "${offending.size} icons mirror wrongly")
    }

    @Test
    fun `no screen uses a platform icon`() {
        val offending =
            sourceDirs
                .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension in setOf("xml", "kt", "java") }.toList() }
                .flatMap { file ->
                    PLATFORM_ICON
                        .findAll(file.readText())
                        .map { "${file.relativeTo(ROOT)}: ${it.value}" }
                        .toList()
                }
        assertEquals(emptyList(), offending, "${offending.size} platform icons")
    }

    private class Lock(
        val style: Map<String, String>,
        val symbols: Map<String, Pair<Boolean, String>>,
    )

    private inner class Icon(
        val file: File,
    ) {
        val where = "${file.path.substringBefore("/src/main/res").removePrefix(ROOT.path + "/")}:${file.parentFile?.name}/${file.name}"
        val text = file.readText()
        val root: Element = builder.parse(file).documentElement

        /** The symbol named in the comment the script writes, or null if the drawable was not made by it */
        val symbol: String? = Regex("${Regex.escape(style.getString("family"))}: ([a-z0-9_]+)").find(text)?.groupValues?.get(1)

        val paths: List<Element> = root.descendants().filter { it.tagName == "path" }.toList()

        fun shapeProblem(): String? {
            if (symbol == null) return "$where: not drawn from Material Symbols (no `${style.getString("family")}: <name>` comment)"
            val locked = lock.symbols[symbol] ?: return "$where: $symbol is not in symbols.lock"
            val name = file.nameWithoutExtension
            if (name != "symbol_$symbol" && !name.startsWith("symbol_${symbol}_")) return "$where: should be named symbol_$symbol"
            val size = "${style.getInt("size")}dp"
            val frame =
                listOf(
                    "android:width" to size,
                    "android:height" to size,
                    "android:viewportWidth" to "960",
                    "android:viewportHeight" to "960",
                ).filter { (attr, value) -> root.getAttribute(attr) != value }
            if (frame.isNotEmpty()) return "$where: ${frame.joinToString { (attr, value) -> "$attr should be $value" }}"
            val groups = root.childElements().toList()
            if (groups.size != 1 || groups[0].tagName != "group" || groups[0].getAttribute("android:translateY") != "960") {
                return "$where: the paths should sit in one <group android:translateY=\"960\">"
            }
            val digest = sha256(paths.joinToString("\n") { it.getAttribute("android:pathData") })
            if (digest != locked.second) return "$where: the shape of $symbol differs from Google's (edited by hand?)"
            return null
        }

        fun colourProblem(): String? {
            val tint = root.getAttribute("android:tint")
            val flagColour = FLAG_COLOUR.matchEntire(file.nameWithoutExtension)?.groupValues?.get(1)
            val tintAllowed = THEME_ATTRIBUTE.matches(tint) || (flagColour != null && tint == "@color/flag_reviewer_$flagColour")
            if (!tintAllowed) return "$where: tinted \"$tint\", not a theme colour role"
            val colours =
                root
                    .descendants()
                    .flatMap { it.attributes() }
                    .filter { (attr, value) -> attr.endsWith("Color") && value != MASK }
                    .toList()
            if (colours.isNotEmpty()) return "$where: ${colours.joinToString { (attr, value) -> "$attr=\"$value\"" }}"
            if (root.descendants().any { it.tagName == "gradient" }) return "$where: holds a gradient"
            return null
        }

        fun mirrorProblem(): String? {
            val mirrored =
                symbol?.let { lock.symbols[it]?.first }
                    ?: return "$where: not a known Material Symbol, so whether it carries direction is unknown"
            val mirrors = root.getAttribute("android:autoMirrored") == "true"
            return when {
                mirrored && !mirrors -> "$where: $symbol carries direction but does not mirror"
                !mirrored && mirrors -> "$where: $symbol has no direction but mirrors"
                else -> null
            }
        }
    }

    private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    private val style: JSONObject by lazy {
        JSONObject(File(ROOT, "docs/redesign/quiet-paper.values.json").readText()).getJSONObject("icons")
    }

    private val lock: Lock by lazy {
        val lines = File(ROOT, "tools/material-symbols/symbols.lock").readLines()
        val header = lines.single { it.startsWith("# style ") }
        Lock(
            style = Regex("(\\w+)=(\"[^\"]*\"|\\S+)").findAll(header).associate { it.groupValues[1] to it.groupValues[2].trim('"') },
            symbols =
                lines
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.split(" ") }
                    .associate { (symbol, direction, digest) -> symbol to ((direction == "mirrored") to digest) },
        )
    }

    /** The lock records symbols in the style the Foundation names */
    private fun lockStyleProblems(): List<String> =
        listOf("family", "weight", "fill", "grade", "opticalSize")
            .filter { lock.style[it] != style.get(it).toString() }
            .map { "symbols.lock: $it is ${lock.style[it]}, the Foundation says ${style.get(it)}" }

    /** Icons the platform draws for the app (the up arrow, the overflow menu, the search field) come from the theme */
    private fun themeIconProblems(): List<String> {
        val items =
            resourceDirs
                .flatMap { res -> res.listFiles { dir -> dir.name == "values" }.orEmpty().toList() }
                .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
                .flatMap {
                    builder
                        .parse(it)
                        .documentElement
                        .childElements()
                        .filter { e -> e.tagName == "style" }
                        .toList()
                }.associate { style ->
                    style.getAttribute("name") to
                        style.childElements().associate { it.getAttribute("name") to it.textContent.trim() }
                }
        return listOf("Base.Theme.Light", "Base.Theme.Dark").flatMap { theme ->
            val own = items[theme].orEmpty()
            val overflow = items[own["actionOverflowButtonStyle"]?.removePrefix("@style/")].orEmpty()["android:src"]
            val search = items[own["searchViewStyle"]?.removePrefix("@style/")].orEmpty()
            listOf(
                "homeAsUpIndicator" to own["homeAsUpIndicator"],
                "actionOverflowButtonStyle android:src" to overflow,
                "searchViewStyle searchIcon" to search["searchIcon"],
                "searchViewStyle closeIcon" to search["closeIcon"],
            ).filter { (_, value) -> value?.startsWith("@drawable/symbol_") != true }
                .map { (slot, value) -> "$theme: $slot is $value, not a Material Symbol" }
        }
    }

    private fun icons(): List<Icon> =
        resourceDirs
            .flatMap { res -> res.listFiles { dir -> dir.name.startsWith("drawable") }.orEmpty().toList() }
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .also { check(it.size > 100) { "only ${it.size} drawables found: is the working directory the module?" } }
            .filter { it.nameWithoutExtension !in NOT_INTERFACE_ICONS }
            .filter { builder.parse(it).documentElement.tagName == "vector" }
            .sortedBy { it.path }
            .map { Icon(it) }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    private fun Element.childElements(): Sequence<Element> =
        (0 until childNodes.length).asSequence().map { childNodes.item(it) }.filterIsInstance<Element>()

    private fun Element.descendants(): Sequence<Element> = sequenceOf(this) + childElements().flatMap { it.descendants() }

    private fun Element.attributes(): Sequence<Pair<String, String>> =
        (0 until attributes.length).asSequence().map { attributes.item(it) }.map { it.nodeName to it.nodeValue }

    companion object {
        private val ROOT = File("..").canonicalFile

        private val modules = listOf("AnkiDroid", "common/android", "widgets", "anki-common", "note-editor", "api")

        // test working directory is the module directory: `AnkiDroid/`
        private val resourceDirs = modules.map { File(ROOT, "$it/src/main/res") }

        private val sourceDirs = modules.map { File(ROOT, "$it/src/main") }

        /** The white the icon is drawn in, so that its tint alone gives its colour */
        private const val MASK = "@android:color/white"

        private val THEME_ATTRIBUTE = Regex("\\?(android:)?(attr/)?[A-Za-z0-9_]+")

        /** `symbol_flag_red`: a Flag's icon is drawn in the Flag's colour, which is what it shows */
        private val FLAG_COLOUR = Regex("symbol_flag_([a-z]+)")

        private val PLATFORM_ICON = Regex("@android:drawable/\\w+|(?<![\\w.])android\\.R\\.drawable\\.\\w+")

        /** Vector drawables that are not interface icons, and why */
        private val NOT_INTERFACE_ICONS =
            mapOf(
                "ic_launcher_background" to "the launcher icon",
                "ic_launcher_foreground" to "the launcher icon",
                "ic_launcher_monochrome" to "the launcher icon",
                "add_shortcut" to "a launcher shortcut, drawn with the launcher icon's colours",
                "browse_shortcut" to "a launcher shortcut, drawn with the launcher icon's colours",
                "review_shortcut" to "a launcher shortcut, drawn with the launcher icon's colours",
                "hayaki_mark" to "Hayaki's mark: the empty deck list, first run, drawer and sync account (GraphicsUseFoundationTest)",
                "ic_hayaki_notification" to "the notification's small icon (ticket 47)",
                "widget_add_note_mark" to "a home-screen widget's picture (GraphicsUseFoundationTest)",
                "widget_small_mark" to "a home-screen widget's picture (GraphicsUseFoundationTest)",
                "widget_small_mark_done" to "a home-screen widget's picture (GraphicsUseFoundationTest)",
                "pin_shape" to "the shape behind the colour picker's swatch, not an icon",
                "ic_gesture_tap_regular" to "a tap zone drawn in the gesture picker, not an icon",
                "ic_gesture_tap_selected" to "a tap zone drawn in the gesture picker, not an icon",
            )
    }
}
