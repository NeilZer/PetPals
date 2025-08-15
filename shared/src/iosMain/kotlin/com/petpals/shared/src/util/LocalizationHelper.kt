
package com.petpals.shared.util

/**
 * Helper for localization on iOS platform
 */
object LocalizationHelper {

    /**
     * Statistics-related localization keys
     */
    object Stats {
        const val TITLE = "stats.title"
        const val HEADER = "stats.header"
        const val LOADING = "stats.loading"

        // Quick stats
        const val POSTS = "stats.quick.posts"
        const val LIKES = "stats.quick.likes"
        const val COMMENTS = "stats.quick.comments"
        const val DISTANCE = "stats.breakdown.distance"
        const val ACTIVE_DAYS = "stats.quick.active_days"
        const val STREAK = "stats.quick.streak"

        // Insights
        const val INSIGHTS_TITLE = "stats.insights.title"
        const val AVG_LIKES = "stats.insights.avg_likes"
        const val MOST_ACTIVE_DAY = "stats.insights.most_active_day"
        const val FAVORITE_LOCATION = "stats.insights.favorite_location"

        // Monthly
        const val MONTHLY_TITLE = "stats.monthly.title"
        const val MONTHLY_POSTS = "stats.monthly.legend.posts"
        const val MONTHLY_LIKES = "stats.monthly.legend.likes"

        // Breakdown
        const val BREAKDOWN_TITLE = "stats.breakdown.title"
        const val BREAKDOWN_ACTIVE_DAYS = "stats.breakdown.active_days"

        // Achievements
        const val ACHIEVEMENTS_TITLE = "stats.achievements.title"
    }

    /**
     * Units and formatting
     */
    object Units {
        const val KM = "unit.km"
        const val METERS = "unit.meters"
        const val DAYS = "unit.days"
    }

    /**
     * Achievement localization keys
     */
    object Achievements {
        // Posting achievements
        const val FIRST_POST_TITLE = "achievement.first_post.title"
        const val FIRST_POST_DESC = "achievement.first_post.description"
        const val ACTIVE_USER_TITLE = "achievement.active_user.title"
        const val ACTIVE_USER_DESC = "achievement.active_user.description"
        const val CONTENT_CREATOR_TITLE = "achievement.content_creator.title"
        const val CONTENT_CREATOR_DESC = "achievement.content_creator.description"
        const val POSTING_MASTER_TITLE = "achievement.posting_master.title"
        const val POSTING_MASTER_DESC = "achievement.posting_master.description"

        // Social achievements
        const val FIRST_LIKE_TITLE = "achievement.first_like.title"
        const val FIRST_LIKE_DESC = "achievement.first_like.description"
        const val POPULAR_POSTS_TITLE = "achievement.popular_posts.title"
        const val POPULAR_POSTS_DESC = "achievement.popular_posts.description"
        const val COMMUNITY_FAVORITE_TITLE = "achievement.community_favorite.title"
        const val COMMUNITY_FAVORITE_DESC = "achievement.community_favorite.description"
        const val VIRAL_SENSATION_TITLE = "achievement.viral_sensation.title"
        const val VIRAL_SENSATION_DESC = "achievement.viral_sensation.description"

        // Activity achievements
        const val FIRST_WALK_TITLE = "achievement.first_walk.title"
        const val FIRST_WALK_DESC = "achievement.first_walk.description"
        const val NEIGHBORHOOD_EXPLORER_TITLE = "achievement.neighborhood_explorer.title"
        const val NEIGHBORHOOD_EXPLORER_DESC = "achievement.neighborhood_explorer.description"
        const val CITY_WANDERER_TITLE = "achievement.city_wanderer.title"
        const val CITY_WANDERER_DESC = "achievement.city_wanderer.description"
        const val ADVENTURE_MASTER_TITLE = "achievement.adventure_master.title"
        const val ADVENTURE_MASTER_DESC = "achievement.adventure_master.description"
    }

    /**
     * Format distance for display with proper localization
     */
    fun formatDistance(distance: Double, useMetric: Boolean = true): String {
        return if (useMetric) {
            when {
                distance < 1.0 -> "${(distance * 1000).toInt()}m"
                distance < 10.0 -> String.format("%.1f km", distance)
                else -> "${distance.toInt()} km"
            }
        } else {
            // Imperial units conversion
            val miles = distance * 0.621371
            when {
                miles < 0.1 -> "${(distance * 3280.84).toInt()} ft"
                miles < 10.0 -> String.format("%.1f mi", miles)
                else -> "${miles.toInt()} mi"
            }
        }
    }

    /**
     * Format duration for display
     */
    fun formatDuration(days: Int): String {
        return when {
            days == 1 -> "1 day"
            days < 7 -> "$days days"
            days < 30 -> "${days / 7} weeks"
            else -> "${days / 30} months"
        }
    }
}
