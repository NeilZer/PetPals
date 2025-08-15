package com.petpals.shared.src.util

expect object ValidationHelper {
    fun validatePetName(name: String): String?
    fun validatePostText(text: String): String?
    fun validateCommentText(text: String): String?
    fun validateEmail(email: String): String?
    fun validatePassword(password: String): String?

}