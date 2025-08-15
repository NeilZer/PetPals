
package com.petpals.shared.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.*
import platform.dispatch.dispatch_get_main_queue
import platform.dispatch.dispatch_async

actual class NetworkManager {
    private val _isConnected = MutableStateFlow(true)
    actual val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.WIFI)
    actual val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_get_global_queue(QOS_CLASS_UTILITY, 0u)

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        nw_path_monitor_set_update_handler(monitor) { path ->
            dispatch_async(dispatch_get_main_queue()) {
                val status = nw_path_get_status(path)
                _isConnected.value = status == nw_path_status_satisfied
                updateConnectionType(path)
            }
        }
        nw_path_monitor_start(monitor, queue)
    }

    private fun updateConnectionType(path: nw_path_t) {
        when {
            nw_path_uses_interface_type(path, nw_interface_type_wifi) -> {
                _connectionType.value = ConnectionType.WIFI
            }
            nw_path_uses_interface_type(path, nw_interface_type_cellular) -> {
                _connectionType.value = ConnectionType.CELLULAR
            }
            nw_path_uses_interface_type(path, nw_interface_type_wired) -> {
                _connectionType.value = ConnectionType.ETHERNET
            }
            nw_path_get_status(path) == nw_path_status_satisfied -> {
                _connectionType.value = ConnectionType.OTHER
            }
            else -> {
                _connectionType.value = ConnectionType.NONE
            }
        }
    }

    actual fun stopMonitoring() {
        nw_path_monitor_cancel(monitor)
    }

    actual enum class ConnectionType {
        WIFI, CELLULAR, ETHERNET, OTHER, NONE
    }
}
