package com.petpals.shared.src.platform

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

expect class LocationService() {
    suspend fun getCurrentLocation(): Result<LatLng>
    fun getLocationUpdates(): Flow<Result<LatLng>>
    suspend fun hasLocationPermission(): Boolean
    suspend fun requestLocationPermission(): Boolean
    fun startLocationUpdates()
    fun stopLocationUpdates()
    suspend fun isLocationServiceEnabled(): Boolean

}