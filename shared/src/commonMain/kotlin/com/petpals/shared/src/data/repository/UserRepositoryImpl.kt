
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.domain.repository.IUserRepository
import com.petpals.shared.src.model.MapUserMarker
import com.petpals.shared.src.model.UserProfile
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow


expect class UserRepositoryImpl(apiService: ApiService) :
    IUserRepository {
    override suspend fun getCurrentUser(): Result<UserProfile>
    override suspend fun getUserById(userId: String): Result<UserProfile>
    open suspend fun loadUserProfile(userId: String): Result<UserProfile>
    open suspend fun updateUserProfile(user: UserProfile): Result<UserProfile>
    open suspend fun saveUserProfile(
        userId: String,
        name: String,
        age: Int,
        breed: String,
        imageUrl: String
    ): Result<Unit>

    open suspend fun updateUserLocation(
        userId: String,
        location: LatLng
    ): Result<Unit>

    open suspend fun getNearbyUsers(
        currentLocation: LatLng,
        radiusKm: Double
    ): Result<List<MapUserMarker>>

    open suspend fun followUser(userId: String): Result<Unit>
    open suspend fun unfollowUser(userId: String): Result<Unit>
    open suspend fun getFollowers(userId: String): Result<List<UserProfile>>
    open suspend fun getFollowing(userId: String): Result<List<UserProfile>>
    override fun observeUser(userId: String): Flow<Result<UserProfile>>

}