package com.example.petpals.ui.screens

import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class FeedPost(
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val petName: String = "",
    val likes: Int = 0
)
@Composable
fun FeedScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener: ListenerRegistration = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                val rawPosts = snapshot.documents.mapNotNull { doc ->
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    FeedPost(
                        postId = doc.id,
                        userId = userId,
                        text = doc.getString("text") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        petName = "", // נעדכן אחר כך
                        likes = doc.getLong("likes")?.toInt() ?: 0
                    )
                }

                coroutineScope.launch {
                    val updatedPosts = rawPosts.map { post ->
                        val userDoc = db.collection("users").document(post.userId).get().await()
                        val petName = userDoc.getString("petName") ?: post.userId.take(5)
                        post.copy(petName = petName)
                    }
                    posts = updatedPosts
                    isLoading = false
                }
            }

        onDispose { listener.remove() }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (posts.isEmpty()) {
                item {
                    Text(
                        "No posts yet. Share your pet’s first photo!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                items(posts) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = post.petName.ifEmpty { "Unknown Pet" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (post.imageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(post.imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(post.text, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Likes: ${post.likes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
