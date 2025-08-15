package com.petpals.shared.src.util

import kotlinx.coroutines.flow.StateFlow

actual class NetworkManager {
    actual val isConnected: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    actual val connectionType: StateFlow<ConnectionType>
        get() = TODO("Not yet implemented")

    actual fun stopMonitoring() {
    }

    actual enum class ConnectionType {
        WIFI, CELLULAR, ETHERNET, OTHER, NONE
    }

}