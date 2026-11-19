// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.worker

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.await
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import anki.sync.MediaSyncProgress
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.NotificationChannel
import com.ichi2.anki.R
import com.ichi2.anki.cancelMediaSync
import com.ichi2.anki.common.android.withAppLocale
import com.ichi2.anki.common.permissions.canPostNotifications
import com.ichi2.anki.notifications.NotificationId
import com.ichi2.anki.notifications.hayakiNotification
import com.ichi2.anki.receiver.CopyToClipboardReceiver
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.sync.MeteredSyncPolicy
import com.ichi2.anki.sync.SyncAuth
import com.ichi2.anki.sync.syncMedia
import com.ichi2.anki.syncCertificate
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.trySetForeground
import com.ichi2.utils.TruncatedString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.exceptions.BackendInterruptedException
import net.ankiweb.rsdroid.exceptions.BackendNetworkException
import net.ankiweb.rsdroid.exceptions.BackendSyncException
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.ichi2.anki.common.android.R as CommonR

/**
 * Held while a media sync runs. A profile switch holds it until the process dies,
 * so a media sync cannot start part way through a restart.
 */
val mediaSyncLock = Mutex()

private const val MEDIA_SYNC_PROGRESS = "mediaSyncProgress"
private const val MEDIA_SYNC_ERROR = "mediaSyncError"
private const val MEDIA_SYNC_INTERRUPTED = "mediaSyncInterrupted"

internal val Data.mediaSyncProgressText: String? get() = getString(MEDIA_SYNC_PROGRESS)
internal val Data.mediaSyncErrorMessage: String? get() = getString(MEDIA_SYNC_ERROR)
internal val Data.mediaSyncWasInterrupted: Boolean get() = getBoolean(MEDIA_SYNC_INTERRUPTED, false)

class SyncMediaWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    private val cancelIntent by lazy {
        val intent =
            Intent(applicationContext, CancelMediaSyncReceiver::class.java)
                .setAction(id.toString())
                .putExtra("session", inputData.getString(SYNC_SESSION_KEY).orEmpty())
                .putExtra(
                    "profile",
                    AnkiDroidApp.instance.profileManager
                        ?.activeProfileContext
                        ?.profileId
                        ?.value,
                )
        PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
    private val notificationManager: NotificationManagerCompat? =
        if (canPostNotifications(context)) {
            NotificationManagerCompat.from(context)
        } else {
            null
        }

    private val notificationContext get() = applicationContext.withAppLocale()

    override suspend fun doWork(): Result {
        Timber.v("SyncMediaWorker::doWork")
        return mediaSyncLock.withLockUnlessSwitching { doWorkHoldingLock() }
    }

    private suspend fun doWorkHoldingLock(): Result {
        try {
            val auth = inputData.toSyncAuth() ?: return Result.failure()
            if (Prefs.mediaSyncPaused) return Result.success()
            if (!CollectionManager.updateCustomCertificate(syncCertificate())) return Result.failure()

            // The collection must be open, but we should not block collection operations while
            // `syncMedia` is executing, the app should be usable during a background media sync
            val backend = CollectionManager.getColUnsafe().backend
            backend.syncMedia(auth)

            delay(1.seconds) // avoid notifications if sync occurs too quickly
            monitorProgress(backend)
        } catch (cancellationException: CancellationException) {
            Timber.w(cancellationException, "SyncMediaWorker cancelled (user tapped Cancel or WorkManager cancelled)")
            notificationManager?.cancel(NotificationId.SYNC_MEDIA)
            Timber.d("SyncMediaWorker: progress notification cancelled after worker cancellation")
            cancelMediaSync(CollectionManager.getBackend())
            throw cancellationException
        } catch (throwable: Throwable) {
            // The engine wraps a dropped response body in a generic sync error.
            val droppedResponse =
                throwable.javaClass == BackendSyncException::class.java &&
                    throwable.message?.startsWith("HttpError { code: 303, context: \"reading stream\"") == true
            if ((throwable is BackendNetworkException || droppedResponse) && runAttemptCount < MAX_NETWORK_RETRIES) {
                Timber.i("Media sync interrupted, retrying after backoff")
                notificationManager?.cancel(NotificationId.SYNC_MEDIA)
                return Result.retry()
            }
            Timber.w(throwable, "SyncMediaWorker failed")
            val message = throwable.localizedMessage
            notify {
                // TODO: add a contentIntent, so tapping the notification opens the app
                //  (ideally showing the media sync log: an AlertDialog in DeckPicker)
                setContentTitle(notificationContext.getString(R.string.notification_sync_media_failed))
                message?.let {
                    setContentText(message)
                    setStyle(
                        NotificationCompat
                            .BigTextStyle()
                            .bigText(message),
                    )
                    addAction(
                        R.drawable.symbol_content_copy,
                        with(notificationContext) { TR.sentenceCase.copyToClipboard },
                        getCopyToClipboardIntent(TruncatedString.from(message)),
                    )
                }
            }
            Timber.d("SyncMediaWorker: showing failure notification")
            return Result.failure(
                workDataOf(
                    // Reserve space for UTF-8 encoding and metadata within WorkManager's Data limit.
                    MEDIA_SYNC_ERROR to message?.take(Data.MAX_DATA_BYTES / 4),
                    MEDIA_SYNC_INTERRUPTED to (throwable is BackendInterruptedException),
                ),
            )
        }
        Timber.d("SyncMediaWorker: cancelling progress notification (sync completed)")
        notificationManager?.cancel(NotificationId.SYNC_MEDIA)

        Timber.d("SyncMediaWorker: success")
        return Result.success()
    }

    private suspend fun monitorProgress(backend: Backend) {
        var syncProgress: MediaSyncProgress? = null
        while (true) {
            // Reading the engine's terminal result consumes it. UI observes WorkManager instead.
            val status = backend.mediaSyncStatus()
            if (!status.active) {
                Timber.i("Ended media sync notification updates")
                break
            }
            if (syncProgress == null) {
                Timber.i("Showing SyncMediaWorker's notification")
                trySetForeground(getForegroundInfo())
            }
            if (syncProgress != status.progress) {
                syncProgress = status.progress
                setProgress(workDataOf(MEDIA_SYNC_PROGRESS to syncProgress.run { "$added\n$removed\n$checked" }))
                // TODO display better the result. Using setContentText leads to
                //  truncated text if it has more than two lines.
                // `added`, `removed` and `checked` already come translated from the
                val notificationText = syncProgress.run { "$added $removed $checked" }
                notify(getProgressNotification(notificationText))
            }
            delay(NOTIFICATION_UPDATE_RATE)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = notificationContext.getString(R.string.notification_sync_media_title)
        val cancelTitle = TR.syncAbortButton()
        val notification =
            buildNotification {
                setContentTitle(title)
                setOngoing(true)
                setProgress(0, 0, true)
                addAction(CommonR.drawable.symbol_close, cancelTitle, cancelIntent)
                foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_DEFERRED
            }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationId.SYNC_MEDIA, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationId.SYNC_MEDIA, notification)
        }
    }

    @VisibleForTesting
    internal fun getCopyToClipboardIntent(text: TruncatedString): PendingIntent {
        val intent = CopyToClipboardReceiver.getIntent(applicationContext, text)
        return PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(notification: Notification) = notificationManager?.notify(NotificationId.SYNC_MEDIA, notification)

    private fun notify(builder: NotificationCompat.Builder.() -> Unit) {
        notify(buildNotification(builder))
    }

    private fun buildNotification(block: NotificationCompat.Builder.() -> Unit): Notification =
        notificationContext
            .hayakiNotification(NotificationChannel.SYNC)
            .apply {
                priority = NotificationCompat.PRIORITY_LOW
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                setSilent(true)
                block()
            }.build()

    private fun getProgressNotification(progress: CharSequence): Notification {
        val title = notificationContext.getString(R.string.notification_sync_media_title)
        val cancelTitle = TR.syncAbortButton()

        return buildNotification {
            setContentTitle(title)
            setContentText(progress)
            setProgress(0, 0, true)
            setOngoing(true)
            addAction(CommonR.drawable.symbol_close, cancelTitle, cancelIntent)
        }
    }

    companion object {
        private const val MAX_NETWORK_RETRIES = 5
        val NOTIFICATION_UPDATE_RATE = 500.milliseconds

        /** Ensure a metered network request approval only affects the intended request. */
        private val schedulingLock = Mutex()

        fun getWorkRequest(
            auth: SyncAuth,
            networkType: NetworkType = MeteredSyncPolicy.getNetworkTypeRequiredForSync(forMedia = true),
            id: UUID = UUID.randomUUID(),
            session: String = Prefs.syncSession,
        ): OneTimeWorkRequest {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(networkType)
                    .build()

            return OneTimeWorkRequestBuilder<SyncMediaWorker>()
                .setId(id)
                .setInputData(auth.toWorkData(session))
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }

        /**
         * A later metered approval must also release media already waiting for Wi-Fi.
         * Updating the existing request preserves any sync that has started in the meantime.
         */
        suspend fun start(
            context: Context,
            auth: SyncAuth,
            networkType: NetworkType = MeteredSyncPolicy.getNetworkTypeRequiredForSync(forMedia = true),
            session: String = Prefs.syncSession,
        ) {
            schedulingLock.withLock {
                if (session != Prefs.syncSession || Prefs.mediaSyncPaused) return
                Timber.i("Launching background media sync")
                val request = getWorkRequest(auth, networkType, session = session)

                val manager = WorkManager.getInstance(context)
                manager.enqueueUniqueWork(UniqueWorkNames.SYNC_MEDIA, ExistingWorkPolicy.KEEP, request).result.await()
                if (networkType == NetworkType.CONNECTED) {
                    manager.allowMeteredMediaSync(auth, session)
                }
            }
        }

        /**
         * Applies a new request's permission to use metered networks to retained media work.
         *
         * If a new request allows metered networks, it does not overwrite the old work due to
         * [ExistingWorkPolicy.KEEP], so explicitly update the work request.
         *
         * Call after `enqueueUniqueWork` has completed while holding [schedulingLock].
         */
        private suspend fun WorkManager.allowMeteredMediaSync(
            auth: SyncAuth,
            session: String,
        ) {
            val existing =
                getWorkInfosForUniqueWork(UniqueWorkNames.SYNC_MEDIA).await().firstOrNull {
                    !it.state.isFinished && it.constraints.requiredNetworkType == NetworkType.UNMETERED
                } ?: return
            if (session != Prefs.syncSession) return
            val result = updateWork(getWorkRequest(auth, NetworkType.CONNECTED, existing.id, session)).await()
            Timber.i("Allowing metered media sync for work %s: %s", existing.id, result)
        }
    }
}

/** Only an explicit Cancel action pauses future automatic media syncs for this profile. */
class CancelMediaSyncReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.getStringExtra("session") != Prefs.syncSession ||
            intent.getStringExtra("profile") !=
            AnkiDroidApp.instance.profileManager
                ?.activeProfileContext
                ?.profileId
                ?.value
        ) {
            return
        }
        val id = intent.action?.let(UUID::fromString) ?: return
        Prefs.mediaSyncPaused = true
        WorkManager.getInstance(context).cancelWorkById(id)
    }
}
