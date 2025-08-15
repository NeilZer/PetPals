
package com.petpals.shared.src.util


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class LanguageManager {
    private val _currentLanguage = MutableStateFlow(getCurrentLanguage())
    actual val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    actual var current: String
        get() = _currentLanguage.value
        set(value) {
            if (_currentLanguage.value != value) {
                _currentLanguage.value = value
                NSUserDefaults.standardUserDefaults.setObject(value, forKey = "selectedLanguage")
                postLanguageChangeNotification()
            }
        }

    actual val locale: String get() = current
    actual val isRTL: Boolean get() = current == "he"

    private fun getCurrentLanguage(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("selectedLanguage") ?: "he"
    }

    private fun postLanguageChangeNotification() {
        NSNotificationCenter.defaultCenter.postNotificationName(
            "app.languageChanged",
            `object` = null
        )
    }

    actual fun setLanguage(languageCode: String) {
        current = languageCode
    }
}
