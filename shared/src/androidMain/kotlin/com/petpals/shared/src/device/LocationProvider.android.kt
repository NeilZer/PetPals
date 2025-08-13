package com.petpals.shared.src.device

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(): LatLng? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val loc = client.lastLocation.await() ?: return null
        return LatLng(loc.latitude, loc.longitude)
    }

    @SuppressLint("MissingPermission")
    override fun watch(intervalMs: Long): Flow<LatLng> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)

        val request = LocationRequest.Builder(intervalMs)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    trySend(LatLng(it.latitude, it.longitude)).isSuccess
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}

/** מוזן ב־MainActivity לפני setContent */
lateinit var petPalsAppContext: Context

actual fun provideLocationProvider(): LocationProvider =
    AndroidLocationProvider(petPalsAppContext)
