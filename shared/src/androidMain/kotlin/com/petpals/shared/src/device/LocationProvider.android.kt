package com.petpals.shared.src.device

import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): Result<LatLng> {
        TODO("Not yet implemented")
    }

    actual suspend fun requestLocationPermissions(): Result<Boolean> {
        TODO("Not yet implemented")
    }

    actual fun getLocationUpdates(): Flow<LatLng> {
        TODO("Not yet implemented")
    }

    actual fun isLocationEnabled(): Boolean {
        TODO("Not yet implemented")
    }
}