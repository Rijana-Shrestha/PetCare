package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["petId"])]
)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val name: String,
    val type: MedicationType,
    val dosage: String,
    val instructions: String? = null,
    val time: String,
    val frequency: String,
    val startDate: Long,
    val endDate: Long? = null,
    val status: MedicationStatus = MedicationStatus.ACTIVE,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 15
)