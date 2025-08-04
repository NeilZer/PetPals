package com.petpals.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,       // ✅ שם המשתמש
    val imageUrl: String,       // ✅ כתובת תמונה
    val text: String,
    val likes: Int = 0
)

class PostRepository {
    private val posts = MutableStateFlow<List<Post>>(emptyList())

    /** מחזיר את כל הפוסטים (פיד כללי) */
    fun getAllPosts(): Flow<List<Post>> = posts

    /** מחזיר את כל הפוסטים (כמו קודם) */
    fun getPosts(): Flow<List<Post>> = posts

    /** הוספת פוסט חדש לפיד */
    fun addPost(post: Post) {
        posts.value = posts.value + post
    }

    /** פוסטים לפי משתמש ספציפי */
    fun getUserPosts(userId: String): Flow<List<Post>> {
        return posts.map { list ->
            list.filter { it.userId == userId }
        }
    }
}
