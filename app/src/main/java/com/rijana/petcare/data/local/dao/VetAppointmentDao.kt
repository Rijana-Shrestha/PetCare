package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.VetAppointment
import com.rijana.petcare.data.local.entity.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VetAppointmentDao {

    @Query("SELECT * FROM vet_appointments WHERE petId = :petId ORDER BY date ASC")
    fun getAppointmentsForPet(petId: Long): Flow<List<VetAppointment>>

    @Query(
        """
        SELECT vet_appointments.* FROM vet_appointments
        INNER JOIN pets ON vet_appointments.petId = pets.id
        WHERE pets.ownerId = :ownerId
        ORDER BY vet_appointments.date ASC
        """
    )
    fun getAppointmentsForOwner(ownerId: Long): Flow<List<VetAppointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: VetAppointment): Long

    @Update
    suspend fun update(appointment: VetAppointment)

    @Query("UPDATE vet_appointments SET status = :status WHERE id = :appointmentId")
    suspend fun setStatus(appointmentId: Long, status: AppointmentStatus)

    @Delete
    suspend fun delete(appointment: VetAppointment)
}