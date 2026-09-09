package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.PetDao
import com.rijana.petcare.data.local.entity.Pet
import kotlinx.coroutines.flow.Flow

class PetRepository(private val petDao: PetDao) {

    fun getPetsForOwner(ownerId: Long): Flow<List<Pet>> =
        petDao.getPetsForOwner(ownerId)

    fun getPetById(petId: Long): Flow<Pet?> =
        petDao.getPetById(petId)

    suspend fun addPet(pet: Pet): Long =
        petDao.insert(pet)

    suspend fun updatePet(pet: Pet) =
        petDao.update(pet)

    suspend fun deletePet(pet: Pet) =
        petDao.delete(pet)
}