// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.launch

/** Renames flags individually, persisting each name when its Save action is selected. */
class FlagRenameDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) {
            val names by produceState(emptyMap<Flag, String>()) {
                value = Flag.queryDisplayNames(requireContext()).filterKeys { it != Flag.NONE }
            }
            PaperDialog(
                title = getString(CommonString.rename_flag),
                onDismissRequest = ::dismiss,
                actions = listOf(DialogAction(getString(CommonString.close), ::dismiss)),
            ) {
                names.forEach { (flag, initialName) ->
                    key(flag) {
                        var name by rememberSaveable { mutableStateOf(initialName) }
                        var draft by rememberSaveable { mutableStateOf<String?>(null) }
                        val focus = remember { FocusRequester() }
                        val edit = getString(CommonString.rename_flag)
                        if (draft == null) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(flag.drawableRes),
                                    null,
                                    Modifier.size(24.dp),
                                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                                )
                                Text(name, Modifier.weight(1f))
                                IconButton(onClick = { draft = name }) {
                                    Icon(painterResource(R.drawable.symbol_edit), edit)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = draft.orEmpty(),
                                onValueChange = { draft = it },
                                label = { Text(name) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                            androidx.compose.runtime.LaunchedEffect(focus) { focus.requestFocus() }
                            androidx.compose.foundation.layout.FlowRow {
                                TextButton(onClick = { draft = null }) { Text(getString(CommonString.dialog_cancel)) }
                                TextButton(onClick = {
                                    val newName = draft.orEmpty().ifEmpty { name }
                                    draft = null
                                    if (newName != name) {
                                        name = newName
                                        val manager = parentFragmentManager
                                        requireActivity().lifecycleScope.launch {
                                            flag.rename(newName)
                                            manager.setFragmentResult(REQUEST_RENAMED_FLAGS, Bundle())
                                        }
                                    }
                                }) { Text(getString(CommonString.save)) }
                            }
                        }
                    }
                }
            }
        }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.invalidateOptionsMenu()
        parentFragmentManager.setFragmentResult(REQUEST_RENAMED_FLAGS, Bundle())
    }

    companion object {
        internal const val REQUEST_RENAMED_FLAGS = "renamed_flags"
    }
}
