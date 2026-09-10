package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.MedicationDao
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.MedicationStatus
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val medicationDao: MedicationDao) {

    fun getMedicationsForOwner(ownerId: Long): Flow<List<Medication>> =
        medicationDao.getMedicationsForOwner(ownerId)

    suspend fun addMedication(medication: Medication): Long =
        medicationDao.insert(medication)

    suspend fun updateMedication(medication: Medication) =
        medicationDao.update(medication)

    suspend fun setStatus(medicationId: Long, status: MedicationStatus) =
        medicationDao.setStatus(medicationId, status)

    suspend fun deleteMedication(medication: Medication) =
        medicationDao.delete(medication)
}