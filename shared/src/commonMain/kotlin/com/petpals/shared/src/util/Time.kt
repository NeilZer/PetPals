package com.petpals.shared.src.util

import kotlinx.datetime.*

/** המרה “בטוחה” ל־epoch millis מכל טיפוס נפוץ (Long/Double/String/Firestore-like). */
fun anyToEpochMillis(value: Any?): Long {
    if (value == null) return 0L
    return when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is String -> value.toLongOrNull() ?: 0L
        else -> {
            // תמיכה רפלקטיבית ב־Timestamp של פיירבייס (seconds + nanoseconds)
            try {
                val k = value::class
                val seconds = k.members.firstOrNull { it.name == "seconds" }?.call(value) as? Long
                val nanoseconds = k.members.firstOrNull { it.name == "nanoseconds" }?.call(value) as? Int
                if (seconds != null && nanoseconds != null) seconds * 1000 + nanoseconds / 1_000_000 else 0L
            } catch (_: Throwable) { 0L }
        }
    }
}

/** פורמט יחסי כמו באנדרואיד (עכשיו/ד׳/ש׳/תאריך). */
fun formatRelativeTime(epochMillis: Long, nowMillis: Long = Clock.System.now().toEpochMilliseconds()): String {
    if (epochMillis <= 0L) return ""
    val diff = nowMillis - epochMillis
    return when {
        diff < 60_000L -> "עכשיו"
        diff < 3_600_000L -> "${diff / 60_000} ד׳"
        diff < 86_400_000L -> "${diff / 3_600_000} ש׳"
        else -> {
            val date = Instant.fromEpochMilliseconds(epochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            "%02d/%02d/%04d".format(date.dayOfMonth, date.monthNumber, date.year)
        }
    }
}

/** שם זהה לזה שבקוד אנדרואיד – כדי שלא תצטרכי לשנות קריאות קיימות. */
fun formatTimestamp(timestamp: Long): String = formatRelativeTime(timestamp)
