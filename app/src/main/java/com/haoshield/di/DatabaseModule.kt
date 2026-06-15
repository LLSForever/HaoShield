package com.haoshield.di

import android.content.Context
import androidx.room.Room
import com.haoshield.data.local.room.HaoShieldDatabase
import com.haoshield.data.local.room.JournalEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): HaoShieldDatabase =
        Room.databaseBuilder(
            context,
            HaoShieldDatabase::class.java,
            "hao_shield.db",
        ).build()

    @Provides
    fun provideJournalEntryDao(database: HaoShieldDatabase): JournalEntryDao =
        database.journalEntryDao()
}