package com.floraflow.app.util

import androidx.annotation.StringRes
import com.floraflow.app.R
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.UserBadge

data class BadgeDefinition(
    val id: String,
    val emoji: String,
    @StringRes val nameResId: Int,
    @StringRes val descResId: Int,
    val category: String
)

object BadgeManager {

    val ALL_BADGES: List<BadgeDefinition> = listOf(
        BadgeDefinition("streak_3",   "🌱", R.string.badge_streak_3_name,   R.string.badge_streak_3_desc,   "discovery"),
        BadgeDefinition("streak_7",   "🌿", R.string.badge_streak_7_name,   R.string.badge_streak_7_desc,   "discovery"),
        BadgeDefinition("streak_30",  "🌳", R.string.badge_streak_30_name,  R.string.badge_streak_30_desc,  "discovery"),
        BadgeDefinition("streak_100", "🏆", R.string.badge_streak_100_name, R.string.badge_streak_100_desc, "discovery"),
        BadgeDefinition("fav_5",      "💚", R.string.badge_fav_5_name,      R.string.badge_fav_5_desc,      "discovery"),
        BadgeDefinition("fav_20",     "🌺", R.string.badge_fav_20_name,     R.string.badge_fav_20_desc,     "discovery"),
        BadgeDefinition("ident_1",    "🔬", R.string.badge_ident_1_name,    R.string.badge_ident_1_desc,    "identification"),
        BadgeDefinition("ident_10",   "🧬", R.string.badge_ident_10_name,   R.string.badge_ident_10_desc,   "identification"),
        BadgeDefinition("ident_25",   "🌍", R.string.badge_ident_25_name,   R.string.badge_ident_25_desc,   "identification"),
        BadgeDefinition("ident_50",   "🔭", R.string.badge_ident_50_name,   R.string.badge_ident_50_desc,   "identification"),
        BadgeDefinition("coll_1",     "📚", R.string.badge_coll_1_name,     R.string.badge_coll_1_desc,     "collection"),
        BadgeDefinition("coll_5",     "🗂️", R.string.badge_coll_5_name,     R.string.badge_coll_5_desc,     "collection")
    )

    fun forId(id: String) = ALL_BADGES.find { it.id == id }

    suspend fun checkAndAwardBadges(
        db: AppDatabase,
        streak: Int = 0,
        favoriteCount: Int = 0,
        identCount: Int = 0,
        collectionCount: Int = 0
    ): List<String> {
        val awarded = mutableListOf<String>()
        val dao = db.userBadgeDao()

        val checks = mapOf(
            "streak_3"   to (streak >= 3),
            "streak_7"   to (streak >= 7),
            "streak_30"  to (streak >= 30),
            "streak_100" to (streak >= 100),
            "fav_5"      to (favoriteCount >= 5),
            "fav_20"     to (favoriteCount >= 20),
            "ident_1"    to (identCount >= 1),
            "ident_10"   to (identCount >= 10),
            "ident_25"   to (identCount >= 25),
            "ident_50"   to (identCount >= 50),
            "coll_1"     to (collectionCount >= 1),
            "coll_5"     to (collectionCount >= 5)
        )

        for ((badgeId, condition) in checks) {
            if (condition && !dao.hasBadge(badgeId)) {
                dao.awardBadge(UserBadge(badgeId))
                awarded.add(badgeId)
            }
        }
        return awarded
    }
}
