package com.petpals.shared.src.util

actual class ImageCache {
    actual companion object {
        actual val shared: ImageCache
            get() = TODO("Not yet implemented")
    }

    actual fun getImage(key: String): Any? {
        TODO("Not yet implemented")
    }

    actual fun setImage(image: Any, key: String) {
    }

    actual fun removeImage(key: String) {
    }

    actual fun clearCache() {
    }

    actual suspend fun loadImageFromDisk(key: String): Any? {
        TODO("Not yet implemented")
    }

    actual suspend fun saveImageToDisk(image: Any, key: String) {
    }

}