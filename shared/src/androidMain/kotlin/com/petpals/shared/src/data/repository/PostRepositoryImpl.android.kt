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

actual class PostRepositoryImpl actual constructor(apiService: ApiService) :
    IPostRepository {
    override suspend fun getFeed(): AppResult<List<Post>> {
        TODO("Not yet implemented")
    }

    actual override suspend fun createPost(post: Post): Result<Post> {
        TODO("Not yet implemented")
    }

    actual open suspend fun createPostWithImage(
        description: String,
        imageUrl: String?,
        lat: Double?,
        lng: Double?
    ): Result<String> {
        TODO("Not yet implemented")
    }

    actual open suspend fun getPosts(
        limit: Int,
        offset: Int
    ): Result<List<Post>> {
        TODO("Not yet implemented")
    }

    actual open suspend fun getPostsByUser(userId: String): Result<List<Post>> {
        TODO("Not yet implemented")
    }

    actual open suspend fun getPostsByLocation(
        location: LatLng,
        radius: Double
    ): Result<List<Post>> {
        TODO("Not yet implemented")
    }

    actual open suspend fun getPostById(postId: String): Result<Post> {
        TODO("Not yet implemented")
    }

    actual override suspend fun likePost(
        postId: String,
        userId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual open suspend fun unlikePost(
        postId: String,
        userId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual open suspend fun toggleLike(
        postId: String,
        userId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual open suspend fun deletePost(postId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual open suspend fun addComment(comment: Comment): Result<Comment> {
        TODO("Not yet implemented")
    }

    actual open suspend fun addCommentToPost(
        postId: String,
        userId: String,
        text: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual open suspend fun getComments(postId: String): Result<List<Comment>> {
        TODO("Not yet implemented")
    }

    actual open suspend fun deleteComment(
        postId: String,
        commentId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual open suspend fun getNearbyPosts(
        currentLocation: LatLng,
        radiusKm: Double
    ): Result<List<MapPostMarker>> {
        TODO("Not yet implemented")
    }

    actual open fun observePosts(): Flow<Result<List<Post>>> {
        TODO("Not yet implemented")
    }

    actual open fun observePostsByUser(userId: String): Flow<Result<List<Post>>> {
        TODO("Not yet implemented")
    }

    actual open fun observeComments(postId: String): Flow<Result<List<Comment>>> {
        TODO("Not yet implemented")
    }

}