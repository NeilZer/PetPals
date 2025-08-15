package com.petpals.shared.src.util

import kotlin.math.*

data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calculate distance to another LatLng point using Haversine formula
     * @param other The other LatLng point
     * @return Distance in kilometers
     */
    fun distanceTo(other: LatLng): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = (other.latitude - latitude) * PI / 180.0
        val dLng = (other.longitude - longitude) * PI / 180.0

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(latitude * PI / 180.0) * cos(other.latitude * PI / 180.0) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    companion object {
        val INVALID = LatLng(0.0, 0.0)
    }
}
