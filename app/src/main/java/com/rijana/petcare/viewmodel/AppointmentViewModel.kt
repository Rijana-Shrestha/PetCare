package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.AppointmentStatus
import com.rijana.petcare.data.local.entity.GroomingAppointment
import com.rijana.petcare.data.local.entity.VetAppointment
import com.rijana.petcare.data.repository.GroomingAppointmentRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.data.repository.VetAppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppointmentViewModel(
    private val vetAppointmentRepository: VetAppointmentRepository,
    private val groomingAppointmentRepository: GroomingAppointmentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ownerId = MutableStateFlow<Long?>(null)

    val vetAppointments: StateFlow<List<VetAppointment>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> vetAppointmentRepository.getAppointmentsForOwner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groomingAppointments: StateFlow<List<GroomingAppointment>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> groomingAppointmentRepository.getAppointmentsForOwner(id) }
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

    fun addVetAppointment(appointment: VetAppointment) {
        viewModelScope.launch { vetAppointmentRepository.addAppointment(appointment) }
    }

    fun addGroomingAppointment(appointment: GroomingAppointment) {
        viewModelScope.launch { groomingAppointmentRepository.addAppointment(appointment) }
    }

    fun deleteVetAppointment(appointment: VetAppointment) {
        viewModelScope.launch { vetAppointmentRepository.deleteAppointment(appointment) }
    }

    fun deleteGroomingAppointment(appointment: GroomingAppointment) {
        viewModelScope.launch { groomingAppointmentRepository.deleteAppointment(appointment) }
    }
}

class AppointmentViewModelFactory(
    private val vetAppointmentRepository: VetAppointmentRepository,
    private val groomingAppointmentRepository: GroomingAppointmentRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppointmentViewModel(
                vetAppointmentRepository, groomingAppointmentRepository, userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}