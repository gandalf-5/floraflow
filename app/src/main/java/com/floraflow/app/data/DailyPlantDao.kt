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

    @Query("SELECT * FROM daily_plants ORDER BY fetchedAt DESC LIMIT 90")
    suspend fun getHistory(): List<DailyPlant>

    @Query("SELECT * FROM daily_plants WHERE isFavorite = 1 ORDER BY fetchedAt DESC")
    suspend fun getFavorites(): List<DailyPlant>

    @Query("UPDATE daily_plants SET isFavorite = :isFavorite WHERE dateKey = :dateKey")
    suspend fun setFavorite(dateKey: String, isFavorite: Boolean)

    @Query("UPDATE daily_plants SET notes = :notes WHERE dateKey = :dateKey")
    suspend fun updateNotes(dateKey: String, notes: String?)

    @Query("DELETE FROM daily_plants WHERE fetchedAt < :cutoff AND isFavorite = 0")
    suspend fun pruneOlderThan(cutoff: Long)

    @Query("SELECT * FROM daily_plants ORDER BY fetchedAt DESC")
    suspend fun getAllForSeasonal(): List<DailyPlant>
}
