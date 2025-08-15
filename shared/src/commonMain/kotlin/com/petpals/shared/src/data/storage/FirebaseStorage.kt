package com.petpals.shared.src.data.storage

import com.petpals.shared.src.core.Result

expect class FirebaseStorage() {
    suspend fun uploadImage(
        image: Any,
        path: String,
        compressionQuality: Double = 0.8
    ): Result<String>

    suspend fun deleteImage(url: String): Result<Unit>

    suspend fun getDownloadUrl(path: String): Result<String>
}
