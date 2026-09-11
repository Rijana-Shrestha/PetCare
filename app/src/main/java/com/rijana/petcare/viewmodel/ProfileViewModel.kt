package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.User
import com.rijana.petcare.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = run {
        val uid = userRepository.currentFirebaseUid
        if (uid != null) {
            userRepository.getUserProfile(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        } else {
            MutableStateFlow(null)
        }
    }

    fun updateProfile(name: String, phone: String?, address: String?, profileImageUri: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            userRepository.updateProfile(
                user.copy(
                    name = name,
                    phone = phone,
                    address = address,
                    profileImageUri = profileImageUri ?: user.profileImageUri
                )
            )
        }
    }

    fun sendPasswordResetEmail(onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        val email = currentUser.value?.email ?: return
        viewModelScope.launch {
            try {
                userRepository.sendPasswordResetEmail(email)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun signOut() = userRepository.signOut()
}

class ProfileViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}