package com.petpals.shared.src.model

import com.petpals.shared.src.util.LatLng

data class Post(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val content: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val petName: String = "",
    val petImage: String = "",
    val likes: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: Long = 0,
    val location: LatLng? = null,
    val locationString: String = "",
    val locationName: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val likedBy: List<String> = emptyList(),
    val isLiked: Boolean = false,
    val tags: List<String> = emptyList()
) {
    // Computed property for backward compatibility
    val locationLatLng: LatLng? get() = location ?: if (lat != null && lng != null) LatLng(lat, lng) else null
}

// Type alias for backward compatibility with Android code
typealias FeedPost = Post
