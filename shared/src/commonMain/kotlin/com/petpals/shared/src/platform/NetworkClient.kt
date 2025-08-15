
package com.petpals.shared.src.platform

import io.ktor.client.HttpClient

expect class NetworkClient() {
    fun httpClient(): HttpClient
    suspend fun isNetworkAvailable(): Boolean
}
