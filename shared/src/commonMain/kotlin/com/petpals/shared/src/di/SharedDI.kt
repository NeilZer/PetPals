
package com.petpals.shared.src.di

import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.data.repository.PostRepositoryImpl
import com.petpals.shared.src.data.repository.UserRepositoryImpl
import com.petpals.shared.src.data.repository.AuthRepositoryImpl
import com.petpals.shared.src.data.repository.StatisticsRepositoryImpl
import com.petpals.shared.src.domain.repository.IPostRepository
import com.petpals.shared.src.domain.repository.IUserRepository
import com.petpals.shared.src.domain.repository.IAuthRepository
import com.petpals.shared.src.domain.repository.IStatisticsRepository
import com.petpals.shared.src.platform.NetworkClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object SharedDI {

    private val networkClient by lazy { NetworkClient() }

    private val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    val authRepository: IAuthRepository by lazy { AuthRepositoryImpl() }
}
