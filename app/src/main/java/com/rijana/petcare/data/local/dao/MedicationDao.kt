package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.MedicationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications WHERE petId = :petId ORDER BY time ASC")
    fun getMedicationsForPet(petId: Long): Flow<List<Medication>>

    @Query(
        """
        SELECT medications.* FROM medications
        INNER JOIN pets ON medications.petId = pets.id
        WHERE pets.ownerId = :ownerId
        ORDER BY medications.startDate ASC
        """
    )
    fun getMedicationsForOwner(ownerId: Long): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Query("UPDATE medications SET status = :status WHERE id = :medicationId")
    suspend fun setStatus(medicationId: Long, status: MedicationStatus)

    @Delete
    suspend fun delete(medication: Medication)
}