
package com.petpals.shared.src.model

data class Comment(
    val id: String = "",
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val text: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val userImageUrl: String? = null
)
