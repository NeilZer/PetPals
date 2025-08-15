
package com.petpals.shared.src.data.api

import com.petpals.shared.src.model.*
import com.petpals.shared.src.util.LatLng
import com.petpals.shared.src.util.Time
import io.ktor.client.HttpClient

import kotlinx.coroutines.delay

actual class ApiService(httpClient: HttpClient) {

    // Mock data for development
    private val mockUsers = mutableMapOf<String, UserProfile>()
    private val mockPosts = mutableMapOf<String, Post>()
    private val mockComments = mutableMapOf<String, MutableList<Comment>>()

    actual suspend fun signUp(email: String, password: String): String {
        delay(1000) // Simulate network delay
        val userId = "user_${Time.nowMillis()}"
        return userId
    }

    actual suspend fun signIn(email: String, password: String): String {
        delay(1000)
        return "current_user_id"
    }

    actual suspend fun signOut() {
        delay(500)
    }

    actual suspend fun getCurrentUserId(): String? {
        return "current_user_id"
    }

    actual suspend fun sendEmailVerification() {
        delay(1000)
    }

    actual suspend fun isEmailVerified(): Boolean {
        return true
    }

    actual suspend fun sendPasswordResetEmail(email: String) {
        delay(1000)
    }

    actual suspend fun getCurrentUser(): UserProfile {
        delay(500)
        return mockUsers["current_user_id"] ?: UserProfile(
            id = "current_user_id",
            email = "test@example.com",
            displayName = "בובי",
            petName = "בובי",
            petAge = 3,
            petBreed = "לברדור",
            petImage = ""
        )
    }

    actual suspend fun getUser(userId: String): UserProfile {
        delay(500)
        return mockUsers[userId] ?: UserProfile(
            id = userId,
            email = "pet@example.com",
            displayName = "חיית מחמד",
            petName = "חיית מחמד",
            petAge = 2,
            petBreed = "מעורב",
            petImage = ""
        )
    }

    actual suspend fun updateUser(user: UserProfile): UserProfile {
        delay(1000)
        mockUsers[user.id] = user
        return user
    }

    actual suspend fun saveUserProfile(
        userId: String,
        name: String,
        age: Int,
        breed: String,
        imageUrl: String
    ) {
        delay(1000)
        mockUsers[userId] = UserProfile(
            id = userId,
            displayName = name,
            petName = name,
            petAge = age,
            petBreed = breed,
            petImage = imageUrl
        )
    }

    actual suspend fun updateUserLocation(userId: String, location: LatLng) {
        delay(500)
        // Update user location in mock data
    }

    actual suspend fun getNearbyUsers(
        location: LatLng,
        radiusKm: Double
    ): List<MapUserMarker> {
        delay(1000)
        return listOf(
            MapUserMarker(
                id = "user1",
                userId = "user1",
                displayName = "מקס",
                petName = "מקס",
                imageUrl = "",
                lat = 32.0853,
                lng = 34.7818,
                distanceKm = 1.2
            ),
            MapUserMarker(
                id = "user2",
                userId = "user2",
                displayName = "לונה",
                petName = "לונה",
                imageUrl = "",
                lat = 32.0863,
                lng = 34.7828,
                distanceKm = 0.8
            )
        )
    }

    actual suspend fun followUser(userId: String) {
        delay(500)
    }

    actual suspend fun unfollowUser(userId: String) {
        delay(500)
    }

    actual suspend fun getFollowers(userId: String): List<UserProfile> {
        delay(500)
        return emptyList()
    }

    actual suspend fun getFollowing(userId: String): List<UserProfile> {
        return emptyList()
    }

    actual suspend fun createPost(post: Post): Post {
        delay(1000)
        mockPosts[post.postId] = post
        return post
    }

    actual suspend fun generatePostId(): String {
        return "post_${Time.nowMillis()}"
    }

    actual suspend fun getPosts(): List<Post> {
        delay(1000)
        return mockPosts.values.toList().sortedByDescending { it.timestamp }
    }

    actual suspend fun getPostById(postId: String): Post {
        delay(500)
        return mockPosts[postId] ?: throw Exception("Post not found")
    }

    actual suspend fun likePost(postId: String, userId: String) {
        delay(500)
        mockPosts[postId]?.let { post ->
            val updatedLikedBy = post.likedBy.toMutableList()
            if (!updatedLikedBy.contains(userId)) {
                updatedLikedBy.add(userId)
                mockPosts[postId] = post.copy(
                    likes = updatedLikedBy.size,
                    likedBy = updatedLikedBy
                )
            }
        }
    }

    actual suspend fun unlikePost(postId: String, userId: String) {
        delay(500)
        mockPosts[postId]?.let { post ->
            val updatedLikedBy = post.likedBy.toMutableList()
            if (updatedLikedBy.contains(userId)) {
                updatedLikedBy.remove(userId)
                mockPosts[postId] = post.copy(
                    likes = updatedLikedBy.size,
                    likedBy = updatedLikedBy
                )
            }
        }
    }

    actual suspend fun toggleLike(postId: String, userId: String) {
        delay(500)
        mockPosts[postId]?.let { post ->
            val updatedLikedBy = post.likedBy.toMutableList()
            if (updatedLikedBy.contains(userId)) {
                updatedLikedBy.remove(userId)
            } else {
                updatedLikedBy.add(userId)
            }
            mockPosts[postId] = post.copy(
                likes = updatedLikedBy.size,
                likedBy = updatedLikedBy
            )
        }
    }

    actual suspend fun deletePost(postId: String) {
        delay(500)
        mockPosts.remove(postId)
    }

    actual suspend fun addComment(comment: Comment): Comment {
        delay(500)
        val postComments = mockComments.getOrPut(comment.postId) { mutableListOf() }
        postComments.add(comment)
        return comment
    }

    actual suspend fun addCommentToPost(postId: String, userId: String, text: String) {
        delay(500)
        val comment = Comment(
            commentId = "comment_${Time.nowMillis()}",
            postId = postId,
            userId = userId,
            text = text,
            timestamp = Time.getCurrentTimestamp()
        )
        addComment(comment)
    }

    actual suspend fun getComments(postId: String): List<Comment> {
        delay(500)
        return mockComments[postId]?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    actual suspend fun deleteComment(postId: String, commentId: String) {
        delay(500)
        mockComments[postId]?.removeAll { it.commentId == commentId }
    }

    actual suspend fun getNearbyPosts(
        location: LatLng,
        radiusKm: Double
    ): List<MapPostMarker> {
        delay(1000)
        return listOf(
            MapPostMarker(
                id = "post1",
                postId = "post1",
                userId = "user1",
                userName = "מקס",
                userProfileImage = "",
                title = "טיול בפארק",
                description = "טיול בפארק",
                imageUrl = "",
                location = LatLng(32.0853, 34.7818),
                timestamp = Time.getCurrentTimestamp(),
                likesCount = 8,
                commentsCount = 3,
                petName = "מקס",
                petBreed = "לברדור"
            ),
            MapPostMarker(
                id = "post2",
                postId = "post2",
                userId = "user2",
                userName = "לונה",
                userProfileImage = "",
                title = "משחק על החוף",
                description = "משחק על החוף",
                imageUrl = "",
                location = LatLng(32.0863, 34.7828),
                timestamp = Time.getCurrentTimestamp() - 3600,
                likesCount = 12,
                commentsCount = 5,
                petName = "לונה",
                petBreed = "גולדן רטריבר"
            )
        )
    }
}
