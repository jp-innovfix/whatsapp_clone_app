package com.jp.whatsappclone.ui.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jp.whatsappclone.data.MessageUi
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): Boolean = runCatching {
        cancel()
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        instance.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        outputFile = file
        recorder = instance
    }.isSuccess

    fun pause(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        runCatching { requireNotNull(recorder).pause() }.isSuccess

    fun resume(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        runCatching { requireNotNull(recorder).resume() }.isSuccess

    fun stop(keepFile: Boolean): File? {
        val file = outputFile
        val stopped = runCatching { recorder?.stop() }.isSuccess
        releaseRecorder()
        if (!keepFile || !stopped || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }
        return file
    }

    fun cancel() { stop(false) }

    private fun releaseRecorder() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile = null
    }
}

class VoicePlaybackController(private val context: Context) {
    var activeMessageId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var speed by mutableFloatStateOf(1f)
        private set

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null
    private var demoJob: Job? = null
    private var demoElapsedMillis = 0f
    private var demoDurationMillis = 0L
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 35)

    fun toggle(message: MessageUi, sourceUri: String, scope: CoroutineScope) {
        if (activeMessageId == message.id && player != null) {
            val current = player ?: return
            if (current.isPlaying) {
                current.pause()
                isPlaying = false
            } else {
                current.start()
                isPlaying = true
                monitor(scope)
            }
            return
        }
        if (activeMessageId == message.id && demoJob != null) {
            isPlaying = !isPlaying
            if (isPlaying) tone.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            return
        }
        stop()
        activeMessageId = message.id
        runCatching {
            MediaPlayer().also { mediaPlayer ->
                player = mediaPlayer
                mediaPlayer.setDataSource(context, Uri.parse(sourceUri))
                mediaPlayer.prepare()
                applySpeed(mediaPlayer)
                mediaPlayer.setOnCompletionListener {
                    progress = 1f
                    isPlaying = false
                    activeMessageId = null
                    it.release()
                    player = null
                }
                mediaPlayer.start()
                isPlaying = true
                monitor(scope)
            }
        }.onFailure {
            runCatching { player?.release() }
            player = null
            startDemo(message, scope)
        }
    }

    fun cycleSpeed() {
        speed = when (speed) {
            1f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
        player?.let(::applySpeed)
    }

    private fun applySpeed(mediaPlayer: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { mediaPlayer.playbackParams = PlaybackParams().setSpeed(speed) }
        }
    }

    private fun monitor(scope: CoroutineScope) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = player ?: break
                if (current.duration > 0) progress = current.currentPosition / current.duration.toFloat()
                delay(70)
            }
        }
    }

    private fun startDemo(message: MessageUi, scope: CoroutineScope) {
        player = null
        isPlaying = true
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        demoElapsedMillis = 0f
        demoDurationMillis = ((message.voiceSeconds ?: 1).coerceAtLeast(1) * 1_000L)
        demoJob = scope.launch {
            while (isActive) {
                if (isPlaying) {
                    demoElapsedMillis += 70f * speed
                    progress = (demoElapsedMillis / demoDurationMillis.toFloat()).coerceIn(0f, 1f)
                    if (demoElapsedMillis >= demoDurationMillis) break
                }
                delay(70)
            }
            if (isActive) {
                progress = 1f
                isPlaying = false
                activeMessageId = null
                demoJob = null
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        demoJob?.cancel()
        progressJob = null
        demoJob = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        activeMessageId = null
        isPlaying = false
        progress = 0f
        speed = 1f
        demoElapsedMillis = 0f
        demoDurationMillis = 0L
    }

    fun release() {
        stop()
        tone.release()
    }
}
