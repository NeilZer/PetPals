
package com.petpals.shared.src.core

// Backward-compatible alias
typealias AppResult<T> = Result<T>

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success<*>
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    @Suppress("UNCHECKED_CAST")
    fun getOrNull(): T? = (this as? Success<T>)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
}

// Helpful extensions
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}
inline fun <T> Result<T>.onError(action: (exception: Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> Result.Loading
}
