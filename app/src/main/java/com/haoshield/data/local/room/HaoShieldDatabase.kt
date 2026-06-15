package com.haoshield.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [JournalEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HaoShieldDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
}