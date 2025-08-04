package com.example.petpals.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // ✅ טעינת הפרופיל הנוכחי
    LaunchedEffect(currentUserId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(currentUserId).get().await()
            petName = doc.getString("petName") ?: ""
            petAge = doc.getLong("petAge")?.toString() ?: ""
            petBreed = doc.getString("petBreed") ?: ""
            existingImageUrl = doc.getString("petImage") ?: ""
        } catch (e: Exception) {
            Log.e("PROFILE", "Failed to load profile for edit", e)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> petImageUri = uri }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Edit Pet Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = petName, onValueChange = { petName = it },
            label = { Text("Pet Name") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = petAge, onValueChange = { petAge = it },
            label = { Text("Pet Age") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = petBreed, onValueChange = { petBreed = it },
            label = { Text("Pet Breed") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Button(onClick = { imagePickerLauncher.launch("image/*") }) { Text("Choose Pet Image") }
        Spacer(Modifier.height(12.dp))

        when {
            petImageUri != null -> Image(
                painter = rememberAsyncImagePainter(petImageUri),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            existingImageUrl.isNotEmpty() -> Image(
                painter = rememberAsyncImagePainter(existingImageUrl),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isLoading && petName.isNotEmpty()) {
                    coroutineScope.launch {
                        isLoading = true
                        saveUserProfile(
                            userId = currentUserId,
                            name = petName,
                            age = petAge.toIntOrNull() ?: 0,
                            breed = petBreed,
                            imageUri = petImageUri ?: existingImageUrl.toUriOrNull()
                        ) { success ->
                            isLoading = false
                            if (success) {
                                Log.d("PROFILE", "Profile saved successfully!")
                                onProfileUpdated() // ✅ חוזר אוטומטית לפרופיל ומרענן אותו
                            } else {
                                Log.e("PROFILE", "Failed to save profile")
                            }
                        }
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Saving..." else "Save Profile")
        }
    }
}

// ✅ פונקציית עזר להמרת URL ל־Uri
fun String.toUriOrNull(): Uri? = try { Uri.parse(this) } catch (_: Exception) { null }

// ✅ שמירת פרופיל עם לוגים ברורים ו־merge()
fun saveUserProfile(
    userId: String,
    name: String,
    age: Int,
    breed: String,
    imageUri: Uri?,
    onComplete: (Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val userDoc = db.collection("users").document(userId)

    // 🔹 אם יש תמונה חדשה להעלות
    if (imageUri != null && imageUri.scheme?.startsWith("http") == false) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId.jpg")
        Log.d("PROFILE", "Uploading image to: ${storageRef.path} with uri: $imageUri")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d("PROFILE", "Upload success!")
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d("PROFILE", "Got download URL: $downloadUrl")
                    val profileData = mapOf(
                        "petName" to name,
                        "petAge" to age,
                        "petBreed" to breed,
                        "petImage" to downloadUrl.toString()
                    )
                    userDoc.set(profileData, SetOptions.merge())
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { e ->
                            Log.e("PROFILE", "Firestore save failed", e)
                            onComplete(false)
                        }
                }.addOnFailureListener { e ->
                    Log.e("PROFILE", "Failed to get download URL", e)
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PROFILE", "Upload failed!", e)
                onComplete(false)
            }
    } else {
        // 🔹 שמירה בלי העלאת תמונה חדשה
        val profileData = mapOf(
            "petName" to name,
            "petAge" to age,
            "petBreed" to breed,
            "petImage" to (imageUri?.toString() ?: "")
        )
        Log.d("PROFILE", "Saving profile without new image: $profileData")

        userDoc.set(profileData, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("PROFILE", "Firestore save failed", e)
                onComplete(false)
            }
    }
}
