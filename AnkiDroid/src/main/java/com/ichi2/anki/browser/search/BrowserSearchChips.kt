// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser.search

import com.ichi2.anki.Flag
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.DeckNameId

/**
 * A chip only represents a positive term or an OR group of choices within one filter.
 * Leave negations, wildcards and mixed expressions in the query for the Engine to handle.
 */
context(col: Collection)
internal fun SearchRequest.populateChips(): SearchRequest {
    val normalized = SearchString.fromUserInput(query).getOrNull()?.value ?: return this
    val terms = normalized.topLevelTerms()
    if ("OR" in terms) {
        // The Engine removes redundant outer parentheses. A whole-query OR group can
        // still be represented by one chip when every alternative belongs to that chip.
        val choices = terms.disjunctionChoices() ?: return this
        val replacement = filters.withChoices(choices) ?: return this
        return SearchRequest("", replacement)
    }

    var updated = filters
    val remaining =
        terms.filter { term ->
            val choices =
                if (term.startsWith('(') && term.endsWith(')')) {
                    term.substring(1, term.lastIndex).topLevelTerms().disjunctionChoices() ?: return@filter true
                } else {
                    listOf(term)
                }
            val replacement = updated.withChoices(choices) ?: return@filter true
            updated = replacement
            false
        }
    // Keep the original spelling and spacing when nothing was extracted.
    if (updated == filters) return this
    return SearchRequest(remaining.filter { it != "AND" }.joinToString(" "), updated)
}

private fun List<String>.disjunctionChoices(): List<String>? {
    if (size < 3 || size % 2 == 0 || indices.any { it % 2 == 1 && this[it] != "OR" }) return null
    return filterIndexed { index, _ -> index % 2 == 0 }
}

context(col: Collection)
private fun SearchFilters.withChoices(terms: List<String>): SearchFilters? {
    fun <T> choices(resolve: (String) -> T?): List<T>? = terms.mapNotNull(resolve).takeIf { it.size == terms.size }?.distinct()

    if (flags.isEmpty() && terms.all { it.startsWith("flag:") }) {
        val flags = choices { term -> Flag.entries.firstOrNull { term == "flag:${it.code}" } }
        return flags?.let { copy(flags = it) }
    }
    if (cardStates.isEmpty() && terms.all { it.startsWith("is:") || it == "tag:marked" }) {
        val byTerm = CardState.entries.associateBy { col.buildSearchString(listOf(it.toSearchNode())) }
        return choices(byTerm::get)?.let { copy(cardStates = it) }
    }
    if (decks.isEmpty() && terms.all { it.trimStart('"').startsWith("deck:") }) {
        val decks =
            choices { term ->
                val name = term.removeSurrounding("\"").removePrefix("deck:").unescapeSearchTerm()
                val id = col.decks.idForName(name) ?: return@choices null
                val deck = DeckNameId(name = col.decks.name(id), id = id)
                val rebuilt = SearchRequest("", SearchFilters.partial(decks = listOf(deck))).toSearchString().getOrNull()?.value
                deck.takeIf { rebuilt == term }
            }
        return decks?.let { copy(decks = it) }
    }
    if (tags.isEmpty() && terms.all { it.trimStart('"').startsWith("tag:") }) {
        val tags =
            choices { term ->
                val tag = term.removeSurrounding("\"").removePrefix("tag:").unescapeSearchTerm()
                val rebuilt = SearchRequest("", SearchFilters.partial(tags = listOf(tag))).toSearchString().getOrNull()?.value
                tag.takeIf { '*' !in tag && '_' !in tag && tag != "none" && rebuilt == term }
            }
        return tags?.let { copy(tags = it) }
    }
    return null
}

private fun String.unescapeSearchTerm(): String = Regex("""\\(.)""").replace(this) { it.groupValues[1] }

/** The Engine normalizes the query first. Whitespace inside a quoted term or group is significant. */
private fun String.topLevelTerms(): List<String> =
    buildList {
        var start = 0
        var depth = 0
        var quoted = false
        var escaped = false
        this@topLevelTerms.forEachIndexed { index, char ->
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> quoted = !quoted
                !quoted && char == '(' -> depth++
                !quoted && char == ')' -> depth--
                !quoted && depth == 0 && char.isWhitespace() -> {
                    if (index > start) add(substring(start, index))
                    start = index + 1
                }
            }
        }
        if (start < length) add(substring(start))
    }
