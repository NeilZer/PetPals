
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.domain.repository.IAuthRepository
import com.petpals.shared.src.core.Result
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

actual class AuthRepositoryImpl actual constructor() : IAuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    actual override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw IllegalStateException("User ID is null")
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw IllegalStateException("User ID is null")
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getCurrentUserId(): Result<String?> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            user?.sendEmailVerification()?.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun isEmailVerified(): Result<Boolean> {
        return try {
            val user = firebaseAuth.currentUser
            val isVerified = user?.isEmailVerified ?: false
            Result.Success(isVerified)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}