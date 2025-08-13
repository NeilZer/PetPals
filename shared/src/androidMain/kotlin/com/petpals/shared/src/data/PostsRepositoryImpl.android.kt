package com.petpals.shared.src.data

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.petpals.shared.src.core.Result
import com.petpals.shared.src.model.Post
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * מימוש אנדרואיד למאגר הפוסטים.
 * ודאי שבנית את ה-dependencies של Firebase במודול shared:androidMain (כבר הוספנו ב-gradle).
 */
class AndroidPostsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : PostsRepository {

    override suspend fun createPost(
        description: String,
        imageLocalPath: String?,
        lat: Double?,
        lng: Double?
    ): Result<String> = try {
        val uid = auth.currentUser?.uid ?: error("User not signed in")
        val postId = db.collection("posts").document().id

        var imageUrl = ""
        if (!imageLocalPath.isNullOrEmpty()) {
            val ref = storage.reference.child("postImages/$uid/$postId.jpg")
            // מעלה את הקובץ
            ref.putFile(Uri.parse(imageLocalPath)).await()
            // משיג קישור הורדה
            imageUrl = ref.downloadUrl.await().toString()
        }

        val post = Post(
            postId = postId,
            userId = uid,
            description = description.trim(),
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            lat = lat,
            lng = lng,
            likes = emptyList()     // שימי לב: בשכבה המשותפת likes הוא List<String>
        )

        db.collection("posts").document(postId).set(post).await()
        Result.Ok(postId)
    } catch (t: Throwable) {
        Result.Err(t)
    }

    override fun listenFeed(): Flow<List<Post>> = callbackFlow {
        val reg = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getPost(postId: String): Result<Post?> = try {
        val post = db.collection("posts").document(postId).get().await()
            .toObject(Post::class.java)
        Result.Ok(post)
    } catch (t: Throwable) {
        Result.Err(t)
    }

    override suspend fun toggleLike(postId: String, userId: String): Result<Unit> = try {
        val ref = db.collection("posts").document(postId)
        db.runTransaction { tr ->
            val snap = tr.get(ref)
            val likes = (snap.get("likes") as? List<String>)?.toMutableList() ?: mutableListOf()
            if (likes.contains(userId)) likes.remove(userId) else likes.add(userId)
            tr.update(ref, mapOf("likes" to likes))
        }.await()
        Result.Ok(Unit)
    } catch (t: Throwable) {
        Result.Err(t)
    }

    override suspend fun addComment(postId: String, userId: String, text: String): Result<Unit> = try {
        val userName = db.collection("users").document(userId).get().await()
            .getString("petName") ?: "Unknown"
        val data = mapOf(
            "userId" to userId,
            "userName" to userName,
            "text" to text,
            "timestamp" to Timestamp.now()
        )
        db.collection("posts").document(postId).collection("comments").add(data).await()
        Result.Ok(Unit)
    } catch (t: Throwable) {
        Result.Err(t)
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = try {
        db.collection("posts").document(postId)
            .collection("comments").document(commentId).delete().await()
        Result.Ok(Unit)
    } catch (t: Throwable) {
        Result.Err(t)
    }

    override suspend fun deletePost(postId: String, userId: String): Result<Unit> = try {
        db.collection("posts").document(postId).delete().await()
        // אם יש לך תת־אוסף myPosts אצל המשתמש – מחק גם שם
        db.collection("users").document(userId)
            .collection("myPosts").document(postId).delete().await()
        Result.Ok(Unit)
    } catch (t: Throwable) {
        Result.Err(t)
    }
}

/** actual תואם ל-expect שב-commonMain */
actual fun providePostsRepository(): PostsRepository = AndroidPostsRepository()
