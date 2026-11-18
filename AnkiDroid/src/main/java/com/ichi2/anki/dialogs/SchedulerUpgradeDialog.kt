// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Mohd Raghib <raghib.khan76@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.DialogFragment
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.android.AdaptionUtil
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.createPaperDialog

/**
 * A reusable, non-dismissible dialog that prompts the user to upgrade the scheduler.
 *
 * This dialog is intended to be shown when the collection is using an outdated scheduler (v1),
 * and prevents interaction with the app until the upgrade is acknowledged or canceled.
 *
 * Reports the decision to the current activity without retaining an activity-owned callback.
 */

class SchedulerUpgradeDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return createPaperDialog(requireContext()) {
            var submitting by remember { mutableStateOf(false) }
            ConfirmationDialog(
                title = getString(CommonString.scheduler_upgrade_required_dialog_title),
                message = TR.schedulingUpdateRequired(),
                confirm =
                    DialogAction(getString(CommonString.dialog_ok), {
                        if (!submitting) {
                            submitting = true
                            reportDecision(upgrade = true)
                        }
                    }, enabled = !submitting),
                onDismissRequest = { reportDecision(upgrade = false) },
            ) {
                if (AdaptionUtil.hasWebBrowser(requireActivity())) {
                    TextButton(onClick = { requireActivity().openUrl(R.string.link_scheduler_upgrade_faq) }) {
                        Text(TR.schedulingUpdateMoreInfoButton())
                    }
                }
            }
        }.apply {
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode != KeyEvent.KEYCODE_BACK || event.action != KeyEvent.ACTION_UP) {
                    return@setOnKeyListener false
                }
                reportDecision(upgrade = false)
                true
            }
        }
    }

    private fun reportDecision(upgrade: Boolean) {
        val manager = parentFragmentManager
        dismiss()
        manager.setFragmentResult(REQUEST_SCHEDULER_UPGRADE, Bundle().apply { putBoolean(RESULT_UPGRADE, upgrade) })
    }

    companion object {
        const val TAG = "schedulerUpgrade"
        const val REQUEST_SCHEDULER_UPGRADE = "request_scheduler_upgrade"
        const val RESULT_UPGRADE = "upgrade"
    }
}
