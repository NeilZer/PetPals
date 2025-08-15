package com.petpals.shared.src.util

import kotlinx.coroutines.flow.StateFlow

actual class LanguageManager {
    actual val currentLanguage: StateFlow<String>
        get() = TODO("Not yet implemented")
    actual var current: String
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val locale: String
        get() = TODO("Not yet implemented")
    actual val isRTL: Boolean
        get() = TODO("Not yet implemented")

    actual fun setLanguage(languageCode: String) {
    }

}