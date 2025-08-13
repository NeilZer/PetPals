// shared/src/commonMain/kotlin/com/petpals/shared/model/Post.kt
package com.petpals.shared.src.model

data class Post(
    val postId: String = "",
    val userId: String = "",
    val description: String = "",      // היה 'text'
    val imageUrl: String = "",
    val timestamp: Long = 0L,          // epoch millis
    val lat: Double? = null,           // קואורדינטות בנפרד
    val lng: Double? = null,
    val likes: List<String> = emptyList(), // מי עשה לייק (userIds)
    // אופציונלי: שדות תצוגה שתוכלי למלא מאוחר יותר אם צריך
    val petName: String = "",
    val petImage: String = ""
)
