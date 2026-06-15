package com.haoshield.data.audio

import android.content.Context
import android.media.MediaPlayer
import com.haoshield.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AmbientMusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackIndex: Int = Random.nextInt(DEFAULT_TRACKS.size)

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun play() {
        val player = mediaPlayer
        if (player == null) {
            startTrack(currentTrackIndex)
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

    private fun startTrack(index: Int) {
        stop()
        currentTrackIndex = index

        val player = MediaPlayer.create(context, DEFAULT_TRACKS[index]) ?: return
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
        const val VOLUME = 0.22f

        val DEFAULT_TRACKS = listOf(
            R.raw.ambient_stillness,
            R.raw.ambient_breath,
            R.raw.ambient_mist,
            R.raw.ambient_soft_earth,
        )
    }
}