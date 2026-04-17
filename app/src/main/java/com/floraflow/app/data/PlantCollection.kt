package com.floraflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_collections")
data class PlantCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "🌿",
    val createdAt: Long = System.currentTimeMillis()
)
