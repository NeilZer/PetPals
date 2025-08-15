
package com.petpals.shared.util

import platform.Foundation.NSCache
import platform.UIKit.UIImage
import platform.Foundation.NSString
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImageCache {
    private val memoryCache = NSCache()

    actual companion object {
        actual val shared = ImageCache()
    }

    init {
        memoryCache.countLimit = 150
        memoryCache.totalCostLimit = (60 * 1024 * 1024).toLong() // 60MB
    }

    actual fun getImage(key: String): Any? {
        return memoryCache.objectForKey(key as NSString)
    }

    actual fun setImage(image: Any, key: String) {
        if (image is UIImage) {
            memoryCache.setObject(image, forKey = key as NSString)
        }
    }

    actual fun removeImage(key: String) {
        memoryCache.removeObjectForKey(key as NSString)
    }

    actual fun clearCache() {
        memoryCache.removeAllObjects()
    }

    actual suspend fun loadImageFromDisk(key: String): Any? {
        return withContext(Dispatchers.Default) {
            // Implementation for loading from disk on iOS
            // This would involve NSFileManager and document directory
            null
        }
    }

    actual suspend fun saveImageToDisk(image: Any, key: String) {
        withContext(Dispatchers.Default) {
            // Implementation for saving to disk on iOS
            // This would involve NSFileManager and document directory
        }
    }
}
