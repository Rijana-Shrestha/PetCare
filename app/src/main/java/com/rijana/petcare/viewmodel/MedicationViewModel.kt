package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.MedicationStatus
import com.rijana.petcare.data.repository.MedicationRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Pairs a Medication definition with whether TODAY's dose has been checked off -
// same per-occurrence idea as RoutineOccurrence, now backed by MedicationCompletion.
data class MedicationOccurrence(
    val medication: Medication,
    val isCompleted: Boolean
)

class MedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ownerId = MutableStateFlow<Long?>(null)

    val medications: StateFlow<List<Medication>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> medicationRepository.getMedicationsForOwner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Medications active today, each paired with today's completion status.
    val medicationsForToday: StateFlow<List<MedicationOccurrence>> = medications
        .map { list -> list.filter { isActiveToday(it) } }
        .flatMapLatest { activeToday ->
            if (activeToday.isEmpty()) {
                flowOf(emptyList())
            } else {
                val today = DateUtils.startOfDay(System.currentTimeMillis())
                combine(
                    activeToday.map { medication ->
                        medicationRepository.getCompletion(medication.id, today)
                            .map { completion -> MedicationOccurrence(medication, completion != null) }
                    }
                ) { it.toList() }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        resolveOwnerId()
    }

    private fun resolveOwnerId() {
        viewModelScope.launch {
            val firebaseUid = userRepository.currentFirebaseUid ?: return@launch
            val localUser = userRepository.getUserProfile(firebaseUid).first()
            _ownerId.value = localUser?.id
        }
    }

    fun toggleComplete(occurrence: MedicationOccurrence) {
        viewModelScope.launch {
            val today = DateUtils.startOfDay(System.currentTimeMillis())
            if (occurrence.isCompleted) {
                medicationRepository.markUndone(occurrence.medication.id, today)
            } else {
                medicationRepository.markDone(occurrence.medication.id, today)
            }
        }
    }

    fun addMedication(medication: Medication) {
        viewModelScope.launch { medicationRepository.addMedication(medication) }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch { medicationRepository.deleteMedication(medication) }
    }

    private fun isActiveToday(medication: Medication): Boolean {
        if (medication.status == MedicationStatus.COMPLETED) return false
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val start = DateUtils.startOfDay(medication.startDate)
        val end = medication.endDate?.let { DateUtils.startOfDay(it) }
        return today >= start && (end == null || today <= end)
    }
}

class MedicationViewModelFactory(
    private val medicationRepository: MedicationRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicationViewModel(medicationRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}