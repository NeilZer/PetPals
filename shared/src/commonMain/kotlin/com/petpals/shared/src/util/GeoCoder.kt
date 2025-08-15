package com.petpals.shared.src.util

expect class GeoCoder {
    suspend fun getAddressFromLocation(
        latitude: Double,
        longitude: Double
    ): Result<String>

    suspend fun getLocationFromAddress(address: String): Result<LatLng>
    fun cancel()

}