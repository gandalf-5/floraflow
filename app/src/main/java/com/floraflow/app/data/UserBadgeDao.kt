package com.floraflow.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserBadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun awardBadge(badge: UserBadge)

    @Query("SELECT * FROM user_badges")
    suspend fun getAllEarnedBadges(): List<UserBadge>

    @Query("SELECT EXISTS(SELECT 1 FROM user_badges WHERE badgeId = :badgeId)")
    suspend fun hasBadge(badgeId: String): Boolean
}
