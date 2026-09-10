package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.Routine
import com.rijana.petcare.data.local.entity.TaskRepeat
import com.rijana.petcare.data.repository.RoutineRepository
import com.rijana.petcare.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// Pairs a Routine definition with whether THIS specific day's occurrence
// has been checked off (see RoutineCompletion's per-occurrence design).
data class RoutineOccurrence(
    val routine: Routine,
    val isCompleted: Boolean
)

class RoutineViewModel(
    private val routineRepository: RoutineRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ownerId = MutableStateFlow<Long?>(null)

    private val _selectedDate = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = startOfDay(dateMillis)
    }

    private val allRoutines: StateFlow<List<Routine>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> routineRepository.getRoutinesForOwner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The list the UI actually renders: only routines that occur on the
    // selected day, each paired with that day's completion status.
    val routinesForSelectedDay: StateFlow<List<RoutineOccurrence>> =
        combine(allRoutines, _selectedDate) { routines, date ->
            routines.filter { occursOn(it, date) } to date
        }.flatMapLatest { (routinesToday, date) ->
            if (routinesToday.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    routinesToday.map { routine ->
                        routineRepository.getCompletion(routine.id, date)
                            .map { completion -> RoutineOccurrence(routine, completion != null) }
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

    fun toggleComplete(occurrence: RoutineOccurrence) {
        viewModelScope.launch {
            val date = _selectedDate.value
            if (occurrence.isCompleted) {
                routineRepository.markUndone(occurrence.routine.id, date)
            } else {
                routineRepository.markDone(occurrence.routine.id, date)
            }
        }
    }

    fun addRoutine(routine: Routine) {
        viewModelScope.launch { routineRepository.addRoutine(routine) }
    }

    fun updateRoutine(routine: Routine) {
        viewModelScope.launch { routineRepository.updateRoutine(routine) }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch { routineRepository.deleteRoutine(routine) }
    }

    companion object {
        fun startOfDay(millis: Long): Long =
            Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        private fun occursOn(routine: Routine, dateMillis: Long): Boolean {
            if (dateMillis < startOfDay(routine.startDate)) return false
            return when (routine.repeat) {
                TaskRepeat.EVERYDAY -> true
                TaskRepeat.WEEKDAYS -> isWeekday(dateMillis)
                TaskRepeat.CUSTOM -> matchesCustomDay(routine.customDays, dateMillis)
            }
        }

        private fun isWeekday(dateMillis: Long): Boolean {
            val day = Calendar.getInstance().apply { timeInMillis = dateMillis }
                .get(Calendar.DAY_OF_WEEK)
            return day != Calendar.SUNDAY && day != Calendar.SATURDAY
        }

        private fun matchesCustomDay(customDays: String?, dateMillis: Long): Boolean {
            if (customDays.isNullOrBlank()) return false
            val abbrev = when (
                Calendar.getInstance().apply { timeInMillis = dateMillis }.get(Calendar.DAY_OF_WEEK)
            ) {
                Calendar.MONDAY -> "MON"
                Calendar.TUESDAY -> "TUE"
                Calendar.WEDNESDAY -> "WED"
                Calendar.THURSDAY -> "THU"
                Calendar.FRIDAY -> "FRI"
                Calendar.SATURDAY -> "SAT"
                else -> "SUN"
            }
            return customDays.split(",").map { it.trim() }.contains(abbrev)
        }
    }
}

class RoutineViewModelFactory(
    private val routineRepository: RoutineRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineViewModel(routineRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}