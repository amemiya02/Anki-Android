// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun SettingsHomeScreen(
    groups: List<SettingsGroup>,
    selectedGroup: SettingsGroup?,
    index: List<SettingsSearchEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onGroup: (SettingsGroup) -> Unit,
    onResult: (SettingsSearchEntry) -> Unit,
) {
    val results = remember(query, index) { index.matchingSettings(query) }
    SettingsPage(title = stringResource(CommonString.settings), onBack = onBack) {
        SettingsSearchField(query, onQueryChange)
        if (query.isBlank()) {
            SettingsGroupList(groups, selectedGroup, onGroup)
        } else {
            SettingsResults(results, query, onResult)
        }
    }
}

@Composable
private fun SettingsGroupList(
    groups: List<SettingsGroup>,
    selectedGroup: SettingsGroup?,
    onGroup: (SettingsGroup) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
        item { SettingsDivider() }
        items(groups, key = { it.name }) { group ->
            SettingsRow(
                title = stringResource(group.title),
                summary = stringResource(group.summary),
                icon = group.icon,
                onClick = { onGroup(group) },
                selected = group == selectedGroup,
                trailing = { SettingsChevron() },
            )
        }
    }
}

@Composable
private fun ColumnScope.SettingsResults(
    results: List<SettingsSearchEntry>,
    query: String,
    onResult: (SettingsSearchEntry) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Text(
        if (results.isEmpty()) {
            stringResource(R.string.settings_search_empty)
        } else {
            pluralStringResource(R.plurals.settings_search_results, results.size, results.size)
        },
        style = foundation.type.caption,
        color = foundation.colors.onBackgroundMuted,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
    if (results.isEmpty()) return
    LazyColumn(Modifier.weight(1f)) {
        item { SettingsDivider() }
        items(results, key = { it.id }) { entry ->
            SettingsRow(
                title = highlightSearchMatch(entry.title, query),
                summary = highlightSearchMatch(entry.path, query),
                value = entry.matchedDetails(query)?.let { highlightSearchMatch(it, query) },
                onClick = { onResult(entry) },
                trailing = { SettingsChevron() },
            )
        }
    }
}

@Composable
private fun SettingsSearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val prompt = stringResource(R.string.settings_search)
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = query,
        onValueChange = onChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = prompt },
        singleLine = true,
        textStyle = foundation.type.body.copy(color = foundation.colors.onBackground),
        cursorBrush = SolidColor(foundation.colors.emphasis),
        decorationBox = { editableText ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(
                        foundation.shapes.hairline,
                        if (focused) foundation.colors.emphasis else foundation.colors.outline,
                        foundation.shapes.button,
                    ).padding(start = 14.dp, end = if (query.isEmpty()) 14.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painterResource(CommonR.drawable.symbol_search),
                    contentDescription = null,
                    tint = foundation.colors.onBackgroundMuted,
                    modifier = Modifier.size(20.dp),
                )
                Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    if (query.isEmpty()) {
                        Text(prompt, style = foundation.type.body, color = foundation.colors.onBackgroundMuted)
                    }
                    editableText()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(
                            painterResource(CommonR.drawable.symbol_close),
                            contentDescription = stringResource(R.string.settings_search_clear),
                            tint = foundation.colors.onBackgroundMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    )
}

private fun highlightSearchMatch(
    text: String,
    query: String,
) = buildAnnotatedString {
    append(text)
    for (term in query.searchTerms()) {
        var start = text.indexOf(term, ignoreCase = true)
        while (start >= 0) {
            val end = start + term.length
            addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, end)
            start = text.indexOf(term, startIndex = end, ignoreCase = true)
        }
    }
}
