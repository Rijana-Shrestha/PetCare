package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_places",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerId"])]
)
data class SavedPlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: Long,
    val name: String,
    val type: PlaceType,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)