package com.petpals.shared.src.util

import kotlinx.coroutines.flow.StateFlow

expect class LanguageManager {
    val currentLanguage: StateFlow<String>
    var current: String
    val locale: String
    val isRTL: Boolean
    fun setLanguage(languageCode: String)

}