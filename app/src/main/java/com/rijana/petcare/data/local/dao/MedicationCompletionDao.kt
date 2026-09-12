package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.MedicationCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationCompletionDao {

    @Query(
        "SELECT * FROM medication_completions WHERE medicationId = :medicationId AND occurrenceDate = :occurrenceDate LIMIT 1"
    )
    fun getCompletion(medicationId: Long, occurrenceDate: Long): Flow<MedicationCompletion?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDone(completion: MedicationCompletion): Long

    @Query(
        "DELETE FROM medication_completions WHERE medicationId = :medicationId AND occurrenceDate = :occurrenceDate"
    )
    suspend fun markUndone(medicationId: Long, occurrenceDate: Long)
}