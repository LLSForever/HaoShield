package com.haoshield.data.audio

import android.content.Context
import android.media.MediaPlayer
import com.haoshield.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmbientMusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun play() {
        val player = mediaPlayer
        if (player == null) {
            startTrack()
            return
        }

        if (!player.isPlaying) {
            runCatching { player.start() }
        }
    }

    fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun stop() {
        mediaPlayer?.run {
            runCatching {
                stop()
                release()
            }
        }
        mediaPlayer = null
    }

    private fun startTrack() {
        stop()

        val player = MediaPlayer.create(context, AMBIENT_TRACK) ?: return
        player.setVolume(VOLUME, VOLUME)
        player.isLooping = true
        player.setOnErrorListener { mp, _, _ ->
            mp.release()
            mediaPlayer = null
            true
        }
        player.start()
        mediaPlayer = player
    }

    private companion object {
        /** Quiet enough to sit under a room, not fill it. */
        const val VOLUME = 0.22f

        val AMBIENT_TRACK = R.raw.deep_rest
    }
}