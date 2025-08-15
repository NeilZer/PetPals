
package com.petpals.shared.src.di

import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.data.repository.PostRepositoryImpl
import com.petpals.shared.src.data.repository.UserRepositoryImpl
import com.petpals.shared.src.domain.repository.IPostRepository
import com.petpals.shared.src.domain.repository.IUserRepository

object SharedDI {
    private val api by lazy { ApiService() }
    val userRepository: IUserRepository by lazy { UserRepositoryImpl(api) }
    val postRepository: IPostRepository by lazy { PostRepositoryImpl(api) }
}
