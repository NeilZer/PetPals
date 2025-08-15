package com.petpals.shared.src.data.api

import com.petpals.shared.src.model.Post
import com.petpals.shared.src.model.UserProfile
import com.petpals.shared.src.model.Comment
import com.petpals.shared.src.model.MapUserMarker
import com.petpals.shared.src.model.MapPostMarker
import com.petpals.shared.src.util.LatLng

expect class ApiService {
    // Auth API
    suspend fun signUp(email: String, password: String): String
    suspend fun signIn(email: String, password: String): String
    suspend fun signOut()
    suspend fun getCurrentUserId(): String?
    suspend fun sendEmailVerification()
    suspend fun isEmailVerified(): Boolean
    suspend fun sendPasswordResetEmail(email: String)

    // Posts API
    suspend fun getPosts(): List<Post>
    suspend fun getPostById(postId: String): Post
    suspend fun createPost(post: Post): Post
    suspend fun generatePostId(): String
    suspend fun likePost(postId: String, userId: String)
    suspend fun unlikePost(postId: String, userId: String)
    suspend fun toggleLike(postId: String, userId: String)
    suspend fun deletePost(postId: String)
    suspend fun getNearbyPosts(location: LatLng, radiusKm: Double): List<MapPostMarker>

    // Users API
    suspend fun getCurrentUser(): UserProfile
    suspend fun getUser(userId: String): UserProfile
    suspend fun updateUser(user: UserProfile): UserProfile
    suspend fun saveUserProfile(userId: String, name: String, age: Int, breed: String, imageUrl: String)
    suspend fun updateUserLocation(userId: String, location: LatLng)
    suspend fun getNearbyUsers(location: LatLng, radiusKm: Double): List<MapUserMarker>
    suspend fun followUser(userId: String)
    suspend fun unfollowUser(userId: String)
    suspend fun getFollowers(userId: String): List<UserProfile>
    suspend fun getFollowing(userId: String): List<UserProfile>

    // Comments API
    suspend fun getComments(postId: String): List<Comment>
    suspend fun addComment(comment: Comment): Comment
    suspend fun addCommentToPost(postId: String, userId: String, text: String)
    suspend fun deleteComment(postId: String, commentId: String)
}
