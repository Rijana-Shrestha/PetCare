package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pets",
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
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: Long,
    val name: String,
    val type: PetType,
    val breed: String,
    val dateOfBirth: Long,
    val gender: Gender,
    val weightKg: Float,
    val dietaryPreference: String? = null,
    val allergies: String? = null,
    val favoriteToy: String? = null,
    val note: String? = null,
    val photoUri: String? = null
)