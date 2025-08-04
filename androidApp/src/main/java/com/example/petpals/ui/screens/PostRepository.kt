package com.petpals.shared

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val likes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class PostRepository {

    private val db = FirebaseFirestore.getInstance()

    /** ✅ זרימה של כל הפוסטים מהפיירבייס לפיד */
    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    /** ✅ זרימה של פוסטים של משתמש מסוים */
    fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    /** ✅ הוספת פוסט חדש */
    suspend fun addPost(post: Post) {
        db.collection("posts").document(post.id).set(post).await()
    }
}
