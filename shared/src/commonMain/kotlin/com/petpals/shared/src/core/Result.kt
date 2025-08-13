package com.petpals.shared.src.core

sealed class AppResult<out T> {
    data class Success<T>(val value: T): AppResult<T>()
    data class Error(val message: String, val cause: Throwable? = null): AppResult<Nothing>()
}
