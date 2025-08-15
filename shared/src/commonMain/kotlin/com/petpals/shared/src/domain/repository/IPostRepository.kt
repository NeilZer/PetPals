
package com.petpals.shared.src.domain.repository

import com.petpals.shared.src.model.Post
import com.petpals.shared.src.core.AppResult



interface IPostRepository {
    suspend fun getFeed(): AppResult<List<Post>>
    suspend fun createPost(post: Post): AppResult<Post>
    suspend fun likePost(postId: String, userId: String): AppResult<Unit>
}
