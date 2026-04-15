package com.floraflow.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyPlantDao {

    @Query("SELECT * FROM daily_plants WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): DailyPlant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: DailyPlant)

    @Query("SELECT * FROM daily_plants ORDER BY fetchedAt DESC LIMIT 30")
    suspend fun getRecent(): List<DailyPlant>
}
