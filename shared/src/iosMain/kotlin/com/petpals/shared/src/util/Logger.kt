
package com.petpals.shared.util

import platform.Foundation.NSLog

actual object Logger {

    actual fun debug(tag: String, message: String) {
        NSLog("🐾 DEBUG [$tag]: $message")
    }

    actual fun info(tag: String, message: String) {
        NSLog("ℹ️ INFO [$tag]: $message")
    }

    actual fun warning(tag: String, message: String) {
        NSLog("⚠️ WARNING [$tag]: $message")
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        val errorMsg = if (throwable != null) {
            "$message - ${throwable.message}"
        } else {
            message
        }
        NSLog("❌ ERROR [$tag]: $errorMsg")
    }

    actual fun verbose(tag: String, message: String) {
        NSLog("📝 VERBOSE [$tag]: $message")
    }

    actual fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> debug(tag, message)
            LogLevel.INFO -> info(tag, message)
            LogLevel.WARNING -> warning(tag, message)
            LogLevel.ERROR -> error(tag, message, throwable)
            LogLevel.VERBOSE -> verbose(tag, message)
        }
    }
}

actual enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, VERBOSE
}
