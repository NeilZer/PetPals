package com.petpals.shared.src.util

expect class ImageCache {
    companion object {
        val shared: ImageCache
    }

    fun getImage(key: String): Any?
    fun setImage(image: Any, key: String)
    fun removeImage(key: String)
    fun clearCache()
    suspend fun loadImageFromDisk(key: String): Any?
    suspend fun saveImageToDisk(image: Any, key: String)

}