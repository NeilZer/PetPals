package com.petpals.shared.src.data.storage


import android.net.Uri
import com.google.firebase.storage.FirebaseStorage as FBStorage
import com.petpals.shared.src.core.Result
import kotlinx.coroutines.tasks.await

actual class FirebaseStorage {

    private val storage = FBStorage.getInstance()

    actual suspend fun uploadImage(
        image: Any,
        path: String,
        compressionQuality: Double
    ): Result<String> {
        return try {
            val imageUri = image as Uri
            val storageRef = storage.reference.child(path)

            // Upload the file
            val uploadTask = storageRef.putFile(imageUri).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Result.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun deleteImage(url: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(url)
            storageRef.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun getDownloadUrl(path: String): Result<String> {
        return try {
            val storageRef = storage.reference.child(path)
            val downloadUrl = storageRef.downloadUrl.await()
            Result.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
