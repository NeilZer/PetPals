package com.example.petpals.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class Post(
    val postId: String = "",
    val userId: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val lat: Double? = null,
    val lng: Double? = null,
    val likes: List<String> = emptyList()
)

class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    /**
     * יוצר פוסט חדש וכותב *תמיד* ל- /posts/<postId>.
     * אם יש תמונה — מעלה לנתיב שמותר ע"פ החוקים: postImages/<uid>/<postId>.jpg
     */
    suspend fun createPost(
        description: String,
        localImageUri: Uri?,
        lat: Double?,
        lng: Double?
    ): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("User not signed in")
        val postId = db.collection("posts").document().id

        var imageUrl: String? = null
        if (localImageUri != null) {
            // <-- תואם לחוקי ה-Storage שלך
            val imageRef = storage.reference.child("postImages/$uid/$postId.jpg")
            imageUrl = imageRef.putFile(localImageUri).await()
                .storage.downloadUrl.await().toString()
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

        // לכתוב ל-top-level feed
        db.collection("posts").document(postId).set(post).await()

        // אם חשוב לך גם לשמור לארכיון של המשתמש, תשמור בעותק (לא חובה):
        // db.collection("users").document(uid)
        //   .collection("posts").document(postId).set(post).await()

        return postId
    }

    /** מאזין לפיד הגלובלי */
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

    /** מביא פוסט בודד לפי מזהה */
    suspend fun getPost(postId: String): Post? {
        val doc = db.collection("posts").document(postId).get().await()
        return doc.toObject(Post::class.java)
    }
}
