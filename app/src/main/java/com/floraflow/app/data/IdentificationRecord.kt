package com.floraflow.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identification_records")
data class IdentificationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val photoPath: String,
    val commonName: String,
    val scientificName: String,
    val confidence: Int,
    val family: String? = null,
    val timestampMs: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)
