package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.VetAppointment
import com.rijana.petcare.data.repository.MedicationRepository
import com.rijana.petcare.data.repository.VetAppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PetHistoryViewModel(
    private val medicationRepository: MedicationRepository,
    private val vetAppointmentRepository: VetAppointmentRepository
) : ViewModel() {

    fun getMedicationHistory(
        petId: Long
    ): Flow<List<Medication>> =
        medicationRepository.getMedicationsForPet(petId)

    fun getVaccinationHistory(
        petId: Long
    ): Flow<List<VetAppointment>> =
        vetAppointmentRepository
            .getAppointmentsForPet(petId)
            .map { appointments ->
                appointments.filter {
                    it.type.contains(
                        "vaccin",
                        ignoreCase = true
                    )
                }
            }
}

class PetHistoryViewModelFactory(
    private val medicationRepository: MedicationRepository,
    private val vetAppointmentRepository: VetAppointmentRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                PetHistoryViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return PetHistoryViewModel(
                medicationRepository,
                vetAppointmentRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}