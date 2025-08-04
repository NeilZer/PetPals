package com.example.petpals.ui.screens

import android.Manifest
import android.net.Uri
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

@Composable
fun NewPostScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }

    // בחירת תמונה מהגלריה
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // בקשת הרשאות למיקום
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    LaunchedEffect(Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                location = loc
            }
        } catch (_: SecurityException) { }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("What's on your pet's mind?") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Pick an Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (text.isNotEmpty() && imageUri != null && !isUploading) {
                    coroutineScope.launch {
                        isUploading = true
                        uploadPost(text, imageUri!!, location, {
                            isUploading = false
                            navController.popBackStack() // חזרה לפיד
                        }, {
                            isUploading = false
                        })
                    }
                }
            },
            enabled = !isUploading
        ) {
            Text(if (isUploading) "Uploading..." else "Post")
        }
    }
}

// פונקציה להעלאת פוסט ל-Firebase
fun uploadPost(
    text: String,
    imageUri: Uri,
    location: Location?,
    onComplete: () -> Unit,
    onError: (Exception) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val userId = user.uid
    val storageRef = FirebaseStorage.getInstance().reference.child("posts/${System.currentTimeMillis()}.jpg")
    val db = FirebaseFirestore.getInstance()

    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val post = hashMapOf(
                    "userId" to userId,
                    "userName" to (user.displayName ?: "Pet Lover"),
                    "text" to text,
                    "imageUrl" to downloadUrl.toString(),
                    "likes" to 0,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "location" to location?.let { GeoPoint(it.latitude, it.longitude) }
                )

                db.collection("posts").add(post)
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { e -> onError(e) }
            }.addOnFailureListener { e -> onError(e) }
        }
        .addOnFailureListener { e -> onError(e) }
}
