package com.zeno.dialer.data

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Thin wrapper around a single [MediaPlayer] instance for voicemail playback — only one
 * voicemail plays at a time; starting a new one stops whatever was previously playing.
 */
class VoicemailPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun play(
        scope: CoroutineScope,
        uri: Uri,
        onProgress: (positionMs: Int, durationMs: Int) -> Unit,
        onComplete: () -> Unit,
    ) {
        stop()
        val player = try {
            MediaPlayer.create(context, uri) ?: return
        } catch (_: Exception) {
            return
        }
        mediaPlayer = player
        player.setOnCompletionListener {
            onComplete()
            stop()
        }
        player.start()
        progressJob = scope.launch {
            while (true) {
                val p = mediaPlayer ?: break
                onProgress(p.currentPosition, p.duration)
                delay(200L)
            }
        }
    }

    fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        progressJob?.cancel()
        progressJob = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.let {
            try { it.stop() } catch (_: Exception) { }
            it.release()
        }
        mediaPlayer = null
    }

    fun release() = stop()
}
