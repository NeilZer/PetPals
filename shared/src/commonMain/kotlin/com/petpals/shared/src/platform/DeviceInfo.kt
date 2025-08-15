
package com.petpals.shared.src.platform

expect class DeviceInfo() {
    val platform: String
    val version: String
    val deviceModel: String
    val isEmulator: Boolean
}
