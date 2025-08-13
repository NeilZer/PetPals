package com.example.petpals.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import com.petpals.shared.src.util.formatTimestamp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.petpals.shared.src.model.Comment
import com.petpals.shared.src.util.anyToEpochMillis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavHostController,
    postId: String
) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val coroutineScope = rememberCoroutineScope()

    var post by remember { mutableStateOf<FeedPost?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newComment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // טעינת הפוסט
    LaunchedEffect(postId) {
        try {
            val postDoc = db.collection("posts").document(postId).get().await()
            if (postDoc.exists()) {
                val geoPoint = postDoc.getGeoPoint("location")
                val locationLatLng = geoPoint?.let {
                    com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                }

                val tempPost = FeedPost(
                    postId = postDoc.id,
                    userId = postDoc.getString("userId") ?: "",
                    text = postDoc.getString("text") ?: "",
                    imageUrl = postDoc.getString("imageUrl") ?: "",
                    likes = postDoc.getLong("likes")?.toInt() ?: 0,
                    timestamp = anyToEpochMillis(postDoc.get("timestamp")),
                    likedBy = postDoc.get("likedBy") as? List<String> ?: emptyList(),
                    location = postDoc.getString("locationString") ?: "",
                    locationLatLng = locationLatLng
                )

                // טעינת פרטי המשתמש
                val userDoc = db.collection("users").document(tempPost.userId).get().await()
                post = tempPost.copy(
                    petName = userDoc.getString("petName") ?: "Unknown Pet",
                    petImage = userDoc.getString("petImage") ?: ""
                )
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("POST_DETAIL", "Error loading post", e)
            errorMessage = "שגיאה בטעינת הפוסט"
            isLoading = false
        }
    }

    // מאזין לתגובות בזמן אמת
    LaunchedEffect(postId) {
        db.collection("posts").document(postId)
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
                            timestamp = anyToEpochMillis(doc.get("timestamp")),
                            userName = doc.getString("userName") ?: ""
                        )
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("פוסט") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "חזור")
                    }
                }
            )

        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                post == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("פוסט לא נמצא")
                            errorMessage?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            PostDetailCard(
                                post = post!!,
                                currentUserId = currentUserId,
                                onLikeClick = { postId ->
                                    coroutineScope.launch {
                                        toggleLike(postId, currentUserId)
                                        // רענן את הפוסט אחרי לייק
                                        val updatedDoc = db.collection("posts").document(postId).get().await()
                                        post = post!!.copy(
                                            likes = updatedDoc.getLong("likes")?.toInt() ?: 0,
                                            likedBy = updatedDoc.get("likedBy") as? List<String> ?: emptyList()
                                        )
                                    }
                                },
                                onLocationClick = { latLng ->
                                    navController.navigate("${com.example.petpals.ui.Screen.Map.route}?lat=${latLng.latitude}&lng=${latLng.longitude}")
                                }
                            )
                        }

                        item {
                            Divider()
                            Text(
                                "תגובות (${comments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(comments) { comment ->
                            CommentItem(
                                comment = comment,
                                currentUserId = currentUserId,
                                onDeleteClick = { commentId ->
                                    coroutineScope.launch {
                                        deleteComment(postId, commentId)
                                    }
                                }
                            )
                        }

                        item {
                            // שדה להוספת תגובה
                            Column {
                                OutlinedTextField(
                                    value = newComment,
                                    onValueChange = { newComment = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("הוסף תגובה") },
                                    minLines = 2
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (newComment.isNotBlank() && currentUserId != null) {
                                                addComment(postId, currentUserId, newComment)
                                                newComment = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("פרסם תגובה")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostDetailCard(
    post: FeedPost,
    currentUserId: String?,
    onLikeClick: (String) -> Unit,
    onLocationClick: (com.google.android.gms.maps.model.LatLng) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // פרטי משתמש
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.petImage.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(post.petImage),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.petName, style = MaterialTheme.typography.titleLarge)
                    Text(formatTimestamp(post.timestamp), style = MaterialTheme.typography.bodyMedium)

                    // מיקום
                    if (post.locationLatLng != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 4.dp)
                        ) {
                            TextButton(
                                onClick = { onLocationClick(post.locationLatLng) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "מיקום",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = post.location.ifEmpty { "הצג במפה" },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // תמונת הפוסט
            if (post.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
            }

            // טקסט הפוסט
            if (post.text.isNotEmpty()) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
            }

            // כפתורי אינטראקציה
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isLiked = currentUserId in post.likedBy
                TextButton(
                    onClick = { onLikeClick(post.postId) }
                ) {
                    Text(
                        text = "${if (isLiked) "❤️" else "🤍"} ${post.likes}",
                        color = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onDeleteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(comment.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentUserId == comment.userId) {
                IconButton(
                    onClick = { onDeleteClick(comment.commentId) }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "מחק תגובה",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}