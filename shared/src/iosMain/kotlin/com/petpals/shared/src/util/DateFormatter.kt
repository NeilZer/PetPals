
package com.petpals.shared.src.util

actual class DateFormatter {

    private val dateFormatter = NSDateFormatter()

    init {
        dateFormatter.locale = NSLocale(localeIdentifier = "he_IL")
    }

    actual fun formatTimeAgo(timestampMillis: Long): String {
        val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
        val now = NSDate()
        val timeInterval = now.timeIntervalSinceDate(date)

        val seconds = timeInterval.toInt()
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

    actual fun formatDate(timestampMillis: Long, pattern: String): String {
        dateFormatter.dateFormat = pattern
        val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
        return dateFormatter.stringFromDate(date)
    }

    actual fun isToday(timestampMillis: Long): Boolean {
        val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
        val calendar = NSCalendar.currentCalendar
        val today = NSDate()

        return calendar.isDate(date, inSameDayAsDate = today)
    }

    actual fun formatShortDate(timestampMillis: Long): String {
        dateFormatter.dateStyle = NSDateFormatterShortStyle
        dateFormatter.timeStyle = NSDateFormatterNoStyle
        val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
        return dateFormatter.stringFromDate(date)
    }

    actual fun formatShortTime(timestampMillis: Long): String {
        dateFormatter.dateStyle = NSDateFormatterNoStyle
        dateFormatter.timeStyle = NSDateFormatterShortStyle
        val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
        return dateFormatter.stringFromDate(date)
    }
}
