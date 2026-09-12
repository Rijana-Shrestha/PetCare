package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.SavedPlace
import com.rijana.petcare.data.repository.SavedPlaceRepository
import com.rijana.petcare.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaceViewModel(
    private val placeRepository: SavedPlaceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ownerId = MutableStateFlow<Long?>(null)

    val places: StateFlow<List<SavedPlace>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> placeRepository.getPlacesForOwner(id) }
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

    fun addPlace(place: SavedPlace) {
        viewModelScope.launch { placeRepository.addPlace(place) }
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch { placeRepository.deletePlace(place) }
    }
}

class PlaceViewModelFactory(
    private val placeRepository: SavedPlaceRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaceViewModel(placeRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}