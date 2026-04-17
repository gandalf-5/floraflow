package com.floraflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_badges")
data class UserBadge(
    @PrimaryKey
    val badgeId: String,
    val earnedAt: Long = System.currentTimeMillis()
)
