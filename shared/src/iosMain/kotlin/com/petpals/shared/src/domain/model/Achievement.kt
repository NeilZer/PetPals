
package com.petpals.shared.src.domain.model

/**
 * Achievement model for iOS implementation
 */
data class Achievement(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val icon: String,
    val level: AchievementLevel,
    val isUnlocked: Boolean,
    val requirement: Int,
    val currentProgress: Int = 0
) {
    val progress: Float
        get() = if (requirement > 0) (currentProgress.toFloat() / requirement).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = currentProgress >= requirement
}

enum class AchievementLevel(val colorHex: String) {
    BRONZE("#CD7F32"),
    SILVER("#C0C0C0"),
    GOLD("#FFD700"),
    PLATINUM("#E5E4E2")
}

/**
 * Factory for creating achievements based on user statistics
 */
object AchievementFactory {

    fun createPostingAchievements(totalPosts: Int): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_post",
                titleKey = "achievement.first_post.title",
                descriptionKey = "achievement.first_post.description",
                icon = "🌟",
                level = AchievementLevel.BRONZE,
                isUnlocked = totalPosts >= 1,
                requirement = 1,
                currentProgress = totalPosts
            ),
            Achievement(
                id = "active_user",
                titleKey = "achievement.active_user.title",
                descriptionKey = "achievement.active_user.description",
                icon = "🔥",
                level = AchievementLevel.SILVER,
                isUnlocked = totalPosts >= 10,
                requirement = 10,
                currentProgress = totalPosts
            ),
            Achievement(
                id = "content_creator",
                titleKey = "achievement.content_creator.title",
                descriptionKey = "achievement.content_creator.description",
                icon = "📸",
                level = AchievementLevel.GOLD,
                isUnlocked = totalPosts >= 50,
                requirement = 50,
                currentProgress = totalPosts
            ),
            Achievement(
                id = "posting_master",
                titleKey = "achievement.posting_master.title",
                descriptionKey = "achievement.posting_master.description",
                icon = "👑",
                level = AchievementLevel.PLATINUM,
                isUnlocked = totalPosts >= 100,
                requirement = 100,
                currentProgress = totalPosts
            )
        )
    }

    fun createSocialAchievements(totalLikes: Int): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_like",
                titleKey = "achievement.first_like.title",
                descriptionKey = "achievement.first_like.description",
                icon = "❤️",
                level = AchievementLevel.BRONZE,
                isUnlocked = totalLikes >= 1,
                requirement = 1,
                currentProgress = totalLikes
            ),
            Achievement(
                id = "popular_posts",
                titleKey = "achievement.popular_posts.title",
                descriptionKey = "achievement.popular_posts.description",
                icon = "💕",
                level = AchievementLevel.SILVER,
                isUnlocked = totalLikes >= 50,
                requirement = 50,
                currentProgress = totalLikes
            ),
            Achievement(
                id = "community_favorite",
                titleKey = "achievement.community_favorite.title",
                descriptionKey = "achievement.community_favorite.description",
                icon = "🌟",
                level = AchievementLevel.GOLD,
                isUnlocked = totalLikes >= 200,
                requirement = 200,
                currentProgress = totalLikes
            ),
            Achievement(
                id = "viral_sensation",
                titleKey = "achievement.viral_sensation.title",
                descriptionKey = "achievement.viral_sensation.description",
                icon = "🚀",
                level = AchievementLevel.PLATINUM,
                isUnlocked = totalLikes >= 500,
                requirement = 500,
                currentProgress = totalLikes
            )
        )
    }

    fun createActivityAchievements(totalDistance: Double): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_walk",
                titleKey = "achievement.first_walk.title",
                descriptionKey = "achievement.first_walk.description",
                icon = "🚶",
                level = AchievementLevel.BRONZE,
                isUnlocked = totalDistance >= 1.0,
                requirement = 1,
                currentProgress = totalDistance.toInt()
            ),
            Achievement(
                id = "neighborhood_explorer",
                titleKey = "achievement.neighborhood_explorer.title",
                descriptionKey = "achievement.neighborhood_explorer.description",
                icon = "🗺️",
                level = AchievementLevel.SILVER,
                isUnlocked = totalDistance >= 25.0,
                requirement = 25,
                currentProgress = totalDistance.toInt()
            ),
            Achievement(
                id = "city_wanderer",
                titleKey = "achievement.city_wanderer.title",
                descriptionKey = "achievement.city_wanderer.description",
                icon = "🏃",
                level = AchievementLevel.GOLD,
                isUnlocked = totalDistance >= 100.0,
                requirement = 100,
                currentProgress = totalDistance.toInt()
            ),
            Achievement(
                id = "adventure_master",
                titleKey = "achievement.adventure_master.title",
                descriptionKey = "achievement.adventure_master.description",
                icon = "🌍",
                level = AchievementLevel.PLATINUM,
                isUnlocked = totalDistance >= 500.0,
                requirement = 500,
                currentProgress = totalDistance.toInt()
            )
        )
    }

    fun getAllAchievements(totalPosts: Int, totalLikes: Int, totalDistance: Double): List<Achievement> {
        return createPostingAchievements(totalPosts) +
                createSocialAchievements(totalLikes) +
                createActivityAchievements(totalDistance)
    }
}
