package com.petpals.shared.src.util

import com.google.firebase.Timestamp

/**
 * כלי עזר ספציפיים לאנדרואיד
 */
object AndroidUtils {

    /**
     * המרת Firebase Timestamp למילישניות
     */
    fun firebaseTimestampToMillis(timestamp: Timestamp): Long {
        return timestamp.toDate().time
    }

    /**
     * המרת ערך כלשהו למילישניות - גרסת אנדרואיד שתומכת ב-Firebase
     * כולל תמיכה רכה ב-map בסגנון {seconds=..., nanoseconds=...}
     */
    fun anyToEpochMillis(value: Any?): Long {
        return when (value) {
            null -> 0L
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Timestamp -> value.seconds * 1000L + (value.nanoseconds / 1_000_000)
            is String -> value.toLongOrNull() ?: 0L
            else -> try {
                // תמיכה רכה ב-map בסגנון {seconds=..., nanoseconds=...}
                val cls = value::class
                val secondsProp = cls.members.firstOrNull { it.name == "seconds" }?.call(value) as? Number
                val nanosProp = cls.members.firstOrNull { it.name == "nanoseconds" }?.call(value) as? Number
                if (secondsProp != null) {
                    secondsProp.toLong() * 1000L + (nanosProp?.toLong() ?: 0L) / 1_000_000L
                } else {
                    value.toString().toLongOrNull() ?: 0L
                }
            } catch (_: Throwable) {
                0L
            }
        }
    }
}
