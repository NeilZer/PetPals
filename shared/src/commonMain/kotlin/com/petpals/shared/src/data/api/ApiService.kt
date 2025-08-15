
package com.petpals.shared.src.data.api

import com.petpals.shared.src.model.Post
import com.petpals.shared.src.model.*
import com.petpals.shared.src.platform.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ApiService(private val client: NetworkClient = NetworkClient()) {
    companion object {
        const val BASE_URL = "https://api.example.com"
    }

    suspend fun getCurrentUser(): UserProfile = client.httpClient().get("$BASE_URL/users/me").body()
    suspend fun getUser(userId: String): UserProfile = client.httpClient().get("$BASE_URL/users/$userId").body()
    suspend fun getFollowers(userId: String): List<UserProfile> = client.httpClient().get("$BASE_URL/users/$userId/followers").body()
    suspend fun getFollowing(userId: String): List<UserProfile> = client.httpClient().get("$BASE_URL/users/$userId/following").body()

    suspend fun getFeed(): List<Post> = client.httpClient().get("$BASE_URL/feed").body()

    suspend fun createPost(post: Post): Post = client.httpClient().post("$BASE_URL/posts") {
        contentType(ContentType.Application.Json); setBody(post)
    }.body()

    suspend fun likePost(postId: String, userId: String) {
        client.httpClient().post("$BASE_URL/posts/$postId/like") {
            contentType(ContentType.Application.Json); setBody(mapOf("userId" to userId))
        }
    }
}
