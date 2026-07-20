package com.haoshield.di

import com.haoshield.data.service.AccessibilityAppBlockingService
import com.haoshield.data.service.AppBlockingService
import com.haoshield.data.nfc.NfcManagerImpl
import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.data.shield.ShieldScanHandlerImpl
import com.haoshield.data.shield.ShieldTokenStoreImpl
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.domain.service.ShieldTokenStore
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
    abstract fun bindShieldTokenStore(
        impl: ShieldTokenStoreImpl,
    ): ShieldTokenStore

    @Binds
    @Singleton
    abstract fun bindShieldScanHandler(
        impl: ShieldScanHandlerImpl,
    ): ShieldScanHandler

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
