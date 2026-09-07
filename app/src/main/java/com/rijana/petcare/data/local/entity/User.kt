package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["firebaseUid"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firebaseUid: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    val profileImageUri: String? = null
)