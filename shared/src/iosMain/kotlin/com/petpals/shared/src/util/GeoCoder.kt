
package com.petpals.shared.src.util

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class GeoCoder {

    private val geocoder = CLGeocoder()

    actual suspend fun getAddressFromLocation(
        latitude: Double,
        longitude: Double
    ): Result<String> = suspendCancellableCoroutine { continuation ->

        val location = CLLocation(latitude = latitude, longitude = longitude)

        geocoder.reverseGeocodeLocation(location) { placemarks, error ->
            if (error != null) {
                continuation.resume(Result.Error(Exception(error.localizedDescription)))
                return@reverseGeocodeLocation
            }

            val placemark = placemarks?.firstOrNull() as? CLPlacemark
            if (placemark != null) {
                val addressParts = mutableListOf<String>()

                placemark.name?.let { addressParts.add(it) }
                placemark.locality?.let { addressParts.add(it) }
                placemark.country?.let { addressParts.add(it) }

                val address = if (addressParts.isNotEmpty()) {
                    addressParts.joinToString(separator = ", ")
                } else {
                    "📍 מיקום"
                }

                continuation.resume(Result.Success(address))
            } else {
                continuation.resume(Result.Success("📍 מיקום"))
            }
        }

        continuation.invokeOnCancellation {
            geocoder.cancelGeocode()
        }
    }

    actual suspend fun getLocationFromAddress(address: String): Result<LatLng> = suspendCancellableCoroutine { continuation ->

        geocoder.geocodeAddressString(address) { placemarks, error ->
            if (error != null) {
                continuation.resume(Result.Error(Exception(error.localizedDescription)))
                return@geocodeAddressString
            }

            val placemark = placemarks?.firstOrNull() as? CLPlacemark
            val location = placemark?.location

            if (location != null) {
                val latLng = LatLng(
                    latitude = location.coordinate.latitude,
                    longitude = location.coordinate.longitude
                )
                continuation.resume(Result.Success(latLng))
            } else {
                continuation.resume(Result.Error(Exception("Address not found")))
            }
        }

        continuation.invokeOnCancellation {
            geocoder.cancelGeocode()
        }
    }

    actual fun cancel() {
        geocoder.cancelGeocode()
    }
}
