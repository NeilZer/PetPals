
package com.petpals.shared.src.util


actual class AppDelegate : NSObject(), UIApplicationDelegateProtocol {

    fun applicationDidFinishLaunching(application: UIApplication): Boolean {
        // Request notification permissions
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        ) { granted, error ->
            if (granted) {
                println("Notification permission granted")
                application.registerForRemoteNotifications()
            } else {
                error?.let { println("Notification permission error: ${it.localizedDescription}") }
            }
        }

        return true
    }

    fun applicationDidRegisterForRemoteNotifications(
        application: UIApplication,
        deviceToken: NSData
    ) {
        // Convert device token to string
        val tokenParts = mutableListOf<String>()
        val bytes = deviceToken.bytes
        for (i in 0 until deviceToken.length.toInt()) {
            val byte = (bytes?.reinterpret<ByteVar>()?.get(i) ?: 0).toUByte()
            tokenParts.add(String.format("%02x", byte))
        }
        val token = tokenParts.joinToString("")
        println("Device Token: $token")
    }

    fun applicationDidFailToRegisterForRemoteNotifications(
        application: UIApplication,
        error: NSError
    ) {
        println("Failed to register for remote notifications: ${error.localizedDescription}")
    }
}
