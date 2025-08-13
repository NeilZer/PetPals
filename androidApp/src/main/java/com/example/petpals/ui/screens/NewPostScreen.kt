package com.example.petpals.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

private const val TAG = "NEW_POST"

@Composable
fun NewPostScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }          // התמונה שתוצג ותועלה
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }     // קובץ ביניים לצילום
    var isUploading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // === גלריה (לא דורש הרשאות בזמן ריצה כי זה SAF) ===
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        errorMessage = null
    }

    // === מצלמה: צילום אל תוך Uri שנוצר ע"י FileProvider ===
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempCameraUri
            errorMessage = null
        }
    }

    // הרשאת מצלמה (נדרשת כדי לפתוח Camera)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            errorMessage = "אין הרשאת מצלמה"
        }
    }

    // הרשאת מיקום
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
        if (isGranted) {
            getCurrentLocation(fusedLocationClient) { loc -> location = loc }
        }
    }

    // בקשת הרשאת מיקום עם כניסה למסך
    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                locationPermissionGranted = true
                getCurrentLocation(fusedLocationClient) { loc -> location = loc }
            }
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // === UI ===
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("פוסט חדש", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it; errorMessage = null },
            label = { Text("מה חיית המחמד שלך עושה היום?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(Modifier.height(16.dp))

        // כפתורי צילום/גלריה
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val uri = createImageUri(context)
                        tempCameraUri = uri
                        takePictureLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("צלם תמונה")
            }

            Button(
                onClick = { galleryPicker.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                // נשתמש באייקון מצלמה/תמונה בלי לייבא filled.Image כדי לא להתנגש עם Image של Compose
                Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("בחר מהגלריה")
            }
        }

        Spacer(Modifier.height(16.dp))

        imageUri?.let { uri ->
            Card(Modifier.fillMaxWidth().height(220.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "מיקום",
                tint = if (location != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    location != null -> "מיקום נוסף לפוסט ✓"
                    locationPermissionGranted -> "מחפש מיקום..."
                    else -> "אין הרשאת מיקום"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (location != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!locationPermissionGranted) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }) { Text("אפשר הרשאת מיקום") }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (text.isNotBlank() && imageUri != null && !isUploading) {
                    scope.launch {
                        isUploading = true
                        errorMessage = null
                        try {
                            uploadPostFixed(text, imageUri!!, location)
                            navController.popBackStack()
                        } catch (e: Exception) {
                            errorMessage = "שגיאה בפרסום הפוסט: ${e.message}"
                            Log.e(TAG, "Error uploading post", e)
                        } finally {
                            isUploading = false
                        }
                    }
                } else {
                    errorMessage = when {
                        text.isBlank() -> "יש להוסיף טקסט לפוסט"
                        imageUri == null -> "יש לבחור או לצלם תמונה"
                        else -> "שגיאה לא ידועה"
                    }
                }
            },
            enabled = !isUploading && text.isNotBlank() && imageUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isUploading) "מפרסם..." else "פרסם")
        }

        errorMessage?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("camera_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

private fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { onLocationReceived(it) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting location", e)
                onLocationReceived(null)
            }
    } catch (e: SecurityException) {
        Log.e(TAG, "Location permission not granted", e)
        onLocationReceived(null)
    }
}

/**
 * ✅ העלאה לנתיב שמתאים לכללי ה-Storage:
 *    postImages/<uid>/<postId>.jpg
 * ✅ כתיבה ל-top-level: /posts/<postId> (+ העתק ל-users/<uid>/myPosts)
 * ✅ שימוש ב-serverTimestamp כדי למנוע הפרשי שעון בין מכשירים
 */
private suspend fun uploadPostFixed(text: String, imageUri: Uri, location: Location?) {
    val user = FirebaseAuth.getInstance().currentUser ?: throw Exception("משתמש לא מחובר")
    val uid = user.uid
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // נייצר מזהה פוסט מראש כדי להשתמש בו גם בשם הקובץ
    val postRef = db.collection("posts").document()
    val postId = postRef.id

    // נתיב שעובר את חוקי ה-Storage
    val imageRef = storage.reference.child("postImages/$uid/$postId.jpg")

    // העלאת התמונה וקבלת URL
    imageRef.putFile(imageUri).await()
    val downloadUrl = imageRef.downloadUrl.await().toString()

    // הנתונים לפוסט
    val data = mutableMapOf<String, Any?>(
        "postId" to postId,
        "userId" to uid,
        "text" to text.trim(),
        "imageUrl" to downloadUrl,
        "likes" to 0,
        "timestamp" to FieldValue.serverTimestamp()
    )
    location?.let { data["location"] = GeoPoint(it.latitude, it.longitude) }

    // כתיבה לפיד הראשי
    postRef.set(data.filterValues { it != null }).await()

    // קישור לפוסטים של המשתמש (למסך הפרופיל)
    db.collection("users").document(uid)
        .collection("myPosts").document(postId)
        .set(data.filterValues { it != null }).await()

    Log.d(TAG, "Post uploaded: $postId")
}
