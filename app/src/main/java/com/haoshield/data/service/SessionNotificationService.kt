package com.haoshield.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.haoshield.MainActivity
import com.haoshield.R
import com.haoshield.data.audio.AmbientMusicPlayer
import com.haoshield.domain.model.Session
import com.haoshield.domain.service.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A quiet presence while protected time is running.
 *
 * Two jobs. It shows the session in the shade — a live timer and the intention you named — so a
 * session can't be forgotten and there is always one tap back into it. And by being a foreground
 * service it keeps the process out of the low-memory killer's reach, so ambient sound doesn't stop
 * when you put the phone down, which is exactly when the app wants you to.
 *
 * Deliberately silent: a low-importance channel, no sound, no heads-up, no badge, never
 * re-alerting. The timer uses the platform chronometer, so the notification is posted once and
 * Android counts on its own — no per-second reposts, no battery cost.
 */
@AndroidEntryPoint
class SessionNotificationService : Service() {

    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var ambientMusicPlayer: AmbientMusicPlayer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null

    /** The live session, mirrored here so the notification can be built without suspending. */
    @Volatile private var session: Session? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_AUDIO) {
            if (ambientMusicPlayer.isPlaying()) ambientMusicPlayer.pause() else ambientMusicPlayer.play()
        }

        // A foreground service must post its notification immediately.
        startForegroundCompat(buildNotification())

        if (observer == null) {
            observer = sessionManager.observeSessionState()
                .onEach { state ->
                    val current = state?.session
                    if (current == null) {
                        stopSelf()
                        return@onEach
                    }
                    // Session state re-emits every second as the clock ticks, but the chronometer
                    // runs itself — only rebuild when something the notification actually shows
                    // has changed.
                    val meaningfulChange = current.id != session?.id ||
                        current.intention != session?.intention
                    session = current
                    if (meaningfulChange) refresh()
                }
                .launchIn(scope)
        } else {
            // A toggle arrived while already running — reflect the new audio state.
            refresh()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // This service lives exactly as long as the session, so it — not any screen — is what
        // owns the ambient bed's lifetime. The sound ends when the session does, however the
        // person happened to end it and whatever screen they were on.
        ambientMusicPlayer.stop()
        observer?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun refresh() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val active = session
        val openSession = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_OPEN_SESSION, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleAudio = PendingIntent.getService(
            this,
            1,
            Intent(this, SessionNotificationService::class.java).setAction(ACTION_TOGGLE_AUDIO),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val intention = active?.intention?.takeIf { it.isNotBlank() }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_hao)
            .setContentTitle(getString(R.string.notification_session_title))
            .setContentText(
                intention?.let { getString(R.string.notification_session_for, it) }
                    ?: getString(R.string.notification_session_body),
            )
            .setContentIntent(openSession)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // One control, showing what a press will do. An icon makes it read as a media button
        // rather than another line of text competing with the intention.
        val playing = ambientMusicPlayer.isPlaying()
        builder.addAction(
            if (playing) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
            getString(
                if (playing) R.string.notification_pause_sound else R.string.notification_play_sound,
            ),
            toggleAudio,
        )

        // The platform counts up from the session's start — we post once, Android does the rest.
        active?.let { builder.setWhen(it.startedAtEpochMillis).setUsesChronometer(true) }
        return builder.build()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_session),
            // LOW: present in the shade, but never a sound, a peek, or a badge.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_session_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "hao_session"
        private const val NOTIFICATION_ID = 1
        const val ACTION_TOGGLE_AUDIO = "com.haoshield.action.TOGGLE_AUDIO"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SessionNotificationService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionNotificationService::class.java))
        }
    }
}
