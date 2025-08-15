package com.petpals.shared.src.data.di

import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.data.repository.PostRepositoryImpl
import com.petpals.shared.src.domain.repository.IPostRepository
import com.petpals.shared.src.domain.repository.IUserRepository
import com.petpals.shared.src.domain.repository.IAuthRepository
import com.petpals.shared.src.domain.repository.IStatisticsRepository
import com.petpals.shared.src.platform.NetworkClient
import com.petpals.shared.src.data.repository.AuthRepositoryImpl
import com.petpals.shared.src.data.repository.StatisticsRepositoryImpl
import com.petpals.shared.src.data.repository.UserRepositoryImpl

object SharedDI {
    private val networkClient by lazy { NetworkClient() }
    private val httpClient by lazy { networkClient.getHttpClient() }
    private val apiService by lazy { ApiService(httpClient) }

    // iOS-specific initialization
    fun initialize() {
        // Initialize any iOS-specific dependencies here
        println("SharedDI initialized for iOS")
    }

    // Repository providers
    fun providePostsRepository(): IPostRepository = PostRepositoryImpl(apiService)
    fun provideUserRepository(): IUserRepository = UserRepositoryImpl(apiService)
    fun provideAuthRepository(): IAuthRepository = AuthRepositoryImpl()

    // Legacy support
    val postRepository: IPostRepository by lazy { PostRepositoryImpl(apiService) }
    val userRepository: IUserRepository by lazy { UserRepositoryImpl(apiService) }
    val authRepository: IAuthRepository by lazy { AuthRepositoryImpl() }
}
