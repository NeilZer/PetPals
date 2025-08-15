
package com.petpals.shared.src.domain.usecases

import com.petpals.shared.src.domain.repository.IAuthRepository
import com.petpals.shared.src.core.Result

class AuthUseCase(
    private val authRepository: IAuthRepository
) {
    suspend fun signUp(email: String, password: String): Result<String> {
        if (!isValidEmail(email)) {
            return Result.Error(IllegalArgumentException("Invalid email format"))
        }

        if (!isValidPassword(password)) {
            return Result.Error(IllegalArgumentException("Password must be at least 6 characters"))
        }

        return authRepository.signUp(email, password)
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(IllegalArgumentException("Email and password cannot be empty"))
        }

        return authRepository.signIn(email, password)
    }

    suspend fun signOut(): Result<Unit> {
        return authRepository.signOut()
    }

    suspend fun getCurrentUserId(): Result<String?> {
        return authRepository.getCurrentUserId()
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return authRepository.sendEmailVerification()
    }

    suspend fun isEmailVerified(): Result<Boolean> {
        return authRepository.isEmailVerified()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (!isValidEmail(email)) {
            return Result.Error(IllegalArgumentException("Invalid email format"))
        }

        return authRepository.sendPasswordResetEmail(email)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
