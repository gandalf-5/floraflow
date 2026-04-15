package com.floraflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_plants")
data class DailyPlant(
    @PrimaryKey
    val dateKey: String,
    val photoId: String,
    val imageUrlFull: String,
    val imageUrlRegular: String,
    val plantName: String,
    val locationName: String?,
    val photographerName: String,
    val photographerUsername: String,
    val photographerProfileUrl: String,
    val downloadLocationUrl: String,
    val botanicalInsight: String,
    val fetchedAt: Long = System.currentTimeMillis()
)
