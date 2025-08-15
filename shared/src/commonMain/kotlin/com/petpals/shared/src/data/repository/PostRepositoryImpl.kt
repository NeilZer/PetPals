
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.core.AppResult
import com.petpals.shared.src.core.Result
import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.domain.repository.IPostRepository
import com.petpals.shared.src.model.Post

class PostRepositoryImpl(private val api: ApiService = ApiService()) : IPostRepository {
    override suspend fun getFeed(): AppResult<List<Post>> = runCatching { api.getFeed() }
        .fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(it) })

    override suspend fun createPost(post: Post): AppResult<Post> = runCatching { api.createPost(post) }
        .fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(it) })

    override suspend fun likePost(postId: String, userId: String): AppResult<Unit> = runCatching { api.likePost(postId, userId) }
        .fold(onSuccess = { Result.Success(Unit) }, onFailure = { Result.Error(it) })
}
