package com.floraflow.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentificationRecordDao {

    @Insert
    suspend fun insert(record: IdentificationRecord): Long

    @Query("SELECT * FROM identification_records ORDER BY timestampMs DESC")
    fun getAllRecords(): Flow<List<IdentificationRecord>>

    @Query("SELECT * FROM identification_records ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<IdentificationRecord>>

    @Delete
    suspend fun delete(record: IdentificationRecord)

    @Query("SELECT COUNT(*) FROM identification_records")
    suspend fun getCount(): Int

    @Query("DELETE FROM identification_records WHERE id NOT IN (SELECT id FROM identification_records ORDER BY timestampMs DESC LIMIT :limit)")
    suspend fun keepOnlyRecent(limit: Int)
}
