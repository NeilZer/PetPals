
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.core.AppResult
import com.petpals.shared.src.core.Result
import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.domain.repository.IPostRepository
import com.petpals.shared.src.model.Comment
import com.petpals.shared.src.model.MapPostMarker
import com.petpals.shared.src.model.Post
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow


expect class PostRepositoryImpl(apiService: ApiService) :
    IPostRepository {
    override suspend fun createPost(post: Post): Result<Post>
    open suspend fun createPostWithImage(
        description: String,
        imageUrl: String?,
        lat: Double?,
        lng: Double?
    ): Result<String>

    open suspend fun getPosts(
        limit: Int,
        offset: Int
    ): Result<List<Post>>

    open suspend fun getPostsByUser(userId: String): Result<List<Post>>
    open suspend fun getPostsByLocation(
        location: LatLng,
        radius: Double
    ): Result<List<Post>>

    open suspend fun getPostById(postId: String): Result<Post>
    override suspend fun likePost(
        postId: String,
        userId: String
    ): Result<Unit>

    open suspend fun unlikePost(
        postId: String,
        userId: String
    ): Result<Unit>

    open suspend fun toggleLike(
        postId: String,
        userId: String
    ): Result<Unit>

    open suspend fun deletePost(postId: String): Result<Unit>
    open suspend fun addComment(comment: Comment): Result<Comment>
    open suspend fun addCommentToPost(
        postId: String,
        userId: String,
        text: String
    ): Result<Unit>

    open suspend fun getComments(postId: String): Result<List<Comment>>
    open suspend fun deleteComment(
        postId: String,
        commentId: String
    ): Result<Unit>

    open suspend fun getNearbyPosts(
        currentLocation: LatLng,
        radiusKm: Double
    ): Result<List<MapPostMarker>>

    open fun observePosts(): Flow<Result<List<Post>>>
    open fun observePostsByUser(userId: String): Flow<Result<List<Post>>>
    open fun observeComments(postId: String): Flow<Result<List<Comment>>>

}