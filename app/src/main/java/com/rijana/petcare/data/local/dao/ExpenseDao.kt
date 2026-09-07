package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query(
        """
        SELECT expenses.* FROM expenses
        INNER JOIN pets ON expenses.petId = pets.id
        WHERE pets.ownerId = :ownerId AND (:petId IS NULL OR expenses.petId = :petId)
        ORDER BY expenses.date DESC
        """
    )
    fun getExpenses(ownerId: Long, petId: Long?): Flow<List<Expense>>

    @Query(
        """
        SELECT COALESCE(SUM(expenses.amount), 0) FROM expenses
        INNER JOIN pets ON expenses.petId = pets.id
        WHERE pets.ownerId = :ownerId
        AND expenses.date BETWEEN :monthStart AND :monthEnd
        AND (:petId IS NULL OR expenses.petId = :petId)
        """
    )
    fun getTotalForMonth(ownerId: Long, monthStart: Long, monthEnd: Long, petId: Long?): Flow<Float>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}