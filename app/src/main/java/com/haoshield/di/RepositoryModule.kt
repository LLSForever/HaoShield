package com.haoshield.di

import com.haoshield.data.repository.BlockingRepositoryImpl
import com.haoshield.data.repository.JournalRepositoryImpl
import com.haoshield.data.repository.SessionRepositoryImpl
import com.haoshield.data.repository.SettingsRepositoryImpl
import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.repository.JournalRepository
import com.haoshield.domain.repository.SessionRepository
import com.haoshield.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl,
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(
        impl: JournalRepositoryImpl,
    ): JournalRepository

    @Binds
    @Singleton
    abstract fun bindBlockingRepository(
        impl: BlockingRepositoryImpl,
    ): BlockingRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository
}