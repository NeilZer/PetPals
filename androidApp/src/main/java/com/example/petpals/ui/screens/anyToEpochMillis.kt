// FirestoreTime.kt (או בכל קובץ utils משותף)
package com.example.petpals.util

fun anyToEpochMillis(value: Any?): Long {
    return when (value) {
        is com.google.firebase.Timestamp -> value.toDate().time
        is Long -> if (value < 1_000_000_000_000L) value * 1000 else value // seconds→ms
        is Int -> value.toLong() * 1000
        is Double -> if (value < 1e12) (value * 1000).toLong() else value.toLong()
        is Map<*, *> -> { // תמיכת חירום בייצוג {_seconds,_nanoseconds}
            val s = (value["_seconds"] as? Number)?.toLong()
            val ns = (value["_nanoseconds"] as? Number)?.toLong() ?: 0L
            if (s != null) s * 1000 + ns / 1_000_000 else 0L
        }
        is String -> value.toLongOrNull() ?: 0L
        else -> 0L
    }
}
