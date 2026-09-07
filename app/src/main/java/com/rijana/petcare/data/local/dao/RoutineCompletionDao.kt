package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.RoutineCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineCompletionDao {

    @Query(
        "SELECT * FROM routine_completions WHERE routineId = :routineId AND occurrenceDate = :occurrenceDate LIMIT 1"
    )
    fun getCompletion(routineId: Long, occurrenceDate: Long): Flow<RoutineCompletion?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDone(completion: RoutineCompletion): Long

    @Query(
        "DELETE FROM routine_completions WHERE routineId = :routineId AND occurrenceDate = :occurrenceDate"
    )
    suspend fun markUndone(routineId: Long, occurrenceDate: Long)
}