// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.testutils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.multimedia.MultimediaActivity

/** Adds the same payload to the intent and, when applicable, the hosted fragment's arguments. */
fun Intent.withLaunchPayload(
    activityClass: Class<out Activity>,
    key: String,
    payload: ByteArray,
): Intent =
    apply {
        putExtra(key, payload)
        // Multimedia passes a file reference under the same key as single-fragment argument bundles.
        if (activityClass != MultimediaActivity::class.java && SingleFragmentActivity::class.java.isAssignableFrom(activityClass)) {
            val arguments = getBundleExtra(SingleFragmentActivity.EXTRA_FRAGMENT_ARGS) ?: Bundle()
            arguments.putByteArray(key, payload)
            putExtra(SingleFragmentActivity.EXTRA_FRAGMENT_ARGS, arguments)
        }
    }
