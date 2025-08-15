package com.petpals.shared.platform

class IOSPlatform: Platform {
    override val name: String = "iOS ${platform.UIKit.UIDevice.currentDevice.systemVersion}"
}

actual fun getPlatform(): Platform = IOSPlatform()
