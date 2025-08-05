package com.example.petpals.ui.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.petpals.ui.Screen
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// פוסט בפיד - מעודכן עם מיקום
data class FeedPost(
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val petName: String = "",
    val petImage: String = "",
    val likes: Int = 0,
    val timestamp: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val location: String = "",
    val locationLatLng: LatLng? = null
)

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val userName: String = ""
)

@Composable
fun FeedScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // מאזין בזמן אמת
    DisposableEffect(Unit) {
        val listener: ListenerRegistration = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FEED", "Error listening to posts", error)
                    errorMessage = "שגיאה בטעינת הפוסטים"
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                val rawPosts = snapshot.documents.mapNotNull { doc ->
                    try {
                        val geoPoint = doc.getGeoPoint("location")
                        val locationLatLng = geoPoint?.let { LatLng(it.latitude, it.longitude) }

                        FeedPost(
                            postId = doc.id,
                            userId = doc.getString("userId") ?: return@mapNotNull null,
                            text = doc.getString("text") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            petName = "",
                            petImage = "",
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L,
                            likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                            location = doc.getString("locationString") ?: "",
                            locationLatLng = locationLatLng
                        )
                    } catch (e: Exception) {
                        Log.e("FEED", "Error parsing post ${doc.id}", e)
                        null
                    }
                }

                coroutineScope.launch {
                    try {
                        val updatedPosts = rawPosts.map { post ->
                            val userDoc = db.collection("users").document(post.userId).get().await()
                            post.copy(
                                petName = userDoc.getString("petName") ?: "Unknown Pet",
                                petImage = userDoc.getString("petImage") ?: ""
                            )
                        }
                        posts = updatedPosts
                        errorMessage = null
                    } catch (e: Exception) {
                        Log.e("FEED", "Error loading user details", e)
                        posts = rawPosts
                        errorMessage = "שגיאה בטעינת פרטי המשתמשים"
                    } finally {
                        isLoading = false
                    }
                }
            }

        onDispose { listener.remove() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            posts.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // הוספת לוגו במקום טקסט רגיל
                        Text(
                            text = "🐾",
                            fontSize = 48.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "ברוכים הבאים ל-PetPals!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("עדיין אין פוסטים בפיד")
                        Spacer(Modifier.height(16.dp))
                        Text("התחילו לשתף את הרגעים המיוחדים עם חיות המחמד שלכם!")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        FeedPostCard(
                            post = post,
                            currentUserId = currentUserId,
                            onLikeClick = { postId ->
                                coroutineScope.launch {
                                    toggleLike(postId, currentUserId)
                                }
                            },
                            onDeleteClick = { postId ->
                                coroutineScope.launch {
                                    deletePost(postId, post.userId)
                                }
                            },
                            onDeleteCommentClick = { postId, commentId ->
                                coroutineScope.launch {
                                    deleteComment(postId, commentId)
                                }
                            },
                            onLocationClick = { latLng ->
                                navController.navigate("${Screen.Map.route}?lat=${latLng.latitude}&lng=${latLng.longitude}")
                            }
                        )
                    }
                }
            }
        }

        // כפתור להוספת פוסט חדש עם עיצוב משופר
        FloatingActionButton(
            onClick = { navController.navigate(Screen.NewPost.route) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "פוסט חדש")
        }
    }
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    currentUserId: String?,
    onLikeClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onDeleteCommentClick: (String, String) -> Unit,
    onLocationClick: (LatLng) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newComment by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // מצבי אנימציה ללייק
    var showLikeAnimation by remember { mutableStateOf(false) }
    val likeAnimationScale by animateFloatAsState(
        targetValue = if (showLikeAnimation) 1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = {
            if (showLikeAnimation) {
                showLikeAnimation = false
            }
        }
    )

    // מאזין לתגובות בזמן אמת
    LaunchedEffect(post.postId) {
        db.collection("posts").document(post.postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("COMMENTS", "Error loading comments", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    comments = snapshot.documents.mapNotNull { doc ->
                        Comment(
                            commentId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L,
                            userName = doc.getString("userName") ?: ""
                        )
                    }
                }
            }
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.padding(16.dp)) {
            // פרטי משתמש
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.petImage.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(post.petImage),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // אייקון ברירת מחדל מהלוגו
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐾", fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        post.petName.ifEmpty { "Unknown Pet" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(formatTimestamp(post.timestamp), style = MaterialTheme.typography.bodySmall)

                    // מיקום - לחיצה עליו מעבירה למפה
                    if (post.locationLatLng != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onLocationClick(post.locationLatLng) }
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "מיקום",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = post.location.ifEmpty { "הצג במפה" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (currentUserId == post.userId) {
                    IconButton(onClick = { onDeleteClick(post.postId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "מחק פוסט")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // תמונת הפוסט עם אנימציית לייק
            if (post.imageUrl.isNotEmpty()) {
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(post.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        // Double tap ליייק
                                        showLikeAnimation = true
                                        onLikeClick(post.postId)
                                    }
                                )
                            },
                        contentScale = ContentScale.Crop
                    )

                    // אנימציית לייק
                    if (showLikeAnimation || likeAnimationScale > 0f) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "❤️",
                                fontSize = 80.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .scale(likeAnimationScale)
                                    .graphicsLayer(alpha = likeAnimationScale)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (post.text.isNotEmpty()) {
                Text(post.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
            }

            // כפתורי אינטראקציה
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isLiked = currentUserId in post.likedBy
                TextButton(
                    onClick = {
                        onLikeClick(post.postId)
                        if (!isLiked) {
                            showLikeAnimation = true
                        }
                    }
                ) {
                    Text(
                        text = "${if (isLiked) "❤️" else "🤍"} ${post.likes}",
                        color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // רשימת תגובות
            if (comments.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(comments) { comment ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${comment.userName}: ${comment.text}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUserId == comment.userId) {
                                IconButton(onClick = { onDeleteCommentClick(post.postId, comment.commentId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "מחק תגובה")
                                }
                            }
                        }
                    }
                }
            }

            // שדה להוספת תגובה
            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("הוסף תגובה") }
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = {
                coroutineScope.launch {
                    if (newComment.isNotBlank() && currentUserId != null) {
                        addComment(post.postId, currentUserId, newComment)
                        newComment = ""
                    }
                }
            }) {
                Text("פרסם תגובה")
            }
        }
    }
}

suspend fun addComment(postId: String, userId: String, text: String) {
    val db = FirebaseFirestore.getInstance()
    val userDoc = db.collection("users").document(userId).get().await()
    val userName = userDoc.getString("petName") ?: "Unknown"
    val commentData = mapOf(
        "userId" to userId,
        "userName" to userName,
        "text" to text,
        "timestamp" to Timestamp.now()
    )
    db.collection("posts").document(postId).collection("comments").add(commentData).await()
}

suspend fun deleteComment(postId: String, commentId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("posts").document(postId).collection("comments").document(commentId).delete().await()
}

suspend fun toggleLike(postId: String, userId: String?) {
    if (userId == null) return
    val db = FirebaseFirestore.getInstance()
    val postRef = db.collection("posts").document(postId)

    db.runTransaction { transaction ->
        val snapshot = transaction.get(postRef)
        val likes = snapshot.getLong("likes") ?: 0
        val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

        if (userId in likedBy) {
            likedBy.remove(userId)
            transaction.update(postRef, mapOf("likes" to likes - 1, "likedBy" to likedBy))
        } else {
            likedBy.add(userId)
            transaction.update(postRef, mapOf("likes" to likes + 1, "likedBy" to likedBy))
        }
    }.await()
}

suspend fun deletePost(postId: String, userId: String) {
    val db = FirebaseFirestore.getInstance()
    try {
        db.collection("posts").document(postId).delete().await()
        db.collection("users").document(userId).collection("myPosts").document(postId).delete().await()
    } catch (e: Exception) {
        Log.e("DELETE_POST", "Error deleting post: ${e.message}", e)
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "עכשיו"
        diff < 3_600_000 -> "${diff / 60_000} ד'"
        diff < 86_400_000 -> "${diff / 3_600_000} ש'"
        else -> java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}