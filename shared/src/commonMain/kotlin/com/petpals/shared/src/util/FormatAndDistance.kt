
package com.petpals.shared.src.util

import kotlinx.datetime.*
import kotlin.math.*

/** המרחק בין שתי נקודות (lat/lng) במטרים – נוסחת Haversine. */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2)
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
            "%02d:%02d".format(dt.hour, dt.minute)
        } else {
            "%02d/%02d/%02d".format(dt.date.dayOfMonth, dt.date.monthNumber, (dt.date.year % 100))
        }
    } catch (_: Throwable) {
        epochMillis.toString()
    }
}
