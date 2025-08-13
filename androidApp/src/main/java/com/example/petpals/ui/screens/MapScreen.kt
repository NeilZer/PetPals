package com.example.petpals.ui.screens

import com.petpals.shared.src.util.calculateDistance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
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
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.petpals.ui.Screen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
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

    // בחירה + רפרנס למפה
    var selectedPostForDisplay by remember { mutableStateOf<MapPostMarker?>(null) }
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
                Log.e("MapScreen", "Failed to init location/data", e)
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(currentLocation, selectedPostLocation) {
        val target = selectedPostLocation ?: currentLocation?.let { LatLng(it.latitude, it.longitude) }
        target?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 16f)
        }
    }

    val density = LocalDensity.current
    val isLocGranted = locationPermissionState.status.isGranted

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = isLocGranted),
            onMapClick = { selectedPostForDisplay = null }
        ) {
            // לוכדים את המפה בשביל projection
            MapEffect { map -> mapInstance = map }

            // סמן משתמש נוכחי
            currentLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = "אני - ${currentUserData?.petName ?: "המיקום שלי"}",
                    icon = currentUserData?.bitmapDescriptor
                        ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {
                        currentUserData?.userId?.let { userId ->
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
                        icon = user.bitmapDescriptor
                            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        onClick = {
                            navController.navigate("profile/${user.userId}")
                            true
                        }
                    )
                }
            }

            // סמני פוסטים
            nearbyPosts.forEach { post ->
                Marker(
                    state = MarkerState(post.latLng),
                    title = post.petName,
                    snippet = post.text.take(50),
                    icon = post.bitmapDescriptor
                        ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                    onClick = {
                        selectedPostForDisplay = post
                        navController.navigate("${Screen.PostDetail.route}/${post.postId}")
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

        // תמונת פוסט צפה מעל הסמן
        selectedPostForDisplay?.let { post ->
            val point = mapInstance?.projection?.toScreenLocation(post.latLng)
            point?.let { screenPoint ->
                val xDp = with(density) { screenPoint.x.toDp() }
                val yDp = with(density) { screenPoint.y.toDp() }
                val offsetX = xDp - 64.dp
                val offsetY = yDp - 150.dp

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
                        .size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(post.imageUrl),
                        contentDescription = "Post Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // מרחק למיקום נבחר (אם נשלח)
        if (selectedPostLocation != null && currentLocation != null) {
            val distance = calculateDistance(
                currentLocation!!.latitude,
                currentLocation!!.longitude,
                selectedPostLocation.latitude,
                selectedPostLocation.longitude
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "המרחק ממך: %.1f ק\"מ".format(distance / 1000))
            }
        }
    }
}

/* ===== עזרי טעינה ===== */

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
                    it.latitude, it.longitude,
                    geoPoint.latitude, geoPoint.longitude
                )
                distance <= 5_000
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
        Log.e("MapScreen", "loadNearbyUsers failed", e)
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
            val petName = doc.getString("petName") ?: "Unknown Pet" // ייתכן ולא קיים במסמך
            val text = doc.getString("description") ?: doc.getString("text").orEmpty() // תמיכה אחורה
            val postId = doc.id
            val imageUrl = doc.getString("imageUrl") ?: ""

            val isNearby = currentLocation?.let {
                val distance = calculateDistance(
                    it.latitude, it.longitude,
                    geoPoint.latitude, geoPoint.longitude
                )
                distance <= 5_000
            } ?: true

            if (isNearby) {
                val icon = createPostMarkerIcon(context, imageUrl)
                nearbyPosts.add(
                    MapPostMarker(
                        postId = postId,
                        petName = petName,
                        latLng = LatLng(geoPoint.latitude, geoPoint.longitude),
                        text = text,
                        imageUrl = imageUrl,
                        bitmapDescriptor = icon
                    )
                )
            }
        }
        onPostsLoaded(nearbyPosts)
    } catch (e: Exception) {
        Log.e("MapScreen", "loadNearbyPosts failed", e)
        onPostsLoaded(emptyList())
    }
}

/* ===== ציור אייקון מעוגל לסמן ===== */

suspend fun createUserMarkerIcon(context: Context, url: String?): BitmapDescriptor? {
    if (url.isNullOrEmpty()) return null
    return try {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
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
        Log.e("MapScreen", "createUserMarkerIcon failed", e)
        null
    }
}

suspend fun createPostMarkerIcon(context: Context, url: String?): BitmapDescriptor? {
    if (url.isNullOrEmpty()) return null
    return try {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
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
        Log.e("MapScreen", "createPostMarkerIcon failed", e)
        null
    }
}

/* ===== מודלים למסך המפה ===== */

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
    val imageUrl: String,
    val bitmapDescriptor: BitmapDescriptor? = null
)
