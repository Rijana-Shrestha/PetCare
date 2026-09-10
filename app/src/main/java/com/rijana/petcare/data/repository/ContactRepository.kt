package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.ContactDao
import com.rijana.petcare.data.local.entity.Contact
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {

    fun getContactsForOwner(ownerId: Long): Flow<List<Contact>> =
        contactDao.getContactsForOwner(ownerId)

    suspend fun addContact(contact: Contact): Long =
        contactDao.insert(contact)

    suspend fun deleteContact(contact: Contact) =
        contactDao.delete(contact)
}