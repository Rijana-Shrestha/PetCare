package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.repository.MedicationRepository
import com.rijana.petcare.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ownerId = MutableStateFlow<Long?>(null)

    val medications: StateFlow<List<Medication>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> medicationRepository.getMedicationsForOwner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addMedication(medication: Medication) {
        viewModelScope.launch { medicationRepository.addMedication(medication) }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch { medicationRepository.deleteMedication(medication) }
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