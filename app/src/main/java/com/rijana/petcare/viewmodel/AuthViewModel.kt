package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.User
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.util.mapSignInError
import com.rijana.petcare.util.mapSignUpError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthErrorField { EMAIL, PASSWORD, CONFIRM_PASSWORD, GENERAL }

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String, val field: AuthErrorField = AuthErrorField.GENERAL) : AuthUiState()
    object PasswordResetSent : AuthUiState()
}

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = userRepository.signUp(name, email, password)
                _uiState.value = AuthUiState.Success(user)
            } catch (e: Exception) {
                val error = mapSignUpError(e)
                _uiState.value = AuthUiState.Error(error.message, error.field)
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = userRepository.signIn(email, password)
                _uiState.value = AuthUiState.Success(user)
            } catch (e: Exception) {
                val error = mapSignInError(e)
                _uiState.value = AuthUiState.Error(error.message, error.field)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                userRepository.sendPasswordResetEmail(email)
                _uiState.value = AuthUiState.PasswordResetSent
            } catch (e: Exception) {
                val error = mapSignInError(e)
                _uiState.value = AuthUiState.Error(error.message, error.field)
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

class AuthViewModelFactory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}