
package com.petpals.shared.src.platform

import android.os.Build

actual class DeviceInfo actual constructor() {
    actual val platform: String = "Android"
    actual val version: String = Build.VERSION.RELEASE ?: "unknown"
    actual val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    actual val isEmulator: Boolean = Build.FINGERPRINT?.contains("generic") == true || Build.MODEL?.contains("Emulator") == true
}
