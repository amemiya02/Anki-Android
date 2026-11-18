// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.utils

import android.content.DialogInterface
import android.widget.Button
import androidx.appcompat.app.AlertDialog

val AlertDialog.positiveButton: Button
    get() = getButton(DialogInterface.BUTTON_POSITIVE)

val AlertDialog.negativeButton: Button
    get() = getButton(DialogInterface.BUTTON_NEGATIVE)

val AlertDialog.neutralButton: Button?
    get() = getButton(DialogInterface.BUTTON_NEUTRAL)
