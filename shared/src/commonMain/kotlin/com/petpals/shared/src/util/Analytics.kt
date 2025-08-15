package com.petpals.shared.src.util

expect object Analytics {
    fun logEvent(
        eventName: String,
        parameters: Map<String, Any>?
    )

    fun setUserId(userId: String)
    fun setUserProperty(name: String, value: String)
    fun logPostCreated(hasImage: Boolean, hasLocation: Boolean)
    fun logPostLiked()
    fun logCommentAdded()
    fun logProfileUpdated()

}