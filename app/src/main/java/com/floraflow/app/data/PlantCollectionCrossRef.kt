package com.floraflow.app.data

import androidx.room.Entity

@Entity(
    tableName = "plant_collection_cross_ref",
    primaryKeys = ["collectionId", "plantDateKey"]
)
data class PlantCollectionCrossRef(
    val collectionId: Long,
    val plantDateKey: String
)
