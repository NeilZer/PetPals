package com.petpals.shared.src.data.repository

import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.domain.repository.IUserRepository
import com.petpals.shared.src.model.UserProfile
import com.petpals.shared.src.model.MapUserMarker
import com.petpals.shared.src.util.LatLng
import com.petpals.shared.src.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class UserRepositoryImpl actual constructor(
    private val apiService: ApiService
) : IUserRepository {

    actual override suspend fun getCurrentUser(): Result<UserProfile> {
        return try {
            val user = apiService.getCurrentUser()
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getUserById(userId: String): Result<UserProfile> {
        return try {
            val user = apiService.getUser(userId)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun loadUserProfile(userId: String): Result<UserProfile> {
        return try {
            val user = apiService.getUser(userId)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun updateUserProfile(user: UserProfile): Result<UserProfile> {
        return try {
            val updatedUser = apiService.updateUser(user)
            Result.Success(updatedUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun saveUserProfile(
        userId: String,
        name: String,
        age: Int,
        breed: String,
        imageUrl: String
    ): Result<Unit> {
        return try {
            apiService.saveUserProfile(userId, name, age, breed, imageUrl)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun updateUserLocation(userId: String, location: LatLng): Result<Unit> {
        return try {
            apiService.updateUserLocation(userId, location)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun getNearbyUsers(currentLocation: LatLng, radiusKm: Double): Result<List<MapUserMarker>> {
        return try {
            val users = apiService.getNearbyUsers(currentLocation, radiusKm)
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun followUser(userId: String): Result<Unit> {
        return try {
            apiService.followUser(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun unfollowUser(userId: String): Result<Unit> {
        return try {
            apiService.unfollowUser(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun getFollowers(userId: String): Result<List<UserProfile>> {
        return try {
            val followers = apiService.getFollowers(userId)
            Result.Success(followers)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual  suspend fun getFollowing(userId: String): Result<List<UserProfile>> {
        return try {
            val following = apiService.getFollowing(userId)
            Result.Success(following)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override fun observeUser(userId: String): Flow<Result<UserProfile>> = flow {
        try {
            val user = apiService.getUser(userId)
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
