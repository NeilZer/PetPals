package com.petpals.shared.src.model


data class MonthlyStats(
    val month: String,
    val postsCount: Int,
    val likesReceived: Int,
    val totalDistance: Double,
    val activeDays: Int
)
