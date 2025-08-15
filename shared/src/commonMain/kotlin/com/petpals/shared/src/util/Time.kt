
package com.petpals.shared.src.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

object Time {
    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun relativeShort(timestamp: Long): String {
        val delta = abs(nowMillis() - timestamp)
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

    fun isToday(timestamp: Long, tz: TimeZone = TimeZone.currentSystemDefault()): Boolean {
        val t = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(tz).date
        val now = Clock.System.now().toLocalDateTime(tz).date
        return t == now
    }
}
