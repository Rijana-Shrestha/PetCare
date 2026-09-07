package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grooming_appointments",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SavedPlace::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["petId"]), Index(value = ["placeId"])]
)
data class GroomingAppointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val placeId: Long? = null,
    val type: String,
    val salonName: String,
    val date: Long,
    val time: String,
    val status: AppointmentStatus = AppointmentStatus.UPCOMING,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 1440
)