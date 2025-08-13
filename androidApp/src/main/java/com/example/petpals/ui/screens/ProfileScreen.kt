
package com.example.petpals.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.petpals.ui.Screen
import com.petpals.shared.src.util.anyToEpochMillis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.petpals.shared.src.model.UserProfile


@Composable
fun ProfileScreen(
    navController: NavHostController,
    userId: String
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var postListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    DisposableEffect(userId) {
        onDispose { postListener?.remove() }
    }

    LaunchedEffect(userId) {
        try {
            isLoading = true
            profile = loadUserProfile(userId)

            val db = FirebaseFirestore.getInstance()
            postListener = db.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.e("PROFILE", "Error listening to posts", error)
                        posts = emptyList()
                        return@addSnapshotListener
                    }
                    posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val geoPoint = doc.getGeoPoint("location")
                            val locationLatLng = geoPoint?.let {
                                com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                            }

                            FeedPost(
                                postId = doc.id,
                                userId = doc.getString("userId") ?: "",
                                text = doc.getString("text") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                petName = profile?.petName ?: "",
                                petImage = profile?.petImage ?: "",
                                likes = doc.getLong("likes")?.toInt() ?: 0,
                                timestamp = anyToEpochMillis(doc.get("timestamp")),
                                likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                                location = doc.getString("locationString") ?: "",
                                locationLatLng = locationLatLng
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

            errorMessage = null
        } catch (e: Exception) {
            Log.e("PROFILE", "Error loading profile", e)
            errorMessage = "שגיאה בטעינת הפרופיל"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "שגיאה לא ידועה")
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileHeader(
                        profile = profile,
                        onEditClick = {
                            if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
                                navController.navigate(Screen.EditProfile.route)
                            }
                        },
                        onLogoutClick = {
                            if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("הפוסטים של ${profile?.petName ?: "המשתמש"} (${posts.size})", style = MaterialTheme.typography.titleLarge)
                        Text("לחץ על פוסט לפרטים", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(8.dp))

                    if (posts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("עדיין אין פוסטים", style = MaterialTheme.typography.bodyMedium)
                                if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("לחץ על הכפתור למטה כדי ליצור פוסט ראשון", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(posts) { post ->
                                ProfilePostItem(
                                    post = post,
                                    onPostClick = { postId ->
                                        navController.navigate("${Screen.PostDetail.route}/$postId")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewPost.route) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "פוסט חדש")
            }
        }
    }
}

@Composable
fun ProfilePostItem(
    post: FeedPost,
    onPostClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onPostClick(post.postId) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            if (post.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.text.take(50) + if (post.text.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.likes > 0) {
                        Text(
                            text = "❤️${post.likes}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (post.locationLatLng != null) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "📍",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profile: UserProfile?,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (profile?.petImage?.isNotEmpty() == true) {
                Image(
                    painter = rememberAsyncImagePainter(profile.petImage),
                    contentDescription = "תמונת חיית המחמד",
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🐾", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (profile != null && profile.petName.isNotEmpty()) {
                Text(profile.petName, style = MaterialTheme.typography.headlineSmall)
                if (profile.petBreed.isNotEmpty() || profile.petAge > 0) {
                    Spacer(Modifier.height(4.dp))
                    val details = listOfNotNull(
                        profile.petBreed.takeIf { it.isNotEmpty() },
                        profile.petAge.takeIf { it > 0 }?.let { "$it שנים" }
                    ).joinToString(" • ")
                    Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("עדיין אין פרופיל חיית מחמד", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                    Text(if (profile?.petName?.isNotEmpty() == true) "ערוך פרופיל" else "צור פרופיל")
                }
                OutlinedButton(onClick = onLogoutClick, modifier = Modifier.weight(1f)) {
                    Text("התנתק")
                }
            }
        }
    }
}

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
        } else {
            UserProfile()
        }
    } catch (e: Exception) {
        Log.e("PROFILE", "Failed to load user profile", e)
        throw e
    }
}
