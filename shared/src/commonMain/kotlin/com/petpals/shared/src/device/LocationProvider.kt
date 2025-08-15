package com.petpals.shared.src.device

import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

expect class LocationProvider() {
    suspend fun getCurrentLocation(): Result<LatLng>
    suspend fun requestLocationPermissions(): Result<Boolean>
    fun getLocationUpdates(): Flow<LatLng>
    fun isLocationEnabled(): Boolean
}