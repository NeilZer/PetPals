package com.petpals.shared.src.util

actual object Analytics {
    actual fun logEvent(
        eventName: String,
        parameters: Map<String, Any>?
    ) {
    }

    actual fun setUserId(userId: String) {
    }

    actual fun setUserProperty(name: String, value: String) {
    }

    actual fun logPostCreated(hasImage: Boolean, hasLocation: Boolean) {
    }

    actual fun logPostLiked() {
    }

    actual fun logCommentAdded() {
    }

    actual fun logProfileUpdated() {
    }

}