package com.example.petpals.ui.screens

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.petpals.shared.src.model.Post
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    /**
     * יצירת פוסט חדש: כותב תמיד ל- /posts/<postId>
     * אם יש תמונה — מעלה לנתיב המורשה: postImages/<uid>/<postId>.jpg
     */
    suspend fun createPost(
        description: String,
        localImageUri: Uri?,
        lat: Double?,
        lng: Double?
    ): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in")
        val postId = db.collection("posts").document().id

        var imageUrl: String = ""
        if (localImageUri != null) {
            val imageRef = storage.reference.child("postImages/$uid/$postId.jpg")
            imageRef.putFile(localImageUri).await()
            imageUrl = imageRef.downloadUrl.await().toString()
        }

        val post = Post(
            postId = postId,
            userId = uid,
            description = description.trim(),
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            lat = lat,
            lng = lng,
            likes = emptyList()
        )

        db.collection("posts").document(postId).set(post).await()

        // אופציונלי: עותק בארכיון המשתמש
        // db.collection("users").document(uid)
        //   .collection("posts").document(postId).set(post).await()

        return postId
    }

    /** מאזין לפיד הגלובלי (Firestore) */
    fun listenFeed(onChange: (List<Post>) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration {
        return db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onError(e); return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                onChange(list)
            }
    }

    /** פוסט בודד לפי מזהה */
    suspend fun getPost(postId: String): Post? {
        val doc = db.collection("posts").document(postId).get().await()
        return doc.toObject(Post::class.java)
    }
}
