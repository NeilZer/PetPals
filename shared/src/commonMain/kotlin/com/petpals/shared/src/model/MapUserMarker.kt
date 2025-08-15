package com.petpals.shared.src.model

import com.petpals.shared.src.util.LatLng

data class MapUserMarker(
    val id: String = "",          // מזהה ייחודי של הסמן (יכול להיות = userId)
    val userId: String = "",      // מזהה המשתמש
    val displayName: String = "", // שם לתצוגה (שם משתמש/כינוי/שם חיית המחמד)
    val petName: String = "",     // אופציונלי: שם חיית המחמד
    val imageUrl: String = "",    // תמונת פרופיל/חיית המחמד
    val lat: Double? = null,      // קואורדינטות
    val lng: Double? = null,
    val distanceKm: Double? = null // אופציונלי: מרחק יחסי מהמשתמש המחפש
) {
    /** נוחות: LatLng מחושב אם יש lat/lng */
    val location: LatLng?
        get() = if (lat != null && lng != null) LatLng(lat, lng) else null
}
