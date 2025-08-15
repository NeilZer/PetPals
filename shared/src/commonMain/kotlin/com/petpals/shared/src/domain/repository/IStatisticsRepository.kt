package com.petpals.shared.src.domain.repository

import com.petpals.shared.src.model.MonthlyStats
import com.petpals.shared.src.core.Result

interface IStatisticsRepository {
    suspend fun loadStatistics(userId: String): Result<StatisticsData>
    suspend fun updateUserActivity(userId: String): Result<Boolean>
    suspend fun incrementPostCount(userId: String): Result<Boolean>
    suspend fun incrementLikeCount(userId: String): Result<Boolean>
    suspend fun addDistance(userId: String, distance: Double): Result<Boolean>
}

data class StatisticsData(
    val monthlyStats: List<MonthlyStats>,
    val totalPosts: Int,
    val totalLikes: Int,
    val totalDistance: Double,
    val activeDaysThisMonth: Int,
    val averageLikesPerPost: Double = if (totalPosts > 0) totalLikes.toDouble() / totalPosts else 0.0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0
)