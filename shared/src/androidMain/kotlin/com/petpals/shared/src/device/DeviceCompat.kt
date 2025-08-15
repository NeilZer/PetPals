
package com.petpals.shared.src.device

import android.os.Build

object device {
    val platform: String get() = "Android"
    val version: String get() = Build.VERSION.RELEASE ?: "unknown"
    val model: String get() = "${Build.MANUFACTURER} ${Build.MODEL}"
    val sdkInt: Int get() = Build.VERSION.SDK_INT
    val isEmulator: Boolean get() =
        (Build.FINGERPRINT?.contains("generic") == true) ||
        (Build.MODEL?.contains("Emulator") == true)
}
