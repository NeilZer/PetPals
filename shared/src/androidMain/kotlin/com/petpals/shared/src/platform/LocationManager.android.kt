package com.petpals.shared.src.platform

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

actual class LocationManager {
    actual val location: Flow<LatLng?>
        get() = TODO("Not yet implemented")
    actual val authorizationStatus: Flow<Int>
        get() = TODO("Not yet implemented")

    actual fun requestLocationPermission() {
    }

    actual suspend fun getCurrentLocation(): Result<LatLng> {
        TODO("Not yet implemented")
    }

}