package com.rijana.petcare.data.repository

import com.rijana.petcare.data.local.dao.SavedPlaceDao
import com.rijana.petcare.data.local.entity.SavedPlace
import kotlinx.coroutines.flow.Flow

class SavedPlaceRepository(private val savedPlaceDao: SavedPlaceDao) {

    fun getPlacesForOwner(ownerId: Long): Flow<List<SavedPlace>> =
        savedPlaceDao.getPlacesForOwner(ownerId)

    suspend fun addPlace(place: SavedPlace): Long = savedPlaceDao.insert(place)

    suspend fun updatePlace(place: SavedPlace) = savedPlaceDao.update(place)

    suspend fun deletePlace(place: SavedPlace) = savedPlaceDao.delete(place)
}