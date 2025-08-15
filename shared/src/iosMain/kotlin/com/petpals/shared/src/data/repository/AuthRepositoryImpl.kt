
package com.petpals.shared.data.repository

import com.petpals.shared.data.api.ApiService
import com.petpals.shared.domain.repository.IAuthRepository
import com.petpals.shared.core.Result

actual class AuthRepositoryImpl actual constructor(
    private val apiService: ApiService
) : IAuthRepository {

    actual override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val userId = apiService.signUp(email, password)
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val userId = apiService.signIn(email, password)
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun signOut(): Result<Unit> {
        return try {
            apiService.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getCurrentUserId(): Result<String?> {
        return try {
            val userId = apiService.getCurrentUserId()
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            apiService.sendEmailVerification()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun isEmailVerified(): Result<Boolean> {
        return try {
            val isVerified = apiService.isEmailVerified()
            Result.Success(isVerified)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            apiService.sendPasswordResetEmail(email)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
