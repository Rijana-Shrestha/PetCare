package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.MedicationCompletionDao
import com.rijana.petcare.data.local.dao.MedicationDao
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.MedicationCompletion
import com.rijana.petcare.data.local.entity.MedicationStatus
import kotlinx.coroutines.flow.Flow

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val medicationCompletionDao: MedicationCompletionDao
) {

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

    fun getCompletion(medicationId: Long, occurrenceDate: Long): Flow<MedicationCompletion?> =
        medicationCompletionDao.getCompletion(medicationId, occurrenceDate)

    suspend fun markDone(medicationId: Long, occurrenceDate: Long) {
        medicationCompletionDao.markDone(
            MedicationCompletion(
                medicationId = medicationId,
                occurrenceDate = occurrenceDate,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markUndone(medicationId: Long, occurrenceDate: Long) {
        medicationCompletionDao.markUndone(medicationId, occurrenceDate)
    }
}