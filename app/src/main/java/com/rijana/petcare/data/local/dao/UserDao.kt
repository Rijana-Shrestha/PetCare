package com.rijana.petcare.data.local.dao

import androidx.room.*
import com.rijana.petcare.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid")
    fun getUserByFirebaseUid(firebaseUid: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)
}