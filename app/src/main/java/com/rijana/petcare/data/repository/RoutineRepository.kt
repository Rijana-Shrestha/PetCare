package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.RoutineCompletionDao
import com.rijana.petcare.data.local.dao.RoutineDao
import com.rijana.petcare.data.local.entity.Routine
import com.rijana.petcare.data.local.entity.RoutineCompletion
import kotlinx.coroutines.flow.Flow

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val routineCompletionDao: RoutineCompletionDao
) {

    fun getRoutinesForPet(petId: Long): Flow<List<Routine>> =
        routineDao.getRoutinesForPet(petId)

    fun getRoutinesForOwner(ownerId: Long): Flow<List<Routine>> =
        routineDao.getRoutinesForOwner(ownerId)

    suspend fun addRoutine(routine: Routine): Long =
        routineDao.insert(routine)

    suspend fun updateRoutine(routine: Routine) =
        routineDao.update(routine)

    suspend fun deleteRoutine(routine: Routine) =
        routineDao.delete(routine)

    fun getCompletion(routineId: Long, occurrenceDate: Long): Flow<RoutineCompletion?> =
        routineCompletionDao.getCompletion(routineId, occurrenceDate)

    suspend fun markDone(routineId: Long, occurrenceDate: Long) {
        routineCompletionDao.markDone(
            RoutineCompletion(
                routineId = routineId,
                occurrenceDate = occurrenceDate,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markUndone(routineId: Long, occurrenceDate: Long) {
        routineCompletionDao.markUndone(routineId, occurrenceDate)
    }
}