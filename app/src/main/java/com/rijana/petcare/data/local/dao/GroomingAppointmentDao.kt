package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.GroomingAppointment
import com.rijana.petcare.data.local.entity.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GroomingAppointmentDao {

    @Query("SELECT * FROM grooming_appointments WHERE petId = :petId ORDER BY date ASC")
    fun getAppointmentsForPet(petId: Long): Flow<List<GroomingAppointment>>

    @Query(
        """
        SELECT grooming_appointments.* FROM grooming_appointments
        INNER JOIN pets ON grooming_appointments.petId = pets.id
        WHERE pets.ownerId = :ownerId
        ORDER BY grooming_appointments.date ASC
        """
    )
    fun getAppointmentsForOwner(ownerId: Long): Flow<List<GroomingAppointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: GroomingAppointment): Long

    @Update
    suspend fun update(appointment: GroomingAppointment)

    @Query("UPDATE grooming_appointments SET status = :status WHERE id = :appointmentId")
    suspend fun setStatus(appointmentId: Long, status: AppointmentStatus)

    @Delete
    suspend fun delete(appointment: GroomingAppointment)
}