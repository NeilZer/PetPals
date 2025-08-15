package com.petpals.shared.src.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object Time {
    fun getCurrentTimestamp(): Long = Clock.System.now().toEpochMilliseconds()

    fun nowMillis(): Long = getCurrentTimestamp()

    fun formatTimeAgo(timestamp: Long): String {
        val now = getCurrentTimestamp()
        val delta = (now - timestamp).coerceAtLeast(0L)
        val seconds = delta / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds < 60 -> "עכשיו"
            minutes < 60 -> "${minutes}דק"
            hours < 24 -> "${hours}שע"
            days < 7 -> "${days}ימים"
            days < 30 -> "${days / 7}שבועות"
            else -> "${days / 30}חודשים"
        }
    }
}
