package com.petpals.shared.src.util

actual class GeoCoder {
    actual suspend fun getAddressFromLocation(
        latitude: Double,
        longitude: Double
    ): Result<String> {
        TODO("Not yet implemented")
    }

    actual suspend fun getLocationFromAddress(address: String): Result<LatLng> {
        TODO("Not yet implemented")
    }

    actual fun cancel() {
    }

}