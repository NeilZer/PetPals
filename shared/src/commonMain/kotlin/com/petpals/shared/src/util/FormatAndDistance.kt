
package com.petpals.shared.src.util

import kotlinx.datetime.*
import kotlin.math.*

/** המרחק בין שתי נקודות (lat/lng) במטרים – נוסחת Haversine. */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // meters
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLon = (lon2 - lon1) * PI / 180.0
    val a = sin(dLat/2).pow(2) + cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLon/2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

/** פורמט תצוגה ידידותי. אם זה היום – מציג שעה; אחרת תאריך קצר. */
fun formatTimestamp(epochMillis: Long): String {
    return try {
        val tz = TimeZone.currentSystemDefault()
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz)
        val now = Clock.System.now().toLocalDateTime(tz)
        if (dt.date == now.date) {
            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        } else {
            "${dt.date.dayOfMonth.toString().padStart(2, '0')}/${dt.date.monthNumber.toString().padStart(2, '0')}/${(dt.date.year % 100).toString().padStart(2, '0')}"
        }
    } catch (_: Throwable) {
        epochMillis.toString()
    }
}
