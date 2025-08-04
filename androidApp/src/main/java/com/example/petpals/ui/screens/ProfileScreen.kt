package com.example.petpals.ui.screens

import android.util.Log
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
import com.example.petpals.ui.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val petName: String = "",
    val petAge: Int = 0,
    val petBreed: String = "",
    val petImage: String = ""
)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val loadedProfile = loadUserProfile(userId)
                profile = loadedProfile
                posts = loadUserPosts(userId)
            } catch (e: Exception) {
                Log.e("PROFILE", "Error loading profile/posts", e)
            } finally {
                isLoading = false
            }
        } else isLoading = false
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        userId == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please log in to view your profile")
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🔹 פרטי פרופיל
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (profile?.petImage?.isNotEmpty() == true) {
                        Image(
                            painter = rememberAsyncImagePainter(profile!!.petImage),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (profile != null && profile!!.petName.isNotEmpty()) {
                            "${profile!!.petName} (${profile!!.petBreed}, ${profile!!.petAge} y/o)"
                        } else {
                            "No pet profile yet"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navController.navigate(Screen.EditProfile.route) }) {
                        Text(if (profile != null) "Edit Profile" else "Create Profile")
                    }
                }
            }

            // 🔹 פוסטים של המשתמש
            if (posts.isNotEmpty()) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (post.imageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(post.imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(200.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(post.text, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Likes: ${post.likes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else item { Text("No posts yet", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}



// 🔹 טעינת פרופיל המשתמש
suspend fun loadUserProfile(userId: String): UserProfile {
    val db = FirebaseFirestore.getInstance()
    return try {
        val doc = db.collection("users").document(userId).get().await()
        if (doc.exists()) {
            UserProfile(
                petName = doc.getString("petName") ?: "",
                petAge = doc.getLong("petAge")?.toInt() ?: 0,
                petBreed = doc.getString("petBreed") ?: "",
                petImage = doc.getString("petImage") ?: ""
            )
        } else UserProfile()
    } catch (e: Exception) {
        Log.e("PROFILE", "Failed to load user profile", e)
        UserProfile()
    }
}

// 🔹 טעינת פוסטים של המשתמש
suspend fun loadUserPosts(userId: String): List<FeedPost> {
    val db = FirebaseFirestore.getInstance()
    return try {
        val snapshot = db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        snapshot.documents.map { doc ->
            FeedPost(
                postId = doc.id,
                userId = userId,
                text = doc.getString("text") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                petName = doc.getString("petName") ?: doc.getString("userName") ?: "",
                likes = doc.getLong("likes")?.toInt() ?: 0
            )
        }
    } catch (e: Exception) {
        Log.e("PROFILE", "Failed to load posts", e)
        emptyList()
    }
}
