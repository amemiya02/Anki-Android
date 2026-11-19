// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import anki.collection.Progress
import anki.sync.SyncCollectionResponse
import com.ichi2.anki.BackupManager
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.NotificationChannel
import com.ichi2.anki.R
import com.ichi2.anki.cancelSync
import com.ichi2.anki.common.android.withAppLocale
import com.ichi2.anki.common.permissions.canPostNotifications
import com.ichi2.anki.followRedirect
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.notifications.NotificationId
import com.ichi2.anki.notifications.hayakiNotification
import com.ichi2.anki.reopen
import com.ichi2.anki.setLastSuccessfulSyncTime
import com.ichi2.anki.setLastSyncTimeToNow
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.sync.MediaSync
import com.ichi2.anki.sync.MeteredSyncPolicy
import com.ichi2.anki.sync.SyncAuth
import com.ichi2.anki.sync.fullUploadOrDownload
import com.ichi2.anki.sync.syncCollection
import com.ichi2.anki.syncCertificate
import com.ichi2.anki.syncServerDisplayName
import com.ichi2.anki.utils.ext.trySetForeground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.ankiweb.rsdroid.exceptions.BackendNetworkException
import net.ankiweb.rsdroid.exceptions.BackendSyncException.BackendSyncAuthFailedException
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import com.ichi2.anki.common.android.R as CommonR

/**
 * Held while a background collection sync runs. A profile switch holds it until
 * the process dies, so a sync cannot start part way through a restart.
 */
val syncLock = Mutex()

/**
 * Syncs collection and media in the background.
 *
 * The collection will be blocked while synchronizing, so any component that
 * depends on it should be blocked as well until the task ends.
 *
 * Media is synced by enqueueing a [SyncMediaWorker] work, which doesn't block the collection.
 *
 * Full uploads and downloads run when the server specifies the direction.
 * Conflicts requiring the user to choose a direction are ignored.
 *
 * That is useful when the user isn't interacting with
 * the app, like when doing an automatic sync after leaving the app.
 */
class SyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    private val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)
    private val notificationManager: NotificationManagerCompat? =
        if (canPostNotifications(context)) {
            NotificationManagerCompat.from(context)
        } else {
            null
        }

    private val notificationContext get() = applicationContext.withAppLocale()

    override suspend fun doWork(): Result {
        Timber.v("SyncWorker::doWork")
        return syncLock.withLockUnlessSwitching { doWorkHoldingLock() }
    }

    private suspend fun doWorkHoldingLock(): Result {
        trySetForeground(getForegroundInfo())

        val auth = inputData.toSyncAuth() ?: return Result.failure()
        if (!CollectionManager.updateCustomCertificate(syncCertificate())) return Result.failure()
        val shouldSyncMedia = inputData.getBoolean(SYNC_MEDIA_KEY, false)

        try {
            syncCollection(auth, shouldSyncMedia)
        } catch (cancellationException: CancellationException) {
            Timber.w(cancellationException, "SyncWorker cancelled (user tapped Cancel or WorkManager cancelled)")
            notificationManager?.cancel(NotificationId.SYNC)
            Timber.d("SyncWorker: progress notification cancelled after worker cancellation")
            cancelSync(CollectionManager.getBackend())
            throw cancellationException
        } catch (throwable: Throwable) {
            Timber.w(throwable, "SyncWorker failed")
            notify {
                setContentTitle(notificationContext.getString(R.string.notification_sync_failed))
                failureMessage(throwable)?.let {
                    setContentText(it)
                    setStyle(NotificationCompat.BigTextStyle().bigText(it))
                }
            }
            Timber.d("SyncWorker: showing failure notification")
            return Result.failure()
        } finally {
            if (inputData.isCurrentSyncSession()) setLastSyncTimeToNow()
        }
        Timber.d("SyncWorker: cancelling progress notification (sync completed)")
        notificationManager?.cancel(NotificationId.SYNC)

        Timber.d("SyncWorker: success")
        return Result.success()
    }

    private suspend fun syncCollection(
        auth: SyncAuth,
        syncMedia: Boolean,
    ) {
        Timber.v("SyncWorker::syncCollection")
        // Resolve the shared backend before the collection operation starts and holds its lease.
        val backend = CollectionManager.getBackend()
        val syncAuth =
            coroutineScope {
                val monitor =
                    launch(Dispatchers.IO) {
                        var syncProgress: Progress.NormalSync? = null
                        while (true) {
                            val progress = backend.latestProgress() // avoid sending repeated notifications
                            if (progress.hasNormalSync() && syncProgress != progress.normalSync) {
                                syncProgress = progress.normalSync
                                val text = syncProgress.run { "$added\n$removed" }
                                notify(getProgressNotification(text))
                            }
                            delay(SyncMediaWorker.NOTIFICATION_UPDATE_RATE)
                        }
                    }
                try {
                    withCol {
                        if (!inputData.isCurrentSyncSession()) return@withCol null
                        val response = syncCollection(auth, syncMedia = false)
                        if (!inputData.isCurrentSyncSession()) return@withCol null
                        val syncAuth = auth.followRedirect(response)
                        Timber.i("Sync required: %s", response.required)
                        // Keep the direction decision and transfer on the queue so the collection
                        // cannot change between checking whether it is empty and replacing it.
                        when (response.required) {
                            SyncCollectionResponse.ChangesRequired.NO_CHANGES -> _loadScheduler()
                            SyncCollectionResponse.ChangesRequired.FULL_DOWNLOAD -> fullSync(syncAuth, upload = false)
                            SyncCollectionResponse.ChangesRequired.FULL_UPLOAD -> fullSync(syncAuth, upload = true)
                            SyncCollectionResponse.ChangesRequired.FULL_SYNC -> {
                                Timber.d("Sync direction required: Skipping background sync")
                                return@withCol null
                            }
                            SyncCollectionResponse.ChangesRequired.UNRECOGNIZED,
                            SyncCollectionResponse.ChangesRequired.NORMAL_SYNC,
                            null,
                            -> TODO("should never happen")
                        }
                        syncAuth
                    }
                } finally {
                    Timber.d("Collection sync completed. Cancelling monitor...")
                    // this doesn't wait for completion; the outer `coroutineScope` does.
                    monitor.cancel()
                }
            } ?: return
        if (!inputData.isCurrentSyncSession()) return
        setLastSuccessfulSyncTime(auth)
        if (syncMedia) syncMedia(syncAuth)
    }

    private fun failureMessage(error: Throwable): String? =
        when (error) {
            is BackendNetworkException -> {
                val context = notificationContext
                val server = syncServerDisplayName()
                val message =
                    if (server == null) {
                        context.getString(R.string.notification_sync_network_unspecified)
                    } else {
                        context.getString(R.string.notification_sync_network, server)
                    }
                if (Prefs.isAutoSyncEnabled) {
                    context.getString(R.string.notification_sync_retry, message, context.getString(R.string.app_name))
                } else {
                    message
                }
            }
            is BackendSyncAuthFailedException -> notificationContext.getString(R.string.notification_sync_sign_in)
            else -> error.localizedMessage
        }

    private fun Collection.fullSync(
        auth: SyncAuth,
        upload: Boolean,
    ) {
        val direction = if (upload) R.string.notification_sync_upload else R.string.notification_sync_download
        val server = syncServerDisplayName() ?: notificationContext.getString(R.string.notification_sync_server)
        notify(getProgressNotification(notificationContext.getString(direction, server)))
        if (!upload) {
            val created =
                createBackup(
                    BackupManager.getBackupDirectoryFromCollection(colDb),
                    force = true,
                    waitForCompletion = true,
                )
            if (!created) {
                Timber.i("SyncWorker: Backup skipped before full download: collection unchanged")
            }
        }
        if (!inputData.isCurrentSyncSession()) return
        close(downgrade = false, forFullSync = true)
        try {
            // Media starts separately so WorkManager can enforce its network constraints.
            fullUploadOrDownload(auth, upload = upload, mediaSync = MediaSync.Skip)
        } finally {
            reopen(afterFullSync = true)
        }
    }

    private suspend fun syncMedia(auth: SyncAuth) {
        Timber.i("Enqueuing SyncMediaWorker")
        SyncMediaWorker.start(applicationContext, auth, session = inputData.getString(SYNC_SESSION_KEY).orEmpty())
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val cancelTitle = notificationContext.getString(CommonString.dialog_cancel)
        val notification =
            buildNotification {
                setContentTitle(notificationContext.getString(R.string.notification_sync_title))
                setOngoing(true)
                setProgress(0, 0, true)
                addAction(CommonR.drawable.symbol_close, cancelTitle, cancelIntent)
                foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_DEFERRED
            }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationId.SYNC, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationId.SYNC, notification)
        }
    }

    private fun notify(notification: Notification) {
        notificationManager?.notify(NotificationId.SYNC, notification)
    }

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
        val cancelTitle = notificationContext.getString(CommonString.dialog_cancel)

        return buildNotification {
            setContentTitle(notificationContext.getString(R.string.notification_sync_title))
            setContentText(progress)
            setProgress(0, 0, true)
            setOngoing(true)
            addAction(CommonR.drawable.symbol_close, cancelTitle, cancelIntent)
        }
    }

    companion object {
        private const val SYNC_MEDIA_KEY = "syncMedia"

        fun start(
            context: Context,
            syncAuth: SyncAuth,
            syncMedia: Boolean,
        ) {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(MeteredSyncPolicy.getNetworkTypeRequiredForSync())
                    .build()

            val data =
                Data
                    .Builder()
                    .putAll(syncAuth.toWorkData())
                    .putBoolean(SYNC_MEDIA_KEY, syncMedia)
                    .build()

            val request =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(data)
                    .setConstraints(constraints)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(UniqueWorkNames.SYNC, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(UniqueWorkNames.SYNC)
        }
    }
}
