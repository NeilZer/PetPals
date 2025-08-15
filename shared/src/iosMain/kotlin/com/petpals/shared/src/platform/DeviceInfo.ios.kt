
package com.petpals.shared.src.platform

actual class DeviceInfo {
    actual val platform: String = "iOS"
    actual val version: String = UIDevice.currentDevice.systemVersion
    actual val deviceModel: String = UIDevice.currentDevice.model
    actual val isEmulator: Boolean = UIDevice.currentDevice.model.contains("Simulator")

    actual fun getDeviceId(): String {
        return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_ios_device"
    }

    actual fun getAppVersion(): String {
        val bundle = NSBundle.mainBundle
        val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
        val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "1"
        return "$version ($build)"
    }

    actual fun getBatteryLevel(): Float {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        val level = UIDevice.currentDevice.batteryLevel
        return if (level < 0) 0.5f else level
    }

    actual fun isNetworkAvailable(): Boolean {
        // Simple check - in real implementation you'd use Reachability
        return true
    }

    actual fun getScreenInfo(): ScreenInfo {
        val screen = UIScreen.mainScreen
        val bounds = screen.bounds
        val scale = screen.scale

        return ScreenInfo(
            width = (bounds.size.width * scale).toInt(),
            height = (bounds.size.height * scale).toInt(),
            density = scale.toFloat()
        )
    }

    actual fun isMobile(): Boolean = true // iOS is always mobile
}