package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.Routine
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines WHERE petId = :petId ORDER BY time ASC")
    fun getRoutinesForPet(petId: Long): Flow<List<Routine>>

    @Query(
        """
        SELECT routines.* FROM routines
        INNER JOIN pets ON routines.petId = pets.id
        WHERE pets.ownerId = :ownerId
        ORDER BY routines.time ASC
        """
    )
    fun getRoutinesForOwner(ownerId: Long): Flow<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)
}