package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.GroomingAppointmentDao
import com.rijana.petcare.data.local.entity.AppointmentStatus
import com.rijana.petcare.data.local.entity.GroomingAppointment
import kotlinx.coroutines.flow.Flow

class GroomingAppointmentRepository(private val groomingAppointmentDao: GroomingAppointmentDao) {

    fun getAppointmentsForOwner(ownerId: Long): Flow<List<GroomingAppointment>> =
        groomingAppointmentDao.getAppointmentsForOwner(ownerId)

    suspend fun addAppointment(appointment: GroomingAppointment): Long =
        groomingAppointmentDao.insert(appointment)

    suspend fun setStatus(appointmentId: Long, status: AppointmentStatus) =
        groomingAppointmentDao.setStatus(appointmentId, status)

    suspend fun deleteAppointment(appointment: GroomingAppointment) =
        groomingAppointmentDao.delete(appointment)
}