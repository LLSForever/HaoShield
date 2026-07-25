package com.haoshield

import android.app.Application
import com.haoshield.data.service.SessionNotificationService
import com.haoshield.di.ApplicationScope
import com.haoshield.domain.service.SessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltAndroidApp
class HaoShieldApplication : Application() {

    @Inject lateinit var sessionManager: SessionManager

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        // The session notification's lifetime is the session's. Owned here rather than by any one
        // screen, because a session can start or end from a shield tap on any screen at all.
        sessionManager.observeSessionState()
            .map { it != null }
            .distinctUntilChanged()
            .onEach { active ->
                if (active) {
                    SessionNotificationService.start(this)
                } else {
                    SessionNotificationService.stop(this)
                }
            }
            .launchIn(applicationScope)
    }
}
