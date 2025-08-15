
package com.petpals.shared.util

import com.petpals.shared.model.MonthlyStats

/**
 * Helper class for chart data processing and formatting on iOS
 */
object ChartHelper {

    /**
     * Formats monthly stats data for chart display
     */
    fun formatMonthlyStatsForChart(stats: List<MonthlyStats>): List<ChartDataPoint> {
        return stats.map { stat ->
            ChartDataPoint(
                label = stat.month,
                posts = stat.postsCount,
                likes = stat.likesReceived,
                distance = stat.totalDistance,
                activeDays = stat.activeDays
            )
        }
    }

    /**
     * Calculates maximum values for chart scaling
     */
    fun getMaxValues(stats: List<MonthlyStats>): ChartMaxValues {
        return ChartMaxValues(
            maxPosts = stats.maxOfOrNull { it.postsCount } ?: 0,
            maxLikes = stats.maxOfOrNull { it.likesReceived } ?: 0,
            maxDistance = stats.maxOfOrNull { it.totalDistance } ?: 0.0,
            maxActiveDays = stats.maxOfOrNull { it.activeDays } ?: 0
        )
    }

    /**
     * Formats distance values for display
     */
    fun formatDistance(distance: Double): String {
        return when {
            distance < 1.0 -> "${(distance * 1000).toInt()}m"
            distance < 10.0 -> String.format("%.1fkm", distance)
            else -> "${distance.toInt()}km"
        }
    }

    /**
     * Calculates average likes per post
     */
    fun calculateAverageLikes(totalLikes: Int, totalPosts: Int): Double {
        return if (totalPosts > 0) totalLikes.toDouble() / totalPosts else 0.0
    }

    /**
     * Gets color for achievement level
     */
    fun getAchievementColor(level: AchievementLevel): String {
        return when (level) {
            AchievementLevel.BRONZE -> "#CD7F32"
            AchievementLevel.SILVER -> "#C0C0C0"
            AchievementLevel.GOLD -> "#FFD700"
            AchievementLevel.PLATINUM -> "#E5E4E2"
        }
    }
}

data class ChartDataPoint(
    val label: String,
    val posts: Int,
    val likes: Int,
    val distance: Double,
    val activeDays: Int
)

data class ChartMaxValues(
    val maxPosts: Int,
    val maxLikes: Int,
    val maxDistance: Double,
    val maxActiveDays: Int
)

enum class AchievementLevel {
    BRONZE, SILVER, GOLD, PLATINUM
}
