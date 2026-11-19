// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Ankitects Pty Ltd <http://apps.ankiweb.net>

package com.ichi2.anki

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.concurrent.futures.await
import androidx.lifecycle.flowWithLifecycle
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import anki.collection.Progress
import anki.sync.SyncCollectionResponse
import anki.sync.syncAuth
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CollectionManager.withColExclusive
import com.ichi2.anki.account.updateSyncAccount
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.observability.ChangeManager.notifySubscribersAllValuesChanged
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.ShouldFetchMedia
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.sync.MediaSync
import com.ichi2.anki.sync.MeteredSyncPermission
import com.ichi2.anki.sync.MeteredSyncPolicy
import com.ichi2.anki.sync.SyncAuth
import com.ichi2.anki.sync.fullUploadOrDownload
import com.ichi2.anki.sync.syncCollection
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.worker.SyncMediaWorker
import com.ichi2.anki.worker.UniqueWorkNames
import com.ichi2.anki.worker.mediaSyncErrorMessage
import com.ichi2.anki.worker.mediaSyncProgressText
import com.ichi2.anki.worker.mediaSyncWasInterrupted
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.preferences.VersatileTextWithASwitchPreference
import com.ichi2.utils.LanguageUtil.withUnicodeIsolation
import com.ichi2.utils.dismissSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbCorruptException
import net.ankiweb.rsdroid.exceptions.BackendNetworkException
import net.ankiweb.rsdroid.exceptions.BackendSyncException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

object SyncPreferences {
    const val PREF_CURRENT_SYNC_URI = "currentSyncUri"
    const val PREF_CUSTOM_SYNC_URI = "syncBaseUrl"
    const val PREF_CUSTOM_SYNC_ENABLED = PREF_CUSTOM_SYNC_URI + VersatileTextWithASwitchPreference.SWITCH_SUFFIX
}

class NoSyncServerException : Exception("Invalid self-hosted sync server address")

enum class ConflictResolution {
    FULL_DOWNLOAD,
    FULL_UPLOAD,
}

/** An absent endpoint in the upstream protocol selects AnkiWeb. An invalid custom setup must never fall back to it. */
internal fun usesAnkiWeb(): Boolean = !Prefs.isCustomSyncEnabled && Prefs.customSyncUri.isNullOrBlank()

fun syncServerAddress(): String? {
    if (!Prefs.isCustomSyncEnabled) return null
    return Prefs.customSyncUri?.trim()?.takeIf(::isSelfHostedSyncAddress)
}

internal fun syncCertificate(): String = if (syncServerAddress() != null) Prefs.customSyncCertificate.orEmpty().ifBlank { "" } else ""

fun syncAuth(session: String = Prefs.syncSession): SyncAuth? {
    val key = Prefs.hkey?.takeIf { it.isNotEmpty() } ?: return null
    if (!usesAnkiWeb() && syncServerAddress() == null) return null
    val resolvedEndpoint = getEndpoint()
    val certificate = syncCertificate()
    if (session != Prefs.syncSession || !CollectionManager.updateCustomCertificate(certificate)) return null
    return SyncAuth(
        syncAuth {
            hkey = key
            resolvedEndpoint?.let { endpoint = it }
            ioTimeoutSecs = Prefs.networkTimeoutSecs
        },
    ).takeIf { session == Prefs.syncSession }
}

fun getEndpoint(): String? = Prefs.currentSyncUri?.takeIf(::acceptsSyncEndpoint) ?: syncServerAddress()

private fun acceptsSyncEndpoint(address: String): Boolean {
    if (!usesAnkiWeb()) return syncServerAddress() != null && isSelfHostedSyncAddress(address)
    val url = address.toHttpUrlOrNull() ?: return false
    val host = url.host.trimEnd('.')
    return url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
        (host == "ankiweb.net" || host.endsWith(".ankiweb.net"))
}

/** AnkiWeb has its own sign-in route. Custom server addresses cannot include credentials. */
internal fun isSelfHostedSyncAddress(address: String): Boolean {
    val url = address.toHttpUrlOrNull() ?: return false
    val host = url.host.trimEnd('.')
    return host != "ankiweb.net" && !host.endsWith(".ankiweb.net") &&
        url.username.isEmpty() && url.password.isEmpty()
}

/** Saves an Engine redirect only when it can be used by this Fork. */
internal fun rememberSyncEndpoint(endpoint: String): String? =
    endpoint.takeIf(::acceptsSyncEndpoint)?.also {
        Timber.i("Sync endpoint updated")
        Prefs.currentSyncUri = it
    }

internal fun SyncAuth.followRedirect(response: SyncCollectionResponse): SyncAuth =
    if (response.hasNewEndpoint()) rememberSyncEndpoint(response.newEndpoint)?.let(::withEndpoint) ?: this else this

internal fun syncServerDisplayName(): String? =
    syncServerAddress()?.toHttpUrlOrNull()?.let { url ->
        withUnicodeIsolation(url.host)
    } ?: "AnkiWeb".takeIf { usesAnkiWeb() }

/**
 * Whether the user is signed in to their sync server.
 * Returning true does not guarantee that the user actually synced recently,
 * or even that the account is still valid.
 */
fun isLoggedIn(): Boolean = !Prefs.hkey.isNullOrEmpty() && (usesAnkiWeb() || syncServerAddress() != null)

/** Elapsed milliseconds since the last manual or automatic sync attempt. */
fun millisecondsSinceLastSyncAttempt() = TimeManager.time.intTimeMS() - Prefs.lastSyncTime

fun DeckPicker.handleNewSync(
    conflict: ConflictResolution?,
    syncMedia: Boolean,
    meteredSyncPermission: MeteredSyncPermission = MeteredSyncPermission.USE_PREFERENCES,
) {
    val session = Prefs.syncSession
    if (conflict != null && sessionOnSyncConflict != null && sessionOnSyncConflict != session) return
    val auth = syncAuth(session) ?: return
    val mediaNetworkType = MeteredSyncPolicy.getNetworkTypeRequiredForSync(forMedia = true, permission = meteredSyncPermission)
    val deckPicker = this
    launchCatchingTask {
        try {
            try {
                when (conflict) {
                    ConflictResolution.FULL_DOWNLOAD ->
                        handleDownload(deckPicker, auth, deckPicker.mediaUsnOnConflict, deckPicker.mediaNetworkTypeOnConflict, session)
                    ConflictResolution.FULL_UPLOAD ->
                        handleUpload(deckPicker, auth, deckPicker.mediaUsnOnConflict, deckPicker.mediaNetworkTypeOnConflict, session)
                    null -> {
                        handleNormalSync(deckPicker, auth, syncMedia, mediaNetworkType, session)
                    }
                }
            } catch (exc: BackendSyncException.BackendSyncAuthFailedException) {
                if (Prefs.syncSession != session) return@launchCatchingTask
                updateSyncAccount(deckPicker)
                showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_USER_NOT_LOGGED_IN_SYNC)
                return@launchCatchingTask
            } catch (exc: BackendNetworkException) {
                showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_CONNECTION_ERROR, exc.localizedMessage)
                return@launchCatchingTask
            } catch (exc: BackendDbCorruptException) {
                showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_SYNC_CORRUPT_COLLECTION, exc.localizedMessage)
                return@launchCatchingTask
            } catch (exc: BackendSyncException) {
                if (exc.isClockOffException()) {
                    showSyncErrorDialog(
                        SyncErrorDialog.Type.DIALOG_SYNC_CLOCK_OFF,
                        exc.localizedMessage,
                    )
                    return@launchCatchingTask
                }

                if (exc.isCollectionTooLargeException()) {
                    showSyncErrorDialog(
                        SyncErrorDialog.Type.DIALOG_COLLECTION_TOO_LARGE,
                        exc.localizedMessage,
                    )
                    return@launchCatchingTask
                }

                if (exc.localizedMessage?.contains(TR.syncSanityCheckFailed()) == true) {
                    sessionOnSyncConflict = session
                    mediaUsnOnConflict = null
                    mediaNetworkTypeOnConflict = mediaNetworkType
                    showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_SYNC_SANITY_ERROR, exc.localizedMessage)
                    return@launchCatchingTask
                }

                if (exc.localizedMessage?.contains(TR.errorsPleaseCheckDatabase()) == true) {
                    showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_SYNC_BASIC_CHECK_ERROR, exc.localizedMessage)
                    return@launchCatchingTask
                }

                throw exc
            }
            withCol { notetypes.clearCache() }
            notifySubscribersAllValuesChanged(deckPicker)
            refreshState()
        } finally {
            if (session == Prefs.syncSession) setLastSyncTimeToNow()
        }
    }
}

/** The backend currently exposes ClockIncorrect as a localized sync error message. */
private fun BackendSyncException.isClockOffException(): Boolean =
    try {
        message == TR.syncClockOff()
    } catch (_: Throwable) {
        false
    }

private val collectionTooLargeDetailsRegex =
    Regex("""\b\d+(?:\.\d{2})?(?: MB)? > \d+(?:\.\d{2})?(?: MB)?\b""")

private fun BackendSyncException.isCollectionTooLargeException(): Boolean =
    message?.let { collectionTooLargeDetailsRegex.containsMatchIn(it) } == true

fun cancelSync(backend: Backend) {
    backend.setWantsAbort()
    backend.abortSync()
}

private suspend fun handleNormalSync(
    deckPicker: DeckPicker,
    auth: SyncAuth,
    syncMedia: Boolean,
    mediaNetworkType: NetworkType,
    session: String,
) {
    Timber.i("Sync: Normal collection sync")
    val output =
        deckPicker.withProgress(
            extractProgress = {
                if (progress.hasNormalSync()) {
                    text = progress.normalSync.run { "$added\n$removed" }
                }
            },
            onCancel = ::cancelSync,
            manualCancelButton = CommonString.dialog_cancel,
        ) {
            withColExclusive(
                operation = CollectionOperation.SYNC,
                onCancel = ::cancelSync,
            ) {
                if (session != Prefs.syncSession) return@withColExclusive null
                syncCollection(auth, syncMedia = false) // media is synced by SyncMediaWorker
            }
        }

    if (output == null || Prefs.syncSession != session) return
    val auth2 = auth.followRedirect(output)
    val mediaUsn =
        if (syncMedia) {
            output.serverMediaUsn
        } else {
            null
        }

    Timber.i("sync result: ${output.required}")
    when (output.required) {
        // a successful sync returns this value
        SyncCollectionResponse.ChangesRequired.NO_CHANGES -> {
            // scheduler version may have changed
            withCol { _loadScheduler() }
            setLastSuccessfulSyncTime(auth)
            val message = if (syncMedia) CommonString.col_synced_media_in_background else CommonString.sync_database_acknowledge
            deckPicker.showSyncLogMessage(message, output.serverMessage)
            deckPicker.refreshState()
            if (syncMedia) {
                SyncMediaWorker.start(deckPicker, auth2, mediaNetworkType, session)
            }
        }

        SyncCollectionResponse.ChangesRequired.FULL_DOWNLOAD -> {
            handleDownload(deckPicker, auth2, mediaUsn, mediaNetworkType, session)
        }

        SyncCollectionResponse.ChangesRequired.FULL_UPLOAD -> {
            handleUpload(deckPicker, auth2, mediaUsn, mediaNetworkType, session)
        }

        SyncCollectionResponse.ChangesRequired.FULL_SYNC -> {
            deckPicker.sessionOnSyncConflict = session
            deckPicker.mediaUsnOnConflict = mediaUsn
            deckPicker.mediaNetworkTypeOnConflict = mediaNetworkType
            deckPicker.showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_SYNC_CONFLICT_RESOLUTION)
        }

        SyncCollectionResponse.ChangesRequired.NORMAL_SYNC,
        SyncCollectionResponse.ChangesRequired.UNRECOGNIZED,
        null,
        -> {
            TODO("should never happen")
        }
    }
}

private fun fullDownloadProgress(title: String): ProgressContext.() -> Unit =
    {
        fun Progress.FullSync.toAmount() = ProgressContext.Amount(transferred.toLong(), total.toLong())

        text = title
        if (progress.hasFullSync() && progress.fullSync.total > 0) {
            amount = progress.fullSync.toAmount()
        }
    }

private suspend fun handleDownload(
    deckPicker: DeckPicker,
    auth: SyncAuth,
    mediaUsn: Int?,
    mediaNetworkType: NetworkType,
    session: String,
) {
    Timber.i("Sync: Full collection download requested")
    deckPicker.withProgress(
        progressContext = ProgressContext.ofBytes(context = deckPicker).copy(separator = "\n"),
        extractProgress =
            fullDownloadProgress(
                deckPicker.getString(
                    CommonString.paper_sync_downloading,
                    syncServerDisplayName() ?: deckPicker.getString(CommonString.sync_server_title),
                ),
            ),
        onCancel = ::cancelSync,
        manualCancelButton = CommonString.dialog_cancel,
    ) {
        withColExclusive(
            operation = CollectionOperation.FULL_DOWNLOAD,
            onCancel = ::cancelSync,
        ) {
            if (session != Prefs.syncSession) return@withColExclusive
            val created =
                createBackup(
                    BackupManager.getBackupDirectoryFromCollection(colDb),
                    force = true,
                    waitForCompletion = true,
                )
            if (!created) {
                Timber.i("Sync: Backup skipped before full download: collection unchanged")
            }
            if (session != Prefs.syncSession) return@withColExclusive
            close(downgrade = false, forFullSync = true)
            try {
                // Media starts through SyncMediaWorker so WorkManager can enforce network constraints.
                fullUploadOrDownload(
                    auth,
                    upload = false,
                    mediaSync = MediaSync.Skip,
                )
            } finally {
                reopen(afterFullSync = true)
            }
        }
        deckPicker.refreshState()
        if (mediaUsn != null && session == Prefs.syncSession) {
            SyncMediaWorker.start(deckPicker, auth, mediaNetworkType, session)
        }
    }

    if (session != Prefs.syncSession) return
    setLastSuccessfulSyncTime(auth)
    Timber.i("Full Download Completed")
    deckPicker.showSyncLogMessage(CommonString.backup_one_way_sync_from_server, "")
}

private suspend fun handleUpload(
    deckPicker: DeckPicker,
    auth: SyncAuth,
    mediaUsn: Int?,
    mediaNetworkType: NetworkType,
    session: String,
) {
    Timber.i("Sync: Full collection upload requested")
    deckPicker.withProgress(
        progressContext = ProgressContext.ofBytes(context = deckPicker).copy(separator = "\n"),
        extractProgress =
            fullDownloadProgress(
                deckPicker.getString(
                    CommonString.paper_sync_uploading,
                    syncServerDisplayName() ?: deckPicker.getString(CommonString.sync_server_title),
                ),
            ),
        onCancel = ::cancelSync,
        manualCancelButton = CommonString.dialog_cancel,
    ) {
        withColExclusive(
            operation = CollectionOperation.FULL_UPLOAD,
            onCancel = ::cancelSync,
        ) {
            if (session != Prefs.syncSession) return@withColExclusive
            close(downgrade = false, forFullSync = true)
            try {
                // Media starts through SyncMediaWorker so WorkManager can enforce network constraints.
                fullUploadOrDownload(
                    auth,
                    upload = true,
                    mediaSync = MediaSync.Skip,
                )
            } finally {
                reopen(afterFullSync = true)
            }
        }
        deckPicker.refreshState()
        if (mediaUsn != null && session == Prefs.syncSession) {
            SyncMediaWorker.start(deckPicker, auth, mediaNetworkType, session)
        }
    }
    if (session != Prefs.syncSession) return
    setLastSuccessfulSyncTime(auth)
    Timber.i("Full Upload Completed")
    deckPicker.showSyncLogMessage(CommonString.sync_log_uploading_message, "")
}

fun cancelMediaSync(backend: Backend) {
    backend.setWantsAbort()
    backend.abortMediaSync()
}

/**
 * Whether media should be fetched on sync. Options from preferences are:
 * * Always
 * * Only if unmetered
 * * Never
 */
fun shouldFetchMedia(): Boolean = Prefs.shouldFetchMedia != ShouldFetchMedia.NEVER

suspend fun monitorMediaSync(deckPicker: DeckPicker): Unit =
    withContext(Dispatchers.Main.immediate) {
        val manager = WorkManager.getInstance(deckPicker)
        val session = Prefs.syncSession
        val work =
            manager.getWorkInfosForUniqueWork(UniqueWorkNames.SYNC_MEDIA).await().firstOrNull { !it.state.isFinished }
                ?: return@withContext
        if (session != Prefs.syncSession || deckPicker.isFinishing) return@withContext
        val scope = this

        fun progressText(info: WorkInfo): String =
            if (info.state == WorkInfo.State.RUNNING) {
                info.progress.mediaSyncProgressText ?: deckPicker.getString(CommonString.sync_media_transferring)
            } else {
                deckPicker.getString(CommonString.sync_media_queued)
            }

        val progress = mutableStateOf(progressText(work))
        val dialog =
            createPaperDialog(deckPicker) { window ->
                PaperDialog(
                    title = with(deckPicker) { TR.sentenceCase.mediaSyncLog },
                    onDismissRequest = window::dismiss,
                    actions =
                        listOf(
                            DialogAction(TR.syncAbortButton(), {
                                if (session == Prefs.syncSession) {
                                    Prefs.mediaSyncPaused = true
                                    manager.cancelWorkById(work.id)
                                } else {
                                    window.dismiss()
                                }
                            }),
                            DialogAction(deckPicker.getString(CommonString.dialog_continue), window::dismiss, DialogActionStyle.Filled),
                        ),
                ) { DialogBody(progress.value) }
            }

        val finished =
            try {
                dialog.setOnDismissListener { scope.cancel() }
                dialog.show()
                manager
                    .getWorkInfoByIdFlow(work.id)
                    .flowWithLifecycle(deckPicker.lifecycle)
                    .first { info ->
                        if (session != Prefs.syncSession || info == null || info.state.isFinished) {
                            true
                        } else {
                            progress.value = progressText(info)
                            false
                        }
                    }
            } finally {
                dialog.setOnDismissListener(null)
                dialog.dismissSafely()
            }

        if (finished == null || session != Prefs.syncSession || deckPicker.isFinishing) return@withContext
        when {
            finished.state == WorkInfo.State.CANCELLED || finished.outputData.mediaSyncWasInterrupted ->
                deckPicker.showSnackbar(TR.syncMediaAborted(), Snackbar.LENGTH_SHORT)
            finished.state == WorkInfo.State.SUCCEEDED -> deckPicker.showSnackbar(TR.syncMediaComplete(), Snackbar.LENGTH_SHORT)
            finished.state == WorkInfo.State.FAILED ->
                deckPicker.showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_MEDIA_SYNC_ERROR, finished.outputData.mediaSyncErrorMessage)
            else -> Unit
        }
    }

/**
 * Show a simple snackbar message or notification if the activity is not in foreground
 * @param messageResource String resource for message
 */
fun DeckPicker.showSyncLogMessage(
    @StringRes messageResource: Int,
    syncMessage: String?,
) {
    if (activityPaused) {
        val res = AnkiDroidApp.appResources
        showSimpleNotification(
            res.getString(R.string.app_name),
            res.getString(messageResource),
            NotificationChannel.SYNC,
        )
    } else {
        if (syncMessage.isNullOrEmpty()) {
            showSnackbar(messageResource)
        } else {
            val res = AnkiDroidApp.appResources
            showSimpleMessageDialog(title = res.getString(messageResource), message = syncMessage)
        }
    }
}

/** Starts the retry delay after every sync attempt, regardless of outcome. */
fun setLastSyncTimeToNow() {
    Prefs.lastSyncTime = TimeManager.time.intTimeMS()
}

/** The account page reports completed collection syncs independently of auto-sync's attempt window. */
internal fun setLastSuccessfulSyncTime(auth: SyncAuth) {
    // A worker can finish as its account is being signed out.
    if (Prefs.hkey == auth.hkey) {
        Prefs.lastSuccessfulSyncTime = TimeManager.time.intTimeMS()
    }
}
