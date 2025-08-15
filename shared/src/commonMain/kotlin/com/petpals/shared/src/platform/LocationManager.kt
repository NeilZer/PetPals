package com.petpals.shared.src.platform

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

expect class LocationManager {
    val location: Flow<LatLng?>
    val authorizationStatus: Flow<Int>
    fun requestLocationPermission()
    suspend fun getCurrentLocation(): Result<LatLng>

}