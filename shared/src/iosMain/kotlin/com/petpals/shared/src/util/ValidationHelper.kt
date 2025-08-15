
package com.petpals.shared.src.util

actual object ValidationHelper {
    actual fun validatePetName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "נא להכניס שם לחיית המחמד"
            trimmed.length < 2 -> "השם חייב להכיל לפחות 2 תווים"
            trimmed.length > 30 -> "השם לא יכול להכיל יותר מ-30 תווים"
            else -> null
        }
    }

    actual fun validatePostText(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> "נא להכניס תוכן לפוסט"
            trimmed.length > 500 -> "הפוסט לא יכול להכיל יותר מ-500 תווים"
            else -> null
        }
    }

    actual fun validateCommentText(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> "נא להכניס תוכן לתגובה"
            trimmed.length > 200 -> "התגובה לא יכולה להכיל יותר מ-200 תווים"
            else -> null
        }
    }

    actual fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        val emailRegex = Regex("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}")
        return when {
            trimmed.isEmpty() -> "נא להכניס כתובת אימייל"
            !emailRegex.matches(trimmed) -> "כתובת אימייל לא תקינה"
            else -> null
        }
    }

    actual fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "נא להכניס סיסמה"
            password.length < 6 -> "הסיסמה חייבת להכיל לפחות 6 תווים"
            else -> null
        }
    }
}
