
package com.petpals.shared.data.repository

import com.petpals.shared.data.api.ApiService
import com.petpals.shared.domain.repository.IPostRepository
import com.petpals.shared.model.Post
import com.petpals.shared.model.Comment
import com.petpals.shared.model.MapPostMarker
import com.petpals.shared.util.LatLng
import com.petpals.shared.core.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class PostRepositoryImpl actual constructor(
    private val apiService: ApiService
) : IPostRepository {

    actual override suspend fun createPost(post: Post): Result<Post> {
        return try {
            val createdPost = apiService.createPost(post)
            Result.Success(createdPost)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun createPostWithImage(
        description: String,
        imageUrl: String?,
        lat: Double?,
        lng: Double?
    ): Result<String> {
        return try {
            val postId = apiService.generatePostId()
            val post = Post(
                postId = postId,
                description = description.trim(),
                imageUrl = imageUrl.orEmpty(),
                timestamp = com.petpals.shared.util.Time.getCurrentTimestamp(),
                lat = lat,
                lng = lng
            )
            apiService.createPost(post)
            Result.Success(postId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getPosts(limit: Int, offset: Int): Result<List<Post>> {
        return try {
            val posts = apiService.getPosts()
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getPostsByUser(userId: String): Result<List<Post>> {
        return try {
            val posts = apiService.getPosts().filter { it.userId == userId }
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getPostsByLocation(location: LatLng, radius: Double): Result<List<Post>> {
        return try {
            val posts = apiService.getPosts().filter { post ->
                post.locationLatLng?.let { postLocation ->
                    postLocation.distanceTo(location) <= radius
                } ?: false
            }
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getPostById(postId: String): Result<Post> {
        return try {
            val post = apiService.getPostById(postId)
            Result.Success(post)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            apiService.likePost(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            apiService.unlikePost(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun toggleLike(postId: String, userId: String): Result<Unit> {
        return try {
            apiService.toggleLike(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            apiService.deletePost(postId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun addComment(comment: Comment): Result<Comment> {
        return try {
            val addedComment = apiService.addComment(comment)
            Result.Success(addedComment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun addCommentToPost(postId: String, userId: String, text: String): Result<Unit> {
        return try {
            apiService.addCommentToPost(postId, userId, text)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val comments = apiService.getComments(postId)
            Result.Success(comments)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            apiService.deleteComment(postId, commentId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override suspend fun getNearbyPosts(currentLocation: LatLng, radiusKm: Double): Result<List<MapPostMarker>> {
        return try {
            val posts = apiService.getNearbyPosts(currentLocation, radiusKm)
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual override fun observePosts(): Flow<Result<List<Post>>> {
        return kotlinx.coroutines.flow.flow {
            try {
                val posts = apiService.getPosts()
                emit(Result.Success(posts))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    actual override fun observePostsByUser(userId: String): Flow<Result<List<Post>>> {
        return kotlinx.coroutines.flow.flow {
            try {
                val posts = apiService.getPosts().filter { it.userId == userId }
                emit(Result.Success(posts))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    actual override fun observeComments(postId: String): Flow<Result<List<Comment>>> {
        return kotlinx.coroutines.flow.flow {
            try {
                val comments = apiService.getComments(postId)
                emit(Result.Success(comments))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }
}
