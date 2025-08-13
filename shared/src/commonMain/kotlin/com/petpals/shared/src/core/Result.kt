package com.petpals.shared.src.core

sealed class Result<out T> {
    data class Ok<T>(val value: T): Result<T>()
    data class Err(val throwable: Throwable): Result<Nothing>()
}
