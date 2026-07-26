package com.haoshield.di

import com.haoshield.data.blocking.AndroidBlockedAppPresets
import com.haoshield.data.blocking.StrictBlockingController
import com.haoshield.data.local.BlockedAppsDataStore
import com.haoshield.data.local.SessionPreferencesDataStore
import com.haoshield.data.local.SettingsPreferencesDataStore
import com.haoshield.data.local.ShieldPreferencesDataStore
import com.haoshield.data.nfc.NfcManagerImpl
import com.haoshield.data.service.AccessibilityAppBlockingService
import com.haoshield.data.service.AppBlockingService
import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.data.shield.ShieldScanHandlerImpl
import com.haoshield.data.shield.ShieldTokenStoreImpl
import com.haoshield.domain.service.BlockedAppPresets
import com.haoshield.domain.service.BlockedAppsStore
import com.haoshield.domain.service.Clock
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.SessionStore
import com.haoshield.domain.service.SettingsStore
import com.haoshield.domain.service.ShieldPreferences
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.domain.service.ShieldTokenStore
import com.haoshield.domain.service.StrictBlocking
import com.haoshield.domain.service.SystemClock
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

    // The session rules depend on these as interfaces, so they can be driven by in-memory fakes
    // in tests and by a different platform's implementations later.

    @Binds
    @Singleton
    abstract fun bindSessionStore(
        impl: SessionPreferencesDataStore,
    ): SessionStore

    @Binds
    @Singleton
    abstract fun bindStrictBlocking(
        impl: StrictBlockingController,
    ): StrictBlocking

    @Binds
    @Singleton
    abstract fun bindClock(
        impl: SystemClock,
    ): Clock

    // The remaining stores, bound the same way, so the repositories that read them could be lifted
    // out of the Android module alongside the session rules.

    @Binds
    @Singleton
    abstract fun bindBlockedAppsStore(
        impl: BlockedAppsDataStore,
    ): BlockedAppsStore

    @Binds
    @Singleton
    abstract fun bindSettingsStore(
        impl: SettingsPreferencesDataStore,
    ): SettingsStore

    @Binds
    @Singleton
    abstract fun bindShieldPreferences(
        impl: ShieldPreferencesDataStore,
    ): ShieldPreferences

    @Binds
    @Singleton
    abstract fun bindBlockedAppPresets(
        impl: AndroidBlockedAppPresets,
    ): BlockedAppPresets
}
