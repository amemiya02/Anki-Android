// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import android.os.Parcelable
import com.ichi2.anki.libanki.CardOrdinal
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Field
import com.ichi2.anki.libanki.NoteTypeId
import com.ichi2.anki.libanki.NotetypeJson
import com.ichi2.anki.servicelayer.LanguageHintService.languageHint
import kotlinx.parcelize.Parcelize
import java.util.Locale

/** A restored dialog may outlive a rename or reorder. Old fields can have no Engine ID. */
@Parcelize
internal data class NoteFieldRef(
    val id: Long?,
    val name: String,
) : Parcelable {
    constructor(field: Field) : this(field.id, field.name)
}

internal data class NoteFieldRow(
    val field: NoteFieldRef,
    val sortsBrowser: Boolean,
    val keyboard: Locale?,
)

internal data class NoteTemplateRow(
    val ord: CardOrdinal,
    val name: String,
    val frontFields: List<String> = emptyList(),
    val backFields: List<String> = emptyList(),
)

internal data class NoteTypeDetails(
    val id: NoteTypeId,
    val fields: List<NoteFieldRow>,
    val templates: List<NoteTemplateRow>,
)

internal fun Collection.noteTypeDetails(id: NoteTypeId): NoteTypeDetails? {
    val type = notetypes.get(id) ?: return null
    val fields = type.fields.toList()
    val names = fields.map { it.name }.toSet()
    return NoteTypeDetails(
        id = id,
        fields = fields.mapIndexed { index, field -> NoteFieldRow(NoteFieldRef(field), index == type.sortf, field.languageHint) },
        templates =
            type.templates.map { template ->
                NoteTemplateRow(template.ord, template.name, templateFields(template.qfmt, names), templateFields(template.afmt, names))
            },
    )
}

private val fieldPlaceholder = Regex("""\{\{([^{}]+)}}""")

private fun templateFields(
    template: String,
    fieldNames: Set<String>,
): List<String> =
    fieldPlaceholder
        .findAll(template)
        .map { it.groupValues[1].trim() }
        .filterNot { it.startsWith('#') || it.startsWith('^') || it.startsWith('/') }
        .map { it.substringAfterLast(':').trim() }
        .filter { it in fieldNames }
        .distinct()
        .toList()

internal enum class FieldAction { Add, Rename, Move, Sort, Keyboard, Delete }

@Parcelize
internal data class FieldChange(
    val action: FieldAction,
    val field: NoteFieldRef? = null,
    val value: String = "",
    val position: Int = 0,
) : Parcelable {
    val needsFullSync: Boolean
        get() = action in setOf(FieldAction.Add, FieldAction.Move, FieldAction.Sort, FieldAction.Delete)
}

internal enum class FieldChangeResult { Changed, Unchanged, DuplicateName, LastField, NeedsFullSync }

internal fun Collection.changeNoteField(
    noteTypeId: NoteTypeId,
    change: FieldChange,
    confirmed: Boolean = false,
): FieldChangeResult {
    // Mutate a copy, so an Engine validation failure cannot leave changes in the local cache.
    val type = notetypes.get(noteTypeId)?.deepClone() ?: return FieldChangeResult.Unchanged
    val index =
        change.field?.let { ref ->
            val fields = type.fields.toList()
            val exact = fields.indexOfFirst { it.id == ref.id && it.name == ref.name }
            if (exact >= 0 || ref.id == null) exact else fields.indices.singleOrNull { fields[it].id == ref.id } ?: -1
        } ?: -1
    if (change.action != FieldAction.Add && index < 0) return FieldChangeResult.Unchanged
    val newName = cleanFieldName(change.value)
    when (change.action) {
        FieldAction.Add, FieldAction.Rename -> {
            if (newName.isBlank() || (change.action == FieldAction.Rename && newName == type.getField(index).name)) {
                return FieldChangeResult.Unchanged
            }
            if (newName in type.fieldsNames) return FieldChangeResult.DuplicateName
        }
        FieldAction.Move -> if (change.position == index || change.position !in type.fieldsNames.indices) return FieldChangeResult.Unchanged
        FieldAction.Sort -> if (index == type.sortf) return FieldChangeResult.Unchanged
        FieldAction.Delete -> if (type.fieldsNames.size == 1) return FieldChangeResult.LastField
        FieldAction.Keyboard -> Unit
    }
    if (change.needsFullSync) {
        if (!confirmed && !schemaChanged()) return FieldChangeResult.NeedsFullSync
        modSchema(check = false)
    }
    applyFieldChange(type, index, newName, change)
    return FieldChangeResult.Changed
}

private fun Collection.applyFieldChange(
    type: NotetypeJson,
    index: Int,
    name: String,
    change: FieldChange,
) {
    when (change.action) {
        FieldAction.Add -> notetypes.addFieldModChanged(type, notetypes.newField(name))
        FieldAction.Rename -> notetypes.renameFieldLegacy(type, type.getField(index), name)
        FieldAction.Move -> notetypes.moveFieldLegacy(type, type.getField(index), change.position)
        FieldAction.Sort -> {
            notetypes.setSortIndex(type, index)
            notetypes.save(type)
        }
        FieldAction.Keyboard -> {
            type.getField(index).languageHint = change.value.takeIf(String::isNotEmpty)?.let(Locale::forLanguageTag)
            notetypes.save(type)
        }
        FieldAction.Delete -> notetypes.remFieldLegacy(type, type.getField(index))
    }
}

internal fun cleanFieldName(input: String): String = input.filterNot { it in "\n\r{}:\"" }.trimStart('#', '^', '/', ' ', '\t').trim()
