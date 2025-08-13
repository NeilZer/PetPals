package com.example.petpals.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

data class MonthlyStats(
    val month: String,
    val postsCount: Int,
    val likesReceived: Int,
    val totalDistance: Double,
    val activeDays: Int
)

data class StatCard(
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun StatisticsScreen(navController: NavHostController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var monthlyStats by remember { mutableStateOf<List<MonthlyStats>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalPosts by remember { mutableStateOf(0) }
    var totalLikes by remember { mutableStateOf(0) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var activeDaysThisMonth by remember { mutableStateOf(0) }

    // טעינת הנתונים
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            loadStatistics(currentUserId) { stats, posts, likes, distance, days ->
                monthlyStats = stats
                totalPosts = posts
                totalLikes = likes
                totalDistance = distance
                activeDaysThisMonth = days
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // כותרת עם לוגו
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🐾", fontSize = 32.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "PetPals Statistics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // כרטיסי סטטיסטיקה מהירה
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statCards = listOf(
                    StatCard("פוסטים", totalPosts.toString(), Icons.Default.PhotoCamera, Color(0xFFE91E63)),
                    StatCard("לייקים", totalLikes.toString(), Icons.Default.Favorite, Color(0xFF9C27B0))
                )

                statCards.forEach { card ->
                    QuickStatCard(
                        card = card,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statCards = listOf(
                    StatCard("מרחק כולל", String.format("%.1f ק\"מ", totalDistance), Icons.Default.LocationOn, Color(0xFF2196F3)),
                    StatCard("ימים פעילים", activeDaysThisMonth.toString(), Icons.Default.DateRange, Color(0xFF4CAF50))
                )

                statCards.forEach { card ->
                    QuickStatCard(
                        card = card,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // גרף פעילות חודשית
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "פעילות חודשית",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))

                    if (monthlyStats.isNotEmpty()) {
                        MonthlyActivityChart(monthlyStats)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("אין נתונים להצגה")
                        }
                    }
                }
            }
        }

        // פירוט חודשי
        item {
            Text(
                "פירוט חודשי",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(monthlyStats.reversed()) { stat ->
            MonthlyStatCard(stat)
        }

        // הישגים
        item {
            AchievementsCard(totalPosts, totalLikes, totalDistance)
        }
    }
}

@Composable
fun QuickStatCard(
    card: StatCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = card.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                card.icon,
                contentDescription = card.title,
                tint = card.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                card.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = card.color
            )
            Text(
                card.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthlyActivityChart(stats: List<MonthlyStats>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        drawMonthlyChart(stats, size)
    }
}

fun DrawScope.drawMonthlyChart(stats: List<MonthlyStats>, canvasSize: androidx.compose.ui.geometry.Size) {
    if (stats.isEmpty()) return

    val maxPosts = stats.maxOfOrNull { it.postsCount } ?: 1
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)

    // ציור רקע
    drawRect(
        color = Color.Gray.copy(alpha = 0.1f),
        topLeft = Offset(padding, padding),
        size = androidx.compose.ui.geometry.Size(chartWidth, chartHeight)
    )

    // ציור קווי רשת
    for (i in 0..4) {
        val y = padding + (chartHeight / 4) * i
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // ציור הגרף
    val path = Path()
    val pointsData = stats.mapIndexed { index, stat ->
        val x = padding + (chartWidth / (stats.size - 1)) * index
        val y = padding + chartHeight - (stat.postsCount.toFloat() / maxPosts) * chartHeight
        Offset(x, y)
    }

    if (pointsData.isNotEmpty()) {
        path.moveTo(pointsData[0].x, pointsData[0].y)
        for (i in 1 until pointsData.size) {
            path.lineTo(pointsData[i].x, pointsData[i].y)
        }

        // ציור הקו
        drawPath(
            path = path,
            color = Color(0xFF2196F3),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )

        // ציור נקודות
        pointsData.forEach { point ->
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 6.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
fun MonthlyStatCard(stat: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stat.month,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("🐾", fontSize = 20.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("פוסטים", stat.postsCount.toString(), Color(0xFFE91E63))
                StatItem("לייקים", stat.likesReceived.toString(), Color(0xFF9C27B0))
                StatItem("מרחק", String.format("%.1fק\"מ", stat.totalDistance), Color(0xFF2196F3))
                StatItem("ימים פעילים", stat.activeDays.toString(), Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AchievementsCard(totalPosts: Int, totalLikes: Int, totalDistance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "הישגים",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            val achievements = mutableListOf<String>()

            when {
                totalPosts >= 50 -> achievements.add("🌟 מפרסם מקצועי - 50+ פוסטים!")
                totalPosts >= 20 -> achievements.add("📸 צלם מוכשר - 20+ פוסטים!")
                totalPosts >= 10 -> achievements.add("📱 פעיל בקהילה - 10+ פוסטים!")
                totalPosts >= 1 -> achievements.add("🎉 פוסט ראשון - ברוכים הבאים!")
            }

            when {
                totalLikes >= 100 -> achievements.add("❤️ אהוב על הקהילה - 100+ לייקים!")
                totalLikes >= 50 -> achievements.add("💕 פופולרי - 50+ לייקים!")
                totalLikes >= 10 -> achievements.add("👍 מוערך - 10+ לייקים!")
            }

            when {
                totalDistance >= 100 -> achievements.add("🚀 נווד מקצועי - 100+ ק\"מ!")
                totalDistance >= 50 -> achievements.add("🗺️ חוקר - 50+ ק\"מ!")
                totalDistance >= 10 -> achievements.add("🚶 הולך רגל - 10+ ק\"מ!")
            }

            if (achievements.isEmpty()) {
                achievements.add("🌱 תתחילו לפרסם כדי לזכות בהישגים!")
            }

            achievements.forEach { achievement ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        achievement,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

suspend fun loadStatistics(
    userId: String,
    onComplete: (List<MonthlyStats>, Int, Int, Double, Int) -> Unit
) {
    try {
        val db = FirebaseFirestore.getInstance()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // טעינת כל הפוסטים של המשתמש
        val postsSnapshot = db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val posts = postsSnapshot.documents.mapNotNull { doc ->
            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
            val likes = doc.getLong("likes")?.toInt() ?: 0
            val geoPoint = doc.getGeoPoint("location")

            Triple(timestamp, likes, geoPoint)
        }

        // חישוב סטטיסטיקות כלליות
        val totalPosts = posts.size
        val totalLikes = posts.sumOf { it.second }

        // חישוב מרחק כולל (הערכה בסיסית)
        val totalDistance = posts.count { it.third != null } * 2.5 // הערכה של 2.5 ק"מ לטיול

        // חישוב ימים פעילים החודש
        val activeDaysThisMonth = posts.count { postData ->
            val postCalendar = Calendar.getInstance().apply { timeInMillis = postData.first }
            postCalendar.get(Calendar.MONTH) == currentMonth &&
                    postCalendar.get(Calendar.YEAR) == currentYear
        }

        // חישוב נתונים חודשיים (6 חודשים אחרונים)
        val monthlyStats = mutableListOf<MonthlyStats>()
        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

        for (i in 5 downTo 0) {
            val monthCalendar = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val month = monthCalendar.get(Calendar.MONTH)
            val year = monthCalendar.get(Calendar.YEAR)
            val monthStr = dateFormat.format(monthCalendar.time)

            val monthPosts = posts.filter { postData ->
                val postCalendar = Calendar.getInstance().apply { timeInMillis = postData.first }
                postCalendar.get(Calendar.MONTH) == month && postCalendar.get(Calendar.YEAR) == year
            }

            val monthStats = MonthlyStats(
                month = monthStr,
                postsCount = monthPosts.size,
                likesReceived = monthPosts.sumOf { it.second },
                totalDistance = monthPosts.count { it.third != null } * 2.5,
                activeDays = monthPosts.map { postData ->
                    val cal = Calendar.getInstance().apply { timeInMillis = postData.first }
                    cal.get(Calendar.DAY_OF_MONTH)
                }.distinct().size
            )

            monthlyStats.add(monthStats)
        }

        onComplete(monthlyStats, totalPosts, totalLikes, totalDistance, activeDaysThisMonth)

    } catch (e: Exception) {
        Log.e("STATISTICS", "Error loading statistics", e)
        onComplete(emptyList(), 0, 0, 0.0, 0)
    }
}
