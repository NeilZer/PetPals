package com.petpals.shared.src.data

import com.petpals.shared.src.core.Result
import com.petpals.shared.src.model.Post
import kotlinx.coroutines.flow.Flow

/**
 * חוזה משותף לשתי הפלטפורמות.
 */
interface PostsRepository {

    suspend fun createPost(
        description: String,
        imageLocalPath: String?,   // מחרוזת ל-URI מקומי (באנדרואיד: content://... או file://...)
        lat: Double?,
        lng: Double?
    ): Result<String>            // מחזיר postId או שגיאה

    fun listenFeed(): Flow<List<Post>>

    suspend fun getPost(postId: String): Result<Post?>

    suspend fun toggleLike(postId: String, userId: String): Result<Unit>

    suspend fun addComment(postId: String, userId: String, text: String): Result<Unit>

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>

    suspend fun deletePost(postId: String, userId: String): Result<Unit>
}

/** מפעל תלויות משותף – לכל פלטפורמה יהיה actual */
expect fun providePostsRepository(): PostsRepository
