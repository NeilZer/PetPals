package com.example.petpals.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.location.Location
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.ImageLoader
import android.graphics.*
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navController: NavController,
    selectedPostLocation: LatLng? = null
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val coroutineScope = rememberCoroutineScope()

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var nearbyUsers by remember { mutableStateOf<List<MapUserMarker>>(emptyList()) }
    var nearbyPosts by remember { mutableStateOf<List<MapPostMarker>>(emptyList()) }
    var currentUserData by remember { mutableStateOf<MapUserMarker?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // לשמור איזה פוסט נבחר להצגה מעל הסמן
    var selectedPostForDisplay by remember { mutableStateOf<MapPostMarker?>(null) }
    // לשמור reference למפה עבור המרה של LatLng לנקודת מסך
    var mapInstance by remember { mutableStateOf<GoogleMap?>(null) }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            try {
                val loc = fusedLocationClient.lastLocation.await()
                currentLocation = loc

                if (loc != null && currentUserId != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(currentUserId)
                        .update("location", GeoPoint(loc.latitude, loc.longitude))
                        .await()
                }

                coroutineScope.launch {
                    loadNearbyUsers(context, currentUserId, loc) { users, currentUser ->
                        nearbyUsers = users
                        currentUserData = currentUser
                    }
                    loadNearbyPosts(context, loc) { posts ->
                        nearbyPosts = posts
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(currentLocation, selectedPostLocation) {
        val targetLocation = selectedPostLocation ?: currentLocation?.let { LatLng(it.latitude, it.longitude) }
        targetLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 16f)
        }
    }

    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            onMapClick = {
                selectedPostForDisplay = null
            }
            // אין onMapLongClick, onPOIClick, onMapReady ב-Compose API
        )
         {
            // סמן המשתמש הנוכחי
            currentLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = "אני - ${currentUserData?.petName ?: "המיקום שלי"}",
                    icon = currentUserData?.bitmapDescriptor
                        ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {
                        currentUserData?.userId?.let { userId ->
                            Log.d("MapScreen", "סמן המשתמש שלי נלחץ - פתח פרופיל: $userId")
                            navController.navigate("profile/$userId")
                        }
                        true
                    }
                )
            }

            // סמני משתמשים אחרים
            nearbyUsers.forEach { user ->
                if (user.userId != currentUserId) {
                    Marker(
                        state = MarkerState(position = user.latLng),
                        title = user.petName,
                        snippet = currentLocation?.let {
                            val distance = calculateDistance(
                                it.latitude, it.longitude,
                                user.latLng.latitude, user.latLng.longitude
                            )
                            "מרחק: %.1f ק\"מ".format(distance / 1000)
                        } ?: "",
                        icon = user.bitmapDescriptor ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        onClick = {
                            Log.d("MapScreen", "סמן של משתמש ${user.userId} נלחץ - פתח פרופיל")
                            navController.navigate("profile/${user.userId}")
                            true
                        }
                    )
                }
            }

            // סמני פוסטים (תמונות)
            nearbyPosts.forEach { post ->
                Marker(
                    state = MarkerState(post.latLng),
                    title = post.petName,
                    snippet = post.text.take(50),
                    icon = post.bitmapDescriptor ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                    onClick = {
                        Log.d("MapScreen", "סמן פוסט נלחץ - פתח פרטי פוסט: ${post.postId}")
                        // עדכון הפוסט שנבחר להצגה מעל הסמן
                        selectedPostForDisplay = post
                        // ניווט לפרטי פוסט
                        navController.navigate("postDetail/${post.postId}")
                        true
                    }
                )
            }

            selectedPostLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "מיקום הפוסט",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }

        // הצגת תמונת הפוסט מעל הסמן, עם חשבון מיקום ממפה לנקודת מסך
        selectedPostForDisplay?.let { post ->
            val point = mapInstance?.projection?.toScreenLocation(post.latLng)
            point?.let { screenPoint ->
                // המרה מ-Pixels ל-DP
                val xDp = with(density) { screenPoint.x.toDp() }
                val yDp = with(density) { screenPoint.y.toDp() }
                // הזזה למעלה ולצד (כדי שהתמונה לא תכסה את הסמן עצמו)
                val offsetX = xDp - 64.dp
                val offsetY = yDp - 150.dp

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
                        .size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(post.bitmapDescriptor?.toString() ?: post.petName),
                        contentDescription = "Post Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // הצגת המרחק אם יש מיקום פוסט נבחר ומיקום נוכחי
        if (selectedPostLocation != null && currentLocation != null) {
            val distance = calculateDistance(
                currentLocation!!.latitude,
                currentLocation!!.longitude,
                selectedPostLocation.latitude,
                selectedPostLocation.longitude
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "המרחק ממך: %.1f ק\"מ".format(distance / 1000))
            }
        }
    }
}

// פונקציות שלך להמשך, ללא שינוי

suspend fun loadNearbyUsers(
    context: Context,
    currentUserId: String?,
    currentLocation: Location?,
    onUsersLoaded: (List<MapUserMarker>, MapUserMarker?) -> Unit
) {
    try {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("users").get().await()

        var currentUserMarker: MapUserMarker? = null
        val nearbyMarkers = mutableListOf<MapUserMarker>()

        for (doc in snapshot.documents) {
            val geoPoint = doc.getGeoPoint("location") ?: continue
            val petImage = doc.getString("petImage") ?: ""
            val petName = doc.getString("petName") ?: "Unknown Pet"
            val userId = doc.id

            val isNearby = currentLocation?.let {
                val distance = calculateDistance(
                    it.latitude,
                    it.longitude,
                    geoPoint.latitude,
                    geoPoint.longitude
                )
                distance <= 5000 // 5 ק"מ
            } ?: true

            if (isNearby) {
                val bitmapDescriptor = createUserMarkerIcon(context, petImage)
                val marker = MapUserMarker(
                    userId = userId,
                    petName = petName,
                    latLng = LatLng(geoPoint.latitude, geoPoint.longitude),
                    imageUrl = petImage,
                    bitmapDescriptor = bitmapDescriptor
                )
                if (userId == currentUserId) currentUserMarker = marker
                else nearbyMarkers.add(marker)
            }
        }
        onUsersLoaded(nearbyMarkers, currentUserMarker)
    } catch (e: Exception) {
        e.printStackTrace()
        onUsersLoaded(emptyList(), null)
    }
}

suspend fun loadNearbyPosts(
    context: Context,
    currentLocation: Location?,
    onPostsLoaded: (List<MapPostMarker>) -> Unit
) {
    try {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("posts").get().await()

        val nearbyPosts = mutableListOf<MapPostMarker>()
        for (doc in snapshot.documents) {
            val geoPoint = doc.getGeoPoint("location") ?: continue
            val userId = doc.getString("userId") ?: continue
            val petName = doc.getString("petName") ?: "Unknown Pet"
            val text = doc.getString("text") ?: ""
            val postId = doc.id

            val isNearby = currentLocation?.let {
                val distance = calculateDistance(
                    it.latitude,
                    it.longitude,
                    geoPoint.latitude,
                    geoPoint.longitude
                )
                distance <= 5000 // 5 ק"מ
            } ?: true

            if (isNearby) {
                val bitmapDescriptor = createPostMarkerIcon(context, doc.getString("imageUrl"))
                nearbyPosts.add(
                    MapPostMarker(
                        postId = postId,
                        petName = petName,
                        latLng = LatLng(geoPoint.latitude, geoPoint.longitude),
                        text = text,
                        bitmapDescriptor = bitmapDescriptor
                    )
                )
            }
        }
        onPostsLoaded(nearbyPosts)
    } catch (e: Exception) {
        e.printStackTrace()
        onPostsLoaded(emptyList())
    }
}

// פונקציות ליצירת אייקוני סמן מעוגלים

suspend fun createUserMarkerIcon(context: Context, url: String?): BitmapDescriptor? {
    if (url.isNullOrEmpty()) return null
    return try {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable ?: return null

        val width = if (result.intrinsicWidth > 0) result.intrinsicWidth else 100
        val height = if (result.intrinsicHeight > 0) result.intrinsicHeight else 100

        val rawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rawBitmap)
        result.setBounds(0, 0, canvas.width, canvas.height)
        result.draw(canvas)

        val size = 128
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // גבול לבן
        paint.color = Color.WHITE
        circleCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val shader = BitmapShader(
            Bitmap.createScaledBitmap(rawBitmap, size - 8, size - 8, false),
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        val radius = (size - 8) / 2f
        circleCanvas.drawCircle(size / 2f, size / 2f, radius, paint)

        BitmapDescriptorFactory.fromBitmap(output)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun createPostMarkerIcon(context: Context, url: String?): BitmapDescriptor? {
    if (url.isNullOrEmpty()) return null
    return try {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable ?: return null

        val width = if (result.intrinsicWidth > 0) result.intrinsicWidth else 100
        val height = if (result.intrinsicHeight > 0) result.intrinsicHeight else 100

        val rawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rawBitmap)
        result.setBounds(0, 0, canvas.width, canvas.height)
        result.draw(canvas)

        val size = 128
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // גבול לבן
        paint.color = Color.WHITE
        circleCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val shader = BitmapShader(
            Bitmap.createScaledBitmap(rawBitmap, size - 8, size - 8, false),
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        val radius = (size - 8) / 2f
        circleCanvas.drawCircle(size / 2f, size / 2f, radius, paint)

        BitmapDescriptorFactory.fromBitmap(output)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

data class MapUserMarker(
    val userId: String,
    val petName: String,
    val latLng: LatLng,
    val imageUrl: String,
    val bitmapDescriptor: BitmapDescriptor? = null
)

data class MapPostMarker(
    val postId: String,
    val petName: String,
    val latLng: LatLng,
    val text: String,
    val bitmapDescriptor: BitmapDescriptor? = null
)

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
