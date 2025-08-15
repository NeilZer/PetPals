
package com.petpals.shared.src.domain.repository

import com.petpals.shared.src.core.AppResult
import com.petpals.shared.src.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    suspend fun getCurrentUser(): AppResult<UserProfile>
    suspend fun getUserById(userId: String): AppResult<UserProfile>
    fun observeUser(userId: String): Flow<AppResult<UserProfile>>
}
