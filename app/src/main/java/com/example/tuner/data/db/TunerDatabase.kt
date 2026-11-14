package com.example.tuner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// M1: tabel magisk_module menyusul di M2 (lihat build-log.md).
@Database(
    entities = [GameEntryEntity::class, GameProfileEntity::class, ApplyLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TunerDatabase : RoomDatabase() {
    abstract fun gameEntryDao(): GameEntryDao
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun applyLogDao(): ApplyLogDao
}
