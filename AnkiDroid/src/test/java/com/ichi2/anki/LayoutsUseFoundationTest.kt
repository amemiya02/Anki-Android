// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

/**
 * Screens that have not been rebuilt yet still follow the Foundation: no layout, and no style
 * a layout names, fixes a colour or a text size of its own.
 *
 * - A colour comes from a theme attribute (a Foundation colour role, or a Material role the
 *   theme maps onto one), or from a colour state list built only from those. Transparent is
 *   no colour and is allowed. A hex value or a fixed `@color/` resource is not.
 * - A text size comes from a Foundation type style: `@style/TextAppearance.Foundation.*`, or a
 *   theme slot that the Light and Dark themes point at one. `android:textSize` is never set.
 *
 * `tools:` attributes only affect the layout preview and are not checked. A view whose colour is
 * something the person chose, not part of the interface, is listed in [SHOWS_A_COLOUR_THE_PERSON_CHOSE].
 */
class LayoutsUseFoundationTest {
    @Test
    fun `no layout sets a colour directly`() {
        val offending = problems().filter { it.kind == Kind.COLOUR }.map { it.toString() }
        assertEquals(emptyList(), offending, "${offending.size} hard-coded colours in layouts")
    }

    @Test
    fun `no layout sets a text size directly`() {
        val offending = problems().filter { it.kind == Kind.TEXT_SIZE }.map { it.toString() }
        assertEquals(emptyList(), offending, "${offending.size} fixed text sizes in layouts")
    }

    private enum class Kind { COLOUR, TEXT_SIZE }

    private data class Problem(
        val kind: Kind,
        val where: String,
        val what: String,
    ) {
        override fun toString() = "$where: $what"
    }

    private data class Style(
        val name: String,
        val parent: String?,
        val items: List<Pair<String, String>>,
    )

    private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /** Every style defined by the Fork's own modules, in any configuration (night, v28, ...) */
    private val styles: Map<String, List<Style>> by lazy {
        resourceDirs
            .flatMap { res -> res.listFiles { dir -> dir.name.matches(CONFIGURATION_VALUES) }.orEmpty().toList() }
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .flatMap { file ->
                builder
                    .parse(file)
                    .documentElement
                    .childElements()
                    .filter { it.tagName == "style" }
            }.map { element ->
                Style(
                    name = element.getAttribute("name"),
                    parent = if (element.hasAttribute("parent")) element.getAttribute("parent") else null,
                    items = element.childElements().map { it.getAttribute("name") to it.textContent.trim() }.toList(),
                )
            }.groupBy { it.name }
    }

    /** Colour state lists (`res/color/`) by name */
    private val colourStateLists: Map<String, File> by lazy {
        resourceDirs
            .flatMap { res -> res.listFiles { dir -> dir.name.startsWith("color") }.orEmpty().toList() }
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .associateBy { it.nameWithoutExtension }
    }

    /** Theme slots (`textAppearanceBodyLarge`, `android:textAppearanceSmall`, ...) which both themes point at a Foundation type style */
    private val foundationTypeSlots: Set<String> by lazy {
        fun slots(theme: String): Set<String> =
            styleChain(theme)
                .flatMap { it.items }
                .filter { (name, value) -> name.contains("textAppearance") && value.removePrefix("@style/").startsWith(FOUNDATION_TYPE) }
                .map { it.first }
                .toSet()
        slots("Theme_Light") intersect slots("Theme_Dark")
    }

    private fun styleChain(name: String): List<Style> {
        val own = styles[name] ?: return emptyList()
        val parents = own.mapNotNull { parentOf(it) }.distinct()
        return own + parents.flatMap { styleChain(it) }
    }

    /** The parent named explicitly, or implied by the name (`A.B` inherits `A`) */
    private fun parentOf(style: Style): String? {
        style.parent?.let { return it.removePrefix("@style/").removePrefix("@android:style/").ifEmpty { null } }
        return style.name.substringBeforeLast('.', "").ifEmpty { null }
    }

    private fun problems(): List<Problem> =
        layoutFiles().flatMap { file ->
            val where = "${moduleOf(file)}:${file.parentFile?.name}/${file.name}"
            builder
                .parse(file)
                .documentElement
                .descendantsAndSelf()
                .flatMap { element ->
                    val id = element.getAttribute("android:id").substringAfter('/')
                    val keepsColour = "${file.parentFile?.name}/${file.name}#$id" in SHOWS_A_COLOUR_THE_PERSON_CHOSE
                    element
                        .attributes()
                        .filter { (name, _) -> !name.startsWith("tools:") }
                        .flatMap { (name, value) ->
                            attributeProblems(name, value).map { (kind, why) ->
                                Problem(kind, where, "$name=\"$value\"$why")
                            }
                        }.filter { !(keepsColour && it.kind == Kind.COLOUR) }
                }.toList()
        }

    /** `AnkiDroid`, `common/android`, ...: the part of the path before `src/main/res` */
    private fun moduleOf(file: File): String = file.path.substringBefore("/src/main/res").removePrefix(ROOT.path + "/")

    private fun layoutFiles(): List<File> =
        resourceDirs
            .flatMap { res -> res.listFiles { dir -> dir.name.startsWith("layout") }.orEmpty().toList() }
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .sortedBy { it.path }
            .also { check(it.size > 100) { "only ${it.size} layouts found: is the working directory the module?" } }

    /** What is wrong with one layout attribute, if anything: the kind of problem and an explanation ("" when the value says it all) */
    private fun attributeProblems(
        name: String,
        value: String,
    ): List<Pair<Kind, String>> {
        val localName = name.substringAfter(':')
        return when {
            localName == "textSize" -> listOf(Kind.TEXT_SIZE to "")
            localName.endsWith("textAppearance", ignoreCase = true) ->
                listOfNotNull(
                    textAppearanceProblem(value)?.let {
                        Kind.TEXT_SIZE to
                            it
                    },
                )
            name == "style" -> styleReferenceProblems(value)
            else -> listOfNotNull(colourProblem(value)?.let { Kind.COLOUR to it })
        }
    }

    /** Why [value] is a colour fixed in place, or null if it follows the theme or is no colour at all */
    private fun colourProblem(
        value: String,
        seen: Set<String> = emptySet(),
    ): String? {
        if (HEX_COLOUR.matches(value)) return ""
        val match = COLOUR_RESOURCE.matchEntire(value) ?: return null
        val (namespace, colour) = match.destructured
        if (colour == "transparent") return null
        if (namespace.isNotEmpty()) return ""
        val stateList = colourStateLists[colour] ?: return ""
        if (colour in seen) return null
        val inner =
            builder
                .parse(stateList)
                .documentElement
                .descendantsAndSelf()
                .flatMap { it.attributes() }
                .filter { (attr, _) -> attr == "android:color" }
                .mapNotNull { (_, itemValue) -> colourProblem(itemValue, seen + colour)?.let { itemValue } }
                .toList()
        return if (inner.isEmpty()) null else " (its colour state list holds ${inner.joinToString()})"
    }

    /** Why a `textAppearance` is not a Foundation type style, or null if it is */
    private fun textAppearanceProblem(value: String): String? {
        val slot = THEME_REFERENCE.matchEntire(value)
        if (slot != null) {
            val (namespace, attr) = slot.destructured
            val key = if (namespace.isEmpty()) attr else "android:$attr"
            return if (key in foundationTypeSlots) null else " (the theme does not point this slot at a Foundation type style)"
        }
        val style = value.removePrefix("@style/").removePrefix("@android:style/")
        if (style.startsWith(FOUNDATION_TYPE)) return null
        if (!value.startsWith("@android:") && style in styles) {
            return styleProblems(style).firstOrNull { it.first == Kind.TEXT_SIZE }?.second
        }
        return " (not a Foundation type style)"
    }

    /** Problems with `style="..."` on a view */
    private fun styleReferenceProblems(value: String): List<Pair<Kind, String>> {
        if (value.startsWith("?")) {
            val attr = value.substringAfterLast('/').substringAfterLast(':').removePrefix("?")
            return if (attr.startsWith(
                    "textAppearance",
                )
            ) {
                listOfNotNull(textAppearanceProblem(value)?.let { Kind.TEXT_SIZE to it })
            } else {
                emptyList()
            }
        }
        val style = value.removePrefix("@style/").removePrefix("@android:style/")
        if (style.startsWith(FOUNDATION_TYPE)) return emptyList()
        if (!value.startsWith("@android:") && style in styles) return styleProblems(style)
        // A library widget style reads its colours and type from the theme
        return if (style.startsWith("TextAppearance")) listOf(Kind.TEXT_SIZE to " (not a Foundation type style)") else emptyList()
    }

    /** Problems with a style defined by the Fork, including those it inherits */
    private fun styleProblems(
        name: String,
        seen: Set<String> = emptySet(),
    ): List<Pair<Kind, String>> {
        if (name in seen || name.startsWith(FOUNDATION_TYPE)) return emptyList()
        val definitions =
            styles[name] ?: return if (name.startsWith("TextAppearance")) listOf(Kind.TEXT_SIZE to " (inherits $name)") else emptyList()
        return definitions
            .flatMap { style ->
                val own =
                    style.items.flatMap { (item, itemValue) ->
                        val itemName = item.substringAfter(':')
                        when {
                            itemName == "textSize" -> listOf(Kind.TEXT_SIZE to " (style $name sets $item=$itemValue)")
                            itemName.endsWith("textAppearance", ignoreCase = true) ->
                                listOfNotNull(
                                    textAppearanceProblem(itemValue)?.let {
                                        Kind.TEXT_SIZE to
                                            " (style $name sets $item=$itemValue)$it"
                                    },
                                )
                            else ->
                                listOfNotNull(
                                    colourProblem(itemValue)?.let { Kind.COLOUR to " (style $name sets $item=$itemValue)$it" },
                                )
                        }
                    }
                own + (parentOf(style)?.let { styleProblems(it, seen + name) } ?: emptyList())
            }.distinct()
    }

    private fun Element.childElements(): Sequence<Element> =
        (0 until childNodes.length).asSequence().map { childNodes.item(it) }.filterIsInstance<Element>()

    private fun Element.descendantsAndSelf(): Sequence<Element> = sequenceOf(this) + childElements().flatMap { it.descendantsAndSelf() }

    private fun Element.attributes(): Sequence<Pair<String, String>> =
        (0 until attributes.length).asSequence().map { attributes.item(it) }.map { it.nodeName to it.nodeValue }

    companion object {
        private const val FOUNDATION_TYPE = "TextAppearance.Foundation."

        private val ROOT = File("..").canonicalFile

        // test working directory is the module directory: `AnkiDroid/`
        private val resourceDirs =
            listOf(
                "src/main/res",
                "../common/android/src/main/res",
                "../widgets/src/main/res",
                "../anki-common/src/main/res",
                "../note-editor/src/main/res",
                "../api/src/main/res",
            ).map { File(it).canonicalFile }

        /** `values` folders for a configuration (night, API level, screen size), not a language */
        private val CONFIGURATION_VALUES = Regex("values(-(night|land|ldrtl|v\\d+|[swh]+\\d+dp))*")

        /**
         * Views whose colour is content, not interface: each pen swatch of the whiteboard shows the
         * ink it selects, so it keeps that ink's colour in every theme.
         */
        private val SHOWS_A_COLOUR_THE_PERSON_CHOSE =
            listOf("white", "black", "red", "green", "blue", "yellow")
                .map { "layout/include_reviewer_whiteboard_editor.xml#pen_color_$it" }
                .toSet()

        private val HEX_COLOUR = Regex("#[0-9a-fA-F]{3,8}")
        private val COLOUR_RESOURCE = Regex("@(android:)?color/([A-Za-z0-9_.]+)")
        private val THEME_REFERENCE = Regex("\\?(?:(android):)?(?:attr/)?([A-Za-z0-9_]+)")
    }
}
