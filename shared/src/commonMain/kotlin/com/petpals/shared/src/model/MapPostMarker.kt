package com.petpals.shared.src.model

import com.petpals.shared.src.util.LatLng

data class MapPostMarker(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val location: LatLng,
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val petName: String = "",
    val petBreed: String = ""
)