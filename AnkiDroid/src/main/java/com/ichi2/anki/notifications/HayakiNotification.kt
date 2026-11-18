// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.ichi2.anki.NotificationChannel
import com.ichi2.anki.R
import com.ichi2.anki.common.android.R as CommonR

/** Android supplies the notification's app name from the manifest. */
fun Context.hayakiNotification(channel: NotificationChannel): NotificationCompat.Builder =
    NotificationCompat
        .Builder(this, channel.id)
        .setSmallIcon(R.drawable.ic_hayaki_notification)
        .setColor(getColor(CommonR.color.foundation_light_primary))
