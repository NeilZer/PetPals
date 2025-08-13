package com.petpals.shared.src.model

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,   // epoch millis
    val userName: String = ""
)
