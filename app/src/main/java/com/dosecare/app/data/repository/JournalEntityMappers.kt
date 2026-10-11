package com.dosecare.app.data.repository

import com.dosecare.app.data.local.NoteEntity
import com.dosecare.app.data.local.TrackedDateEntity
import com.dosecare.app.model.journal.AnchorIcon
import com.dosecare.app.model.journal.HeroBackground
import com.dosecare.app.model.journal.Note
import com.dosecare.app.model.journal.TrackedDate
import com.dosecare.app.model.medication.MedicationGroupColorKey
import java.time.LocalDate

fun TrackedDateEntity.toModel(): TrackedDate = TrackedDate(
    id = uuid,
    name = name,
    icon = AnchorIcon.fromStorageValue(iconKey),
    date = LocalDate.parse(dateIso),
    palette = MedicationGroupColorKey.fromStorageValueOrNull(paletteKey),
    heroBackground = HeroBackground.fromStorageValue(heroBackgroundKey),
    pinnedOrder = pinnedOrder,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun TrackedDate.toEntity(createdAtEpochMillis: Long, updatedAtEpochMillis: Long): TrackedDateEntity =
    TrackedDateEntity(
        uuid = id,
        name = name,
        iconKey = icon.storageKey,
        dateIso = date.toString(),
        paletteKey = palette?.name,
        heroBackgroundKey = heroBackground.storageKey,
        pinnedOrder = pinnedOrder,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun NoteEntity.toModel(): Note = Note(id = uuid, date = LocalDate.parse(dateIso), text = text)
