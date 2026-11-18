// SPDX-FileCopyrightText: 2024 David Allison <davidallisongithub@gmail.com>
// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.crashreporting.runCatchingWithLog
import com.ichi2.anki.startup.getDefaultAnkiDroidDirectory
import com.ichi2.anki.utils.ext.containsFile
import timber.log.Timber
import java.io.File

/**
 * Lets the user pick the AnkiDroid directory from a list of detected storage
 * locations, so an SD card can be chosen without knowing its path.
 * A custom path can still be typed in via the dialog.
 */
class ExternalDirectorySelectionPreference(
    context: Context,
    attrs: AttributeSet?,
) : ListPreference(context, attrs) {
    init {
        setSummaryProvider {
            value.takeUnless { it.isNullOrEmpty() } ?: context.getString(CommonString.pref_directory_not_set)
        }
    }

    /** The default AnkiDroid directory, or null if storage is unavailable. */
    private val defaultAnkiDir: File?
        get() =
            try {
                getDefaultAnkiDroidDirectory(context)
            } catch (e: Exception) {
                Timber.w(e, "Could not access default AnkiDroid directory")
                null
            }

    /**
     * Builds the candidate paths to show: the current value, the default
     * directory, and anything found by scanning storage.
     */
    fun loadDirectories(): List<String> =
        buildList {
            value?.takeIf(String::isNotEmpty)?.let { add(File(it)) }
            defaultAnkiDir?.let { add(it) }
            addAll(getScannedDirectories())
        }.map { it.absolutePath }
            .distinct()

    private fun isValidAnkiDir(dir: File): Boolean {
        try {
            if (!dir.isDirectory || dir.name.startsWith(".")) return false
            if (IGNORED_DIRECTORIES.contains(dir.name.lowercase())) return false
            if (dir.name.startsWith("AnkiDroid", ignoreCase = true)) return true
            return dir.containsFile("collection.anki2")
        } catch (e: Exception) {
            Timber.w(e, "Could not access directory")
            return false
        }
    }

    /**
     * Scans the external storage roots for AnkiDroid directories.
     * A root with none, or which cannot be listed, is offered as a new AnkiDroid location instead.
     */
    private fun getScannedDirectories(): List<File> {
        val storageRoots =
            runCatchingWithLog("get storage roots") {
                context.getExternalFilesDirs(null)
            }.getOrNull()
                .orEmpty()
                .filterNotNull()

        fun File.getValidAnkiSubfolders(): List<File>? {
            val subFolders = listFiles() ?: return null
            return subFolders.filter { isValidAnkiDir(it) }.ifEmpty { null }
        }

        return storageRoots.flatMap { root ->
            when (val ankiFolders = root.getValidAnkiSubfolders()) {
                // If no anki directories are found, we can list this as it is likely an SD card
                null -> listOf(File(root, "AnkiDroid"))
                else -> ankiFolders
            }
        }
    }

    companion object {
        private val IGNORED_DIRECTORIES = setOf("collection.media", "backups", "cache", "code_cache")
    }
}
