package com.floraflow.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantCollectionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: PlantCollection): Long

    @Delete
    suspend fun deleteCollection(collection: PlantCollection)

    @Query("SELECT * FROM plant_collections ORDER BY createdAt DESC")
    fun getAllCollectionsFlow(): Flow<List<PlantCollection>>

    @Transaction
    @Query("SELECT * FROM plant_collections ORDER BY createdAt DESC")
    suspend fun getAllCollectionsWithPlants(): List<PlantCollectionWithPlants>

    @Transaction
    @Query("SELECT * FROM plant_collections WHERE id = :id")
    suspend fun getCollectionWithPlants(id: Long): PlantCollectionWithPlants?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPlantToCollection(crossRef: PlantCollectionCrossRef)

    @Delete
    suspend fun removePlantFromCollection(crossRef: PlantCollectionCrossRef)

    @Query("SELECT * FROM plant_collection_cross_ref WHERE plantDateKey = :dateKey")
    suspend fun getCollectionsForPlant(dateKey: String): List<PlantCollectionCrossRef>

    @Query("SELECT COUNT(*) FROM plant_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun getPlantCount(collectionId: Long): Int

    @Query("SELECT COUNT(*) FROM plant_collections")
    suspend fun getCollectionCount(): Int
}
