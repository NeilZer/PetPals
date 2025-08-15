
package com.petpals.shared.src.data.repository

import com.petpals.shared.src.core.AppResult
import com.petpals.shared.src.data.api.ApiService
import com.petpals.shared.src.domain.repository.IPostRepository
import com.petpals.shared.src.model.Post
import com.petpals.shared.src.core.Result

import com.petpals.shared.src.model.Comment
import com.petpals.shared.src.model.MapPostMarker
import com.petpals.shared.src.util.LatLng
import kotlinx.coroutines.flow.Flow

actual class PostRepositoryImpl actual constructor(
    private val apiService: ApiService
) : IPostRepository {
    override suspend fun getFeed(): AppResult<List<Post>> {
        TODO("Not yet implemented")
    }

    actual override suspend fun createPost(post: Post): Result<Post> {
        return try {
            val createdPost = apiService.createPost(post)
            Result.Success(createdPost)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun createPostWithImage(
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
                timestamp = com.petpals.shared.src.util.Time.getCurrentTimestamp(),
                lat = lat,
                lng = lng
            )
            apiService.createPost(post)
            Result.Success(postId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun getPosts(limit: Int, offset: Int): Result<List<Post>> {
        return try {
            val posts = apiService.getPosts()
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun getPostsByUser(userId: String): Result<List<Post>> {
        return try {
            val posts = apiService.getPosts().filter { it.userId == userId }
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun getPostsByLocation(location: LatLng, radius: Double): Result<List<Post>> {
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

    actual suspend fun getPostById(postId: String): Result<Post> {
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

    actual suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            apiService.unlikePost(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun toggleLike(postId: String, userId: String): Result<Unit> {
        return try {
            apiService.toggleLike(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            apiService.deletePost(postId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun addComment(comment: Comment): Result<Comment> {
        return try {
            val addedComment = apiService.addComment(comment)
            Result.Success(addedComment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun addCommentToPost(postId: String, userId: String, text: String): Result<Unit> {
        return try {
            apiService.addCommentToPost(postId, userId, text)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val comments = apiService.getComments(postId)
            Result.Success(comments)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            apiService.deleteComment(postId, commentId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual suspend fun getNearbyPosts(currentLocation: LatLng, radiusKm: Double): Result<List<MapPostMarker>> {
        return try {
            val posts = apiService.getNearbyPosts(currentLocation, radiusKm)
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    actual fun observePosts(): Flow<Result<List<Post>>> {
        return kotlinx.coroutines.flow.flow {
            try {
                val posts = apiService.getPosts()
                emit(Result.Success(posts))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    actual fun observePostsByUser(userId: String): Flow<Result<List<Post>>> {
        return kotlinx.coroutines.flow.flow {
            try {
                val posts = apiService.getPosts().filter { it.userId == userId }
                emit(Result.Success(posts))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    actual fun observeComments(postId: String): Flow<Result<List<Comment>>> {
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
