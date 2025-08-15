
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.domain.repository.IAuthRepository

expect class AuthRepositoryImpl() : IAuthRepository {
    override suspend fun signUp(
        email: String,
        password: String
    ): Result<String>

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<String>

    override suspend fun signOut(): Result<Unit>
    override suspend fun getCurrentUserId(): Result<String?>
    override suspend fun sendEmailVerification(): Result<Unit>
    override suspend fun isEmailVerified(): Result<Boolean>
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
