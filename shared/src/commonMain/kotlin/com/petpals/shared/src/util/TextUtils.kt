package com.petpals.shared.src.util

import com.petpals.shared.src.model.UserProfile
import com.petpals.shared.src.model.Post
import com.petpals.shared.src.model.Comment

object TestUtils {

    fun createMockUser(
        id: String = "test_user_id",
        email: String = "test@example.com",
        displayName: String = "Test User",
        bio: String = "Test bio"
    ): UserProfile {
        return UserProfile(
            id = id,
            email = email,
            displayName = displayName,
            bio = bio,
            followersCount = 10,
            followingCount = 5,
            postsCount = 15
        )
    }

    fun createMockPost(
        id: String = "test_post_id",
        userId: String = "test_user_id",
        content: String = "Test post content",
        location: LatLng? = null
    ): Post {
        return Post(
            id = id,
            userId = userId,
            userName = "Test User",
            content = content,
            location = location,
            timestamp = System.currentTimeMillis(),
            likesCount = 5,
            commentsCount = 2
        )
    }

    fun createMockComment(
        id: String = "test_comment_id",
        postId: String = "test_post_id",
        userId: String = "test_user_id",
        content: String = "Test comment"
    ): Comment {
        return Comment(
            id = id,
            postId = postId,
            userId = userId,
            userName = "Test User",
            content = content,
            timestamp = System.currentTimeMillis(),
            likesCount = 1
        )
    }

    fun createMockLocation(
        latitude: Double = 32.0853,
        longitude: Double = 34.7818
    ): LatLng {
        return LatLng(latitude, longitude)
    }
}
