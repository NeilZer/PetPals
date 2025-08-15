package com.petpals.shared.src.util

/**
 * פונקציות עזר נפוצות לכל הפלטפורמות
 */
object CommonUtils {

    /**
     * המרת ערך כלשהו לזמן במילישניות
     * מטפל בסוגים שונים של זמן
     */
    fun anyToEpochMillis(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is String -> value.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }

    /**
     * בדיקה אם ערך הוא זמן תקין
     */
    fun isValidTimestamp(timestamp: Long): Boolean {
        return timestamp > 0 && timestamp < Long.MAX_VALUE
    }

    /**
     * המרת זמן נוכחי למילישניות
     */
    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}