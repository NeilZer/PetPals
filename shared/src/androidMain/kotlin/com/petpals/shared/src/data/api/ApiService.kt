
package com.petpals.shared.src.data.api

import com.petpals.shared.src.model.Post
import com.petpals.shared.src.model.UserProfile
import com.petpals.shared.src.model.Comment
import com.petpals.shared.src.model.MapUserMarker
import com.petpals.shared.src.model.MapPostMarker
import com.petpals.shared.src.util.LatLng
import com.petpals.shared.src.util.Time
import kotlinx.coroutines.delay

actual class ApiService {

    // Mock data for Android development
    private val mockUsers = mutableMapOf<String, UserProfile>()
    private val mockPosts = mutableMapOf<String, Post>()
    private val mockComments = mutableMapOf<String, MutableList<Comment>>()

    actual suspend fun signUp(email: String, password: String): String {
        delay(1000)
        return "android_user_${System.currentTimeMillis()}"
    }

    actual suspend fun signIn(email: String, password: String): String {
        delay(1000)
        return "android_current_user_id"
    }

    actual suspend fun signOut() {
        delay(500)
    }

    actual suspend fun getCurrentUserId(): String? {
        return "android_current_user_id"
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
        return mockUsers["android_current_user_id"] ?: UserProfile(
            id = "android_current_user_id",
            email = "test@example.com",
            displayName = "רקס",
            petName = "רקס",
            petAge = 4,
            petBreed = "גולדן רטריבר",
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
    }

    actual suspend fun getNearbyUsers(location: LatLng, radiusKm: Double): List<MapUserMarker> {
        delay(1000)
        return listOf(
            MapUserMarker(
                id = "android_user1",
                userId = "android_user1",
                displayName = "צ'רלי",
                petName = "צ'רלי",
                imageUrl = "",
                lat = 32.0853,
                lng = 34.7818,
                distanceKm = 1.5
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
        return "android_post_${System.currentTimeMillis()}"
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
            commentId = "android_comment_${System.currentTimeMillis()}",
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

    actual suspend fun getNearbyPosts(location: LatLng, radiusKm: Double): List<MapPostMarker> {
        delay(1000)
        return listOf(
            MapPostMarker(
                id = "android_post1",
                postId = "android_post1",
                userId = "android_user1",
                userName = "צ'רלי",
                userProfileImage = "",
                title = "יום חופש באנדרואיד",
                description = "יום חופש באנדרואיד",
                imageUrl = "",
                location = LatLng(32.0853, 34.7818),
                timestamp = Time.getCurrentTimestamp(),
                likesCount = 5,
                commentsCount = 2,
                petName = "צ'רלי",
                petBreed = "גולדן רטריבר"
            )
        )
    }
}
