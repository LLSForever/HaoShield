package com.haoshield.di

import com.haoshield.data.service.AccessibilityAppBlockingService
import com.haoshield.data.service.AppBlockingService
import com.haoshield.data.nfc.NfcManagerImpl
import com.haoshield.data.service.NfcShieldService
import com.haoshield.data.service.NfcShieldServiceImpl
import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.SessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindAppBlockingService(
        impl: AccessibilityAppBlockingService,
    ): AppBlockingService

    @Binds
    @Singleton
    abstract fun bindNfcShieldService(
        impl: NfcShieldServiceImpl,
    ): NfcShieldService

    @Binds
    @Singleton
    abstract fun bindNfcManager(
        impl: NfcManagerImpl,
    ): NfcManager

    @Binds
    @Singleton
    abstract fun bindSessionManager(
        impl: SessionManagerImpl,
    ): SessionManager
}