package com.petpals.shared.src.util

expect class DateFormatter {
    fun formatTimeAgo(timestampMillis: Long): String
    fun formatDate(timestampMillis: Long, pattern: String): String
    fun isToday(timestampMillis: Long): Boolean
    fun formatShortDate(timestampMillis: Long): String
    fun formatShortTime(timestampMillis: Long): String

}