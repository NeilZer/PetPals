
package com.petpals.shared.src.domain.repository

import com.petpals.shared.src.core.Result

interface IAuthRepository {
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUserId(): Result<String?>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun isEmailVerified(): Result<Boolean>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
