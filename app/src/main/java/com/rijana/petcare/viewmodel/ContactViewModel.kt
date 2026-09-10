package com.rijana.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rijana.petcare.data.local.entity.Contact
import com.rijana.petcare.data.repository.ContactRepository
import com.rijana.petcare.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _ownerId = MutableStateFlow<Long?>(null)

    val contacts: StateFlow<List<Contact>> = _ownerId
        .filterNotNull()
        .flatMapLatest { id -> contactRepository.getContactsForOwner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val firebaseUid = userRepository.currentFirebaseUid ?: return@launch
            val localUser = userRepository.getUserProfile(firebaseUid).first()
            _ownerId.value = localUser?.id
        }
    }

    fun addContact(name: String, phoneNumber: String, relationship: String?) {
        val ownerId = _ownerId.value ?: return
        viewModelScope.launch {
            contactRepository.addContact(
                Contact(
                    ownerId = ownerId,
                    name = name,
                    phoneNumber = phoneNumber,
                    relationship = relationship?.ifBlank { null }
                )
            )
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch { contactRepository.deleteContact(contact) }
    }
}

class ContactViewModelFactory(
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactViewModel(contactRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}