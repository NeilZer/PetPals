
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.core.AppResult
import com.petpals.shared.src.core.Result
import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.domain.repository.IUserRepository
import com.petpals.shared.src.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserRepositoryImpl(private val api: ApiService = ApiService()) : IUserRepository {
    override suspend fun getCurrentUser(): AppResult<UserProfile> = runCatching { api.getCurrentUser() }
        .fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(it) })

    override suspend fun getUserById(userId: String): AppResult<UserProfile> = runCatching { api.getUser(userId) }
        .fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(it) })

    override fun observeUser(userId: String): Flow<AppResult<UserProfile>> = flow { emit(getUserById(userId)) }
}
