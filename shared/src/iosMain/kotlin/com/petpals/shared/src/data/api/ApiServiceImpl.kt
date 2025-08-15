
package com.petpals.shared.data.api

import com.petpals.shared.model.*
import com.petpals.shared.util.LatLng
import com.petpals.shared.util.Time
import kotlinx.coroutines.delay

actual class ApiServiceImpl : ApiService {

    // Mock data for development
    private val mockUsers = mutableMapOf<String, UserProfile>()
    private val mockPosts = mutableMapOf<String, Post>()
    private val mockComments = mutableMapOf<String, MutableList<Comment>>()

    actual override suspend fun signUp(email: String, password: String): String {
        delay(1000) // Simulate network delay
        val userId = "user_${System.currentTimeMillis()}"
        return userId
    }

    actual override suspend fun signIn(email: String, password: String): String {
        delay(1000)
        return "current_user_id"
    }

    actual override suspend fun signOut() {
        delay(500)
    }

    actual override suspend fun getCurrentUserId(): String? {
        return "current_user_id"
    }

    actual override suspend fun sendEmailVerification() {
        delay(1000)
    }

    actual override suspend fun isEmailVerified(): Boolean {
        return true
    }

    actual override suspend fun sendPasswordResetEmail(email: String) {
        delay(1000)
    }

    actual override suspend fun getCurrentUser(): UserProfile {
        delay(500)
        return mockUsers["current_user_id"] ?: UserProfile(
            userId = "current_user_id",
            petName = "בובי",
            age = 3,
            breed = "לברדור",
            imageUrl = ""
        )
    }

    actual override suspend fun getUser(userId: String): UserProfile {
        delay(500)
        return mockUsers[userId] ?: UserProfile(
            userId = userId,
            petName = "חיית מחמד",
            age = 2,
            breed = "מעורב",
            imageUrl = ""
        )
    }

    actual override suspend fun updateUser(user: UserProfile): UserProfile {
        delay(1000)
        mockUsers[user.userId] = user
        return user
    }

    actual override suspend fun saveUserProfile(
        userId: String,
        name: String,
        age: Int,
        breed: String,
        imageUrl: String
    ) {
        delay(1000)
        mockUsers[userId] = UserProfile(
            userId = userId,
            petName = name,
            age = age,
            breed = breed,
            imageUrl = imageUrl
        )
    }

    actual override suspend fun updateUserLocation(userId: String, location: LatLng) {
        delay(500)
        // Update user location in mock data
    }

    actual override suspend fun getNearbyUsers(
        currentLocation: LatLng,
        radiusKm: Double
    ): List<MapUserMarker> {
        delay(1000)
        return listOf(
            MapUserMarker(
                userId = "user1",
                petName = "מקס",
                location = LatLng(32.0853, 34.7818),
                imageUrl = "",
                distance = 1.2
            ),
            MapUserMarker(
                userId = "user2",
                petName = "לונה",
                location = LatLng(32.0863, 34.7828),
                imageUrl = "",
                distance = 0.8
            )
        )
    }

    actual override suspend fun followUser(userId: String) {
        delay(500)
    }

    actual override suspend fun unfollowUser(userId: String) {
        delay(500)
    }

    actual override suspend fun getFollowers(userId: String): List<UserProfile> {
        delay(500)
        return emptyList()
    }

    actual override suspend fun getFollowing(userId: String): List<UserProfile> {
        delay(500)
        return emptyList()
    }

    actual override suspend fun createPost(post: Post): Post {
        delay(1000)
        mockPosts[post.postId] = post
        return post
    }

    actual override suspend fun generatePostId(): String {
        return "post_${System.currentTimeMillis()}"
    }

    actual override suspend fun getPosts(): List<Post> {
        delay(1000)
        return mockPosts.values.toList().sortedByDescending { it.timestamp }
    }

    actual override suspend fun getPostById(postId: String): Post {
        delay(500)
        return mockPosts[postId] ?: throw Exception("Post not found")
    }

    actual override suspend fun likePost(postId: String, userId: String) {
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

    actual override suspend fun unlikePost(postId: String, userId: String) {
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

    actual override suspend fun toggleLike(postId: String, userId: String) {
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

    actual override suspend fun deletePost(postId: String) {
        delay(500)
        mockPosts.remove(postId)
    }

    actual override suspend fun addComment(comment: Comment): Comment {
        delay(500)
        val postComments = mockComments.getOrPut(comment.postId) { mutableListOf() }
        postComments.add(comment)
        return comment
    }

    actual override suspend fun addCommentToPost(postId: String, userId: String, text: String) {
        delay(500)
        val comment = Comment(
            commentId = "comment_${System.currentTimeMillis()}",
            postId = postId,
            userId = userId,
            text = text,
            timestamp = Time.getCurrentTimestamp()
        )
        addComment(comment)
    }

    actual override suspend fun getComments(postId: String): List<Comment> {
        delay(500)
        return mockComments[postId]?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    actual override suspend fun deleteComment(postId: String, commentId: String) {
        delay(500)
        mockComments[postId]?.removeAll { it.commentId == commentId }
    }

    actual override suspend fun getNearbyPosts(
        currentLocation: LatLng,
        radiusKm: Double
    ): List<MapPostMarker> {
        delay(1000)
        return listOf(
            MapPostMarker(
                postId = "post1",
                petName = "מקס",
                location = LatLng(32.0853, 34.7818),
                description = "טיול בפארק",
                imageUrl = "",
                timestamp = Time.getCurrentTimestamp(),
                distance = 0.5
            ),
            MapPostMarker(
                postId = "post2",
                petName = "לונה",
                location = LatLng(32.0863, 34.7828),
                description = "משחק על החוף",
                imageUrl = "",
                timestamp = Time.getCurrentTimestamp() - 3600,
                distance = 1.2
            )
        )
    }
}
