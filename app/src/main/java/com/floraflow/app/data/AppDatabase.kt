package com.floraflow.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailyPlant::class, IdentificationRecord::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyPlantDao(): DailyPlantDao
    abstract fun identificationRecordDao(): IdentificationRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_plants ADD COLUMN scientificName TEXT")
                database.execSQL(
                    "ALTER TABLE daily_plants ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_plants ADD COLUMN notes TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_plants ADD COLUMN nativeRegion TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS identification_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        photoPath TEXT NOT NULL,
                        commonName TEXT NOT NULL,
                        scientificName TEXT NOT NULL,
                        confidence INTEGER NOT NULL,
                        family TEXT,
                        timestampMs INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationName TEXT
                    )"""
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "floraflow_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
