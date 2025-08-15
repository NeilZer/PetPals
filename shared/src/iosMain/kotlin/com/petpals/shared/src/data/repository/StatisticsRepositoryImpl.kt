package com.petpals.shared.data.repository

import com.petpals.shared.data.api.ApiService
import com.petpals.shared.domain.repository.IStatisticsRepository
import com.petpals.shared.domain.repository.StatisticsData
import com.petpals.shared.model.MonthlyStats
import com.petpals.shared.core.Result
import kotlinx.coroutines.delay

actual class StatisticsRepositoryImpl actual constructor(
    private val apiService: ApiService
) : IStatisticsRepository {

    actual override suspend fun loadStatistics(userId: String): Result<StatisticsData> {
        return try {
            delay(1000) // Simulate network delay

            // Mock monthly statistics data
            val monthlyStats = listOf(
                MonthlyStats(
                    month = "ינואר 2024",
                    postsCount = 8,
                    likesReceived = 45,
                    totalDistance = 22.5,
                    activeDays = 15
                ),
                MonthlyStats(
                    month = "פברואר 2024",
                    postsCount = 12,
                    likesReceived = 67,
                    totalDistance = 31.2,
                    activeDays = 18
                ),
                MonthlyStats(
                    month = "מרץ 2024",
                    postsCount = 15,
                    likesReceived = 89,
                    totalDistance = 42.8,
                    activeDays = 22
                ),
                MonthlyStats(
                    month = "אפריל 2024",
                    postsCount = 10,
                    likesReceived = 56,
                    totalDistance = 28.6,
                    activeDays = 16
                ),
                MonthlyStats(
                    month = "מאי 2024",
                    postsCount = 18,
                    likesReceived = 102,
                    totalDistance = 48.3,
                    activeDays = 25
                ),
                MonthlyStats(
                    month = "יוני 2024",
                    postsCount = 14,
                    likesReceived = 78,
                    totalDistance = 35.7,
                    activeDays = 20
                )
            )

            val statistics = StatisticsData(
                monthlyStats = monthlyStats,
                totalPosts = monthlyStats.sumOf { it.postsCount },
                totalLikes = monthlyStats.sumOf { it.likesReceived },
                totalDistance = monthlyStats.sumOf { it.totalDistance },
                activeDaysThisMonth = monthlyStats.lastOrNull()?.activeDays ?: 0
            )

            Result.Success(statistics)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
