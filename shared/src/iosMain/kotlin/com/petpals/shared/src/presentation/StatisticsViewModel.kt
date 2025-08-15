
package com.petpals.shared.presentation

import com.petpals.shared.domain.repository.IStatisticsRepository
import com.petpals.shared.domain.repository.StatisticsData
import com.petpals.shared.domain.model.Achievement
import com.petpals.shared.domain.model.AchievementFactory
import com.petpals.shared.util.ChartHelper
import com.petpals.shared.core.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for statistics screen on iOS
 */
class StatisticsViewModel(
    private val statisticsRepository: IStatisticsRepository
) {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun loadStatistics(userId: String) {
        _isLoading.value = true
        _error.value = null

        when (val result = statisticsRepository.loadStatistics(userId)) {
            is Result.Success -> {
                val data = result.data
                val achievements = AchievementFactory.getAllAchievements(
                    totalPosts = data.totalPosts,
                    totalLikes = data.totalLikes,
                    totalDistance = data.totalDistance
                )

                _uiState.value = StatisticsUiState(
                    totalPosts = data.totalPosts,
                    totalLikes = data.totalLikes,
                    totalDistance = data.totalDistance,
                    activeDaysThisMonth = data.activeDaysThisMonth,
                    monthlyStats = data.monthlyStats,
                    achievements = achievements,
                    averageLikesPerPost = ChartHelper.calculateAverageLikes(data.totalLikes, data.totalPosts),
                    streakDays = calculateStreakDays(data.monthlyStats),
                    mostActiveDay = findMostActiveDay(data.monthlyStats),
                    favoriteLocation = "Tel Aviv Park" // Mock data
                )
            }
            is Result.Error -> {
                _error.value = result.exception.message ?: "Unknown error occurred"
            }
        }

        _isLoading.value = false
    }

    suspend fun refreshStatistics(userId: String) {
        loadStatistics(userId)
    }

    private fun calculateStreakDays(monthlyStats: List<com.petpals.shared.model.MonthlyStats>): Int {
        // Simple streak calculation - in real app would be more sophisticated
        return monthlyStats.lastOrNull()?.activeDays ?: 0
    }

    private fun findMostActiveDay(monthlyStats: List<com.petpals.shared.model.MonthlyStats>): String {
        // Mock implementation - in real app would analyze actual posting patterns
        val days = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
        return days.random()
    }
}

data class StatisticsUiState(
    val totalPosts: Int = 0,
    val totalLikes: Int = 0,
    val totalDistance: Double = 0.0,
    val activeDaysThisMonth: Int = 0,
    val monthlyStats: List<com.petpals.shared.model.MonthlyStats> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val averageLikesPerPost: Double = 0.0,
    val streakDays: Int = 0,
    val mostActiveDay: String = "",
    val favoriteLocation: String = ""
)
