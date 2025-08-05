package com.example.petpals.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EditProfileScreen(
    onProfileUpdated: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId == null) {
        onProfileUpdated()
        return
    }

    val coroutineScope = rememberCoroutineScope()
    var petName by remember { mutableStateOf("") }
    var petAge by remember { mutableStateOf("") }
    var petBreed by remember { mutableStateOf("") }
    var petImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // טעינת הפרופיל הנוכחי
    LaunchedEffect(currentUserId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(currentUserId).get().await()

            if (doc.exists()) {
                petName = doc.getString("petName") ?: ""
                petAge = doc.getLong("petAge")?.toString() ?: ""
                petBreed = doc.getString("petBreed") ?: ""
                existingImageUrl = doc.getString("petImage") ?: ""
            }

            isLoadingProfile = false
        } catch (e: Exception) {
            Log.e("EDIT_PROFILE", "Failed to load profile", e)
            errorMessage = "שגיאה בטעינת הפרופיל"
            isLoadingProfile = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        petImageUri = uri
        errorMessage = null
    }

    if (isLoadingProfile) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "עריכת פרופיל חיית המחמד",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // תצוגת תמונה
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                petImageUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(petImageUri),
                        contentDescription = "תמונת חיית המחמד",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                existingImageUrl.isNotEmpty() -> {
                    Image(
                        painter = rememberAsyncImagePainter(existingImageUrl),
                        contentDescription = "תמונת חיית המחמד",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Card(
                        modifier = Modifier.size(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("אין תמונה")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            enabled = !isLoading
        ) {
            Text("בחר תמונה")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = petName,
            onValueChange = {
                petName = it
                errorMessage = null
            },
            label = { Text("שם חיית המחמד") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = petAge,
            onValueChange = { input ->
                // רק מספרים
                if (input.isEmpty() || input.all { it.isDigit() }) {
                    petAge = input
                    errorMessage = null
                }
            },
            label = { Text("גיל") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = petBreed,
            onValueChange = {
                petBreed = it
                errorMessage = null
            },
            label = { Text("גזע") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (petName.isNotEmpty()) {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null

                        try {
                            saveUserProfile(
                                userId = currentUserId,
                                name = petName,
                                age = petAge.toIntOrNull() ?: 0,
                                breed = petBreed,
                                imageUri = petImageUri,
                                existingImageUrl = existingImageUrl
                            )
                            onProfileUpdated()
                        } catch (e: Exception) {
                            errorMessage = "שגיאה בשמירת הפרופיל: ${e.message}"
                            Log.e("EDIT_PROFILE", "Failed to save profile", e)
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    errorMessage = "יש להזין שם לחיית המחמד"
                }
            },
            enabled = !isLoading && petName.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "שומר..." else "שמור פרופיל")
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
suspend fun saveUserProfile(
    userId: String,
    name: String,
    age: Int,
    breed: String,
    imageUri: Uri?,
    existingImageUrl: String
) {
    val db = FirebaseFirestore.getInstance()
    val userDoc = db.collection("users").document(userId)

    try {
        val finalImageUrl = if (imageUri != null) {
            Log.d("PROFILE", "Uploading new image...")
            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("profileImages/$userId/${System.currentTimeMillis()}.jpg")

            // העלאת התמונה
            storageRef.putFile(imageUri).await()

            // קבלת URL להורדה
            storageRef.downloadUrl.await().toString()
        } else if (existingImageUrl.isNotEmpty()) {
            existingImageUrl
        } else {
            // ברירת מחדל – תמונת placeholder ציבורית
            "https://firebasestorage.googleapis.com/v0/b/<your-bucket>/o/default_pet.png?alt=media"
        }

        // שמירת הנתונים ב-Firestore
        val profileData = mapOf(
            "petName" to name,
            "petAge" to age,
            "petBreed" to breed,
            "petImage" to finalImageUrl,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        userDoc.set(profileData, SetOptions.merge()).await()
        Log.d("PROFILE", "Profile saved successfully")

    } catch (e: Exception) {
        Log.e("PROFILE", "Error saving profile", e)
        throw e
    }
}
