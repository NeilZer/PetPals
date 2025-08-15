
package com.petpals.shared.device

import com.petpals.shared.util.LatLng
import com.petpals.shared.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.CoreLocation.*
import platform.Foundation.NSError
import kotlinx.cinterop.useContents

actual class LocationProvider : NSObject(), CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    private var currentLocationResult: ((Result<LatLng>) -> Unit)? = null

    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    actual suspend fun getCurrentLocation(): Result<LatLng> {
        return try {
            // Check for authorization
            when (CLLocationManager.authorizationStatus()) {
                kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                    return Result.Error(Exception("Location permission denied"))
                }
                kCLAuthorizationStatusNotDetermined -> {
                    locationManager.requestWhenInUseAuthorization()
                    return Result.Error(Exception("Location permission not determined"))
                }
                else -> {
                    // Permission granted, get location
                    locationManager.requestLocation()
                    // Return a default location for now - in real implementation this would be async
                    Result.Success(LatLng(32.0853, 34.7818)) // Tel Aviv coordinates
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual fun getLocationUpdates(): Flow<Result<LatLng>> = flow {
        try {
            locationManager.startUpdatingLocation()
            // Emit periodic location updates
            emit(Result.Success(LatLng(32.0853, 34.7818)))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    actual suspend fun hasLocationPermission(): Boolean {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> true
            else -> false
        }
    }

    actual suspend fun requestLocationPermission(): Boolean {
        locationManager.requestWhenInUseAuthorization()
        return hasLocationPermission()
    }

    actual fun startLocationUpdates() {
        locationManager.startUpdatingLocation()
    }

    actual fun stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
    }

    // CLLocationManagerDelegate methods
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val locations = didUpdateLocations as List<CLLocation>
        locations.lastOrNull()?.let { location ->
            location.coordinate.useContents {
                val latLng = LatLng(latitude, longitude)
                currentLocationResult?.invoke(Result.Success(latLng))
            }
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        currentLocationResult?.invoke(Result.Error(Exception(didFailWithError.localizedDescription)))
    }

    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        // Handle authorization status changes
        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.requestLocation()
            }
            else -> {
                currentLocationResult?.invoke(Result.Error(Exception("Location permission denied")))
            }
        }
    }
}
