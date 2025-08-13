package com.petpals.shared.src.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

class PetApi {
    private val client = HttpClient { install(ContentNegotiation) { json() } }

    @Serializable data class Breed(val name: String)

    suspend fun breeds(): List<Breed> {
        // דוגמה – החליפי ל-API שבא לך
        return client.get("https://catfact.ninja/breeds").body<Map<String, Any>>()["data"] as? List<Breed> ?: emptyList()
    }
}
