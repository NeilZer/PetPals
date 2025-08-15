package com.petpals.shared.src.util

expect object Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable?)
    fun verbose(tag: String, message: String)

}