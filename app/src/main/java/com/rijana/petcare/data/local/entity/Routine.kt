package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routines",
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
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val taskName: String,
    val taskType: String,
    val time: String,
    val durationMinutes: Int? = null,
    val repeat: TaskRepeat,
    val customDays: String? = null,   // only used when repeat = CUSTOM, e.g. "MON,WED,FRI"
    val startDate: Long,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 15
)