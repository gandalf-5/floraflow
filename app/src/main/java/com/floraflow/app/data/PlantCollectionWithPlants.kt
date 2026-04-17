package com.floraflow.app.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlantCollectionWithPlants(
    @Embedded val collection: PlantCollection,
    @Relation(
        parentColumn = "id",
        entityColumn = "dateKey",
        associateBy = Junction(
            value = PlantCollectionCrossRef::class,
            parentColumn = "collectionId",
            entityColumn = "plantDateKey"
        )
    )
    val plants: List<DailyPlant>
)
