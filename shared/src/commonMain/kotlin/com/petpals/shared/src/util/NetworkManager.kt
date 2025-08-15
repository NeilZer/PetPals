package com.petpals.shared.src.util

import kotlinx.coroutines.flow.StateFlow

expect class NetworkManager {
    val isConnected: StateFlow<Boolean>
    val connectionType: StateFlow<ConnectionType>
    fun stopMonitoring()
    enum class ConnectionType {
        WIFI, CELLULAR, ETHERNET, OTHER, NONE
    }

}