// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.tags

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.compose.theme.foundation
import com.ichi2.utils.TagsUtil

/** Keep the Engine's tri-state selection separate from expansion and filtering. */
internal class TagPickerState(
    val tags: TagsList,
) {
    private val tree = TagTree(tags)
    var revision by mutableIntStateOf(0)
        private set

    val size get() = tree.size

    operator fun get(index: Int) = requireNotNull(tree[index])

    fun state(node: TagTree.Node) = tree.state(node)

    fun filter(
        query: String,
        expandTarget: String = "",
    ) {
        val sorted = tags.toList()
        tree.publishResults(if (query.isEmpty()) sorted else tree.filterResults(query, sorted), expandTarget)
        revision++
    }

    fun toggle(node: TagTree.Node) {
        tree.toggleChecked(node)
        revision++
    }

    fun expand(node: TagTree.Node) {
        tree.toggleExpanded(node)
        revision++
    }

    fun toggleAll() {
        if (tags.toggleAllCheckedStatuses()) {
            tree.refreshSelection()
            revision++
        }
    }
}

@Composable
internal fun TagPickerList(
    state: TagPickerState,
    onChecked: () -> Unit,
    onAddChild: ((String) -> Unit)?,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    val revision = state.revision
    LazyColumn(modifier.fillMaxWidth().testTag("tag_list")) {
        item { header() }
        items(state.size, key = { state[it].tag }) { index ->
            val node = androidx.compose.runtime.remember(state, revision, index) { state[index] }
            val checkState =
                when (state.state(node)) {
                    TagTree.State.CHECKED -> ToggleableState.On
                    TagTree.State.INDETERMINATE -> ToggleableState.Indeterminate
                    TagTree.State.UNCHECKED -> ToggleableState.Off
                }

            fun check() {
                state.toggle(node)
                onChecked()
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = foundation.spacing.touchTarget)
                    .then(
                        if (onAddChild == null) {
                            Modifier
                        } else {
                            Modifier.pointerInput(node.tag, onAddChild) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                            event.changes.forEach { it.consume() }
                                            onAddChild(node.tag)
                                        }
                                    }
                                }
                            }
                        },
                    ).combinedClickable(
                        onLongClick = onAddChild?.let { { it(node.tag) } },
                        onClick = { if (node.isNotLeaf()) state.expand(node) else check() },
                    ).padding(start = (node.level.coerceIn(0, 4) * 10).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (node.isNotLeaf()) {
                    val description = stringResource(CommonString.expand_tag, node.tag.replace("::", " "))
                    Text(if (node.isExpanded) "−" else "+", Modifier.semantics { contentDescription = description })
                }
                Text(
                    TagsUtil.getTagParts(node.tag).last(),
                    Modifier.weight(1f),
                    style = foundation.type.rowTitle,
                    color = foundation.colors.onBackground,
                )
                TriStateCheckbox(checkState, onClick = ::check, modifier = Modifier.semantics { contentDescription = node.tag })
            }
        }
    }
}
