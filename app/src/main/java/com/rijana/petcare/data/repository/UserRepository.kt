package com.rijana.petcare.data.repository

import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.dao.UserDao
import com.rijana.petcare.data.local.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserRepository(
    private val authManager: AuthManager,
    private val userDao: UserDao
) {

    val currentFirebaseUid: String?
        get() = authManager.currentUser?.uid

    suspend fun signUp(name: String, email: String, password: String): User {
        val firebaseUser = authManager.signUp(email, password)
        val newUser = User(firebaseUid = firebaseUser.uid, name = name, email = email)
        val localId = userDao.insert(newUser)
        return newUser.copy(id = localId)
    }

    suspend fun signIn(email: String, password: String): User {
        val firebaseUser = authManager.signIn(email, password)
        return getOrCreateLocalUser(firebaseUser.uid, firebaseUser.email ?: email)
    }

    private suspend fun getOrCreateLocalUser(firebaseUid: String, email: String): User {
        val existing = userDao.getUserByFirebaseUid(firebaseUid).first()
        if (existing != null) return existing

        val newUser = User(firebaseUid = firebaseUid, name = email.substringBefore("@"), email = email)
        val localId = userDao.insert(newUser)
        return newUser.copy(id = localId)
    }

    fun getUserProfile(firebaseUid: String): Flow<User?> = userDao.getUserByFirebaseUid(firebaseUid)

    fun signOut() = authManager.signOut()
}