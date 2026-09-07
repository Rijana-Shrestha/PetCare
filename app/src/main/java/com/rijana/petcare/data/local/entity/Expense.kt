package com.rijana.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
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
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val category: ExpenseCategory,
    val amount: Float,
    val date: Long,
    val note: String? = null
)