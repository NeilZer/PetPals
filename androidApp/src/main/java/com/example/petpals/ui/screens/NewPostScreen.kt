package com.example.petpals.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun NewPostScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        errorMessage = null
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
        if (isGranted) {
            getCurrentLocation(fusedLocationClient) { loc ->
                location = loc
            }
        }
    }

    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            PackageManager.PERMISSION_GRANTED -> {
                locationPermissionGranted = true
                getCurrentLocation(fusedLocationClient) { loc ->
                    location = loc
                }
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("פוסט חדש", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                errorMessage = null
            },
            label = { Text("מה חיית המחמד שלך עושה היום?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("בחר תמונה")
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageUri?.let { uri ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "מיקום",
                tint = if (location != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    location != null -> "מיקום נוסף לפוסט ✓"
                    locationPermissionGranted -> "מחפש מיקום..."
                    else -> "אין הרשאת מיקום"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (location != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!locationPermissionGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }) {
                Text("אפשר הרשאת מיקום")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (text.isNotEmpty() && imageUri != null && !isUploading) {
                    coroutineScope.launch {
                        isUploading = true
                        errorMessage = null
                        try {
                            uploadPost(text, imageUri!!, location)
                            navController.popBackStack()
                        } catch (e: Exception) {
                            errorMessage = "שגיאה בפרסום הפוסט: ${e.message}"
                            Log.e("NEW_POST", "Error uploading post", e)
                        } finally {
                            isUploading = false
                        }
                    }
                } else {
                    errorMessage = when {
                        text.isEmpty() -> "יש להוסיף טקסט לפוסט"
                        imageUri == null -> "יש לבחור תמונה"
                        else -> "שגיאה לא ידועה"
                    }
                }
            },
            enabled = !isUploading && text.isNotEmpty() && imageUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isUploading) "מפרסם..." else "פרסם")
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            onLocationReceived(location)
        }.addOnFailureListener { exception ->
            Log.e("NEW_POST", "Error getting location", exception)
            onLocationReceived(null)
        }
    } catch (e: SecurityException) {
        Log.e("NEW_POST", "Location permission not granted", e)
        onLocationReceived(null)
    }
}

// ✅ העלאת פוסט מעודכנת: שומר גם ב-posts וגם ב-users/{uid}/myPosts
suspend fun uploadPost(text: String, imageUri: Uri, location: Location?) {
    val user = FirebaseAuth.getInstance().currentUser ?: throw Exception("משתמש לא מחובר")
    val userId = user.uid
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    try {
        val imageRef = storage.reference.child("posts/${userId}_${System.currentTimeMillis()}.jpg")
        imageRef.putFile(imageUri).await()
        val downloadUrl = imageRef.downloadUrl.await()

        val postData = hashMapOf(
            "userId" to userId,
            "text" to text,
            "imageUrl" to downloadUrl.toString(),
            "likes" to 0,
            "timestamp" to Timestamp.now(),
            "location" to location?.let { GeoPoint(it.latitude, it.longitude) }
        )

        val postRef = db.collection("posts").add(postData).await()

        db.collection("users").document(userId)
            .collection("myPosts")
            .document(postRef.id)
            .set(postData)
            .await()

        Log.d("NEW_POST", "Post uploaded and linked to user profile")

    } catch (e: Exception) {
        Log.e("NEW_POST", "Error uploading post", e)
        throw e
    }
}
