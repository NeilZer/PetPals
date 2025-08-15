package com.petpals.shared.src.platform

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

actual class LocationService actual constructor() {
    actual suspend fun getCurrentLocation(): Result<LatLng> {
        TODO("Not yet implemented")
    }

    actual fun getLocationUpdates(): Flow<Result<LatLng>> {
        TODO("Not yet implemented")
    }

    actual suspend fun hasLocationPermission(): Boolean {
        TODO("Not yet implemented")
    }

    actual suspend fun requestLocationPermission(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun startLocationUpdates() {
    }

    actual fun stopLocationUpdates() {
    }

    actual suspend fun isLocationServiceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

}