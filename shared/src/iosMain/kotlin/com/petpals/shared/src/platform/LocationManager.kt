
package com.petpals.shared.platform

import com.petpals.shared.util.LatLng
import com.petpals.shared.core.Result
import platform.CoreLocation.*
import platform.Foundation.NSError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.useContents

actual class LocationManager : NSObject(), CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    private val _location = MutableStateFlow<LatLng?>(null)
    private val _authorizationStatus = MutableStateFlow(CLAuthorizationStatus.kCLAuthorizationStatusNotDetermined)

    actual val location: Flow<LatLng?> = _location.asStateFlow()
    actual val authorizationStatus: Flow<Int> = _authorizationStatus.asStateFlow()

    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10.0
        _authorizationStatus.value = CLLocationManager.authorizationStatus()
    }

    actual fun requestLocationPermission() {
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.startUpdatingLocation()
            }
            else -> {
                // Permission denied or restricted
            }
        }
    }

    actual suspend fun getCurrentLocation(): Result<LatLng> {
        return try {
            when (CLLocationManager.authorizationStatus()) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways -> {
                    locationManager.requestLocation()
                    // For demo purposes, return Tel Aviv coordinates
                    Result.Success(LatLng(32.0853, 34.7818))
                }
                else -> {
                    Result.Error(SecurityException("Location permission not granted"))
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // CLLocationManagerDelegate methods
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        _authorizationStatus.value = manager.authorizationStatus
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                manager.startUpdatingLocation()
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                _location.value = null
            }
            else -> {
                // Handle other statuses if needed
            }
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val locations = didUpdateLocations as List<CLLocation>
        locations.lastOrNull()?.let { location ->
            location.coordinate.useContents {
                _location.value = LatLng(latitude, longitude)
            }
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        println("Location error: ${didFailWithError.localizedDescription}")
    }
}
