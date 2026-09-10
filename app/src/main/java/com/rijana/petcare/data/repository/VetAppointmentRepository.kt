package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.VetAppointmentDao
import com.rijana.petcare.data.local.entity.AppointmentStatus
import com.rijana.petcare.data.local.entity.VetAppointment
import kotlinx.coroutines.flow.Flow

class VetAppointmentRepository(private val vetAppointmentDao: VetAppointmentDao) {

    fun getAppointmentsForOwner(ownerId: Long): Flow<List<VetAppointment>> =
        vetAppointmentDao.getAppointmentsForOwner(ownerId)

    suspend fun addAppointment(appointment: VetAppointment): Long =
        vetAppointmentDao.insert(appointment)

    suspend fun setStatus(appointmentId: Long, status: AppointmentStatus) =
        vetAppointmentDao.setStatus(appointmentId, status)

    suspend fun deleteAppointment(appointment: VetAppointment) =
        vetAppointmentDao.delete(appointment)
}