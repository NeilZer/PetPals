package com.petpals.shared.src.device

import kotlinx.coroutines.flow.Flow
import com.petpals.shared.src.util.LatLng

interface LocationProvider {
    suspend fun lastKnown(): LatLng?
    fun watch(intervalMs: Long = 5_000L): Flow<LatLng>
}

// יצירת אינסטנס פלטפורמי
expect fun provideLocationProvider(): LocationProvider
