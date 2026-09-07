package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getContactsForOwner(ownerId: Long): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Delete
    suspend fun delete(contact: Contact)
}