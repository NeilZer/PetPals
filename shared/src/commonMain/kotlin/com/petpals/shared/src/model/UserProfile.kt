
package com.petpals.shared.src.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val petName: String = "",
    val petAge: Int = 0,
    val petBreed: String = "",
    val petImage: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowing: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
