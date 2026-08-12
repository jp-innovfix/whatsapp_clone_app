package com.jp.whatsappclone.ui.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
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
        release()
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
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
        recorder = mediaRecorder
    }.isSuccess

    fun pause(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return runCatching { recorder?.pause() }.isSuccess
    }

    fun resume(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return runCatching { recorder?.resume() }.isSuccess
    }

    fun stop(keepFile: Boolean): String? {
        val file = outputFile
        val stopped = runCatching { recorder?.stop() }.isSuccess
        release()
        if (!keepFile || !stopped || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }
        return file.absolutePath
    }

    fun cancel() {
        stop(keepFile = false)
    }

    private fun release() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile = null
    }
}

class VoicePlaybackController {
    var playingMessageId by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 45)

    fun toggle(message: MessageUi, scope: CoroutineScope) {
        if (playingMessageId == message.id) {
            stop(resetProgress = false)
            return
        }
        stop(resetProgress = true)
        playingMessageId = message.id

        val path = message.audioPath
        if (path != null && File(path).exists()) {
            runCatching {
                MediaPlayer().also { player ->
                    mediaPlayer = player
                    player.setDataSource(path)
                    player.prepare()
                    player.setOnCompletionListener {
                        playingMessageId = null
                        progress = 1f
                        it.release()
                        mediaPlayer = null
                    }
                    player.start()
                    playbackJob = scope.launch {
                        while (isActive && player.isPlaying) {
                            progress = if (player.duration > 0) player.currentPosition / player.duration.toFloat() else 0f
                            delay(80)
                        }
                    }
                }
            }.onFailure { startDemoPlayback(message, scope) }
        } else {
            startDemoPlayback(message, scope)
        }
    }

    private fun startDemoPlayback(message: MessageUi, scope: CoroutineScope) {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 220)
        val duration = ((message.voiceSeconds ?: 1).coerceAtLeast(1) * 1000L)
        playbackJob = scope.launch {
            val startedAt = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                progress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                if (elapsed >= duration) break
                delay(80)
            }
            progress = 1f
            playingMessageId = null
        }
    }

    fun stop(resetProgress: Boolean = true) {
        playbackJob?.cancel()
        playbackJob = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingMessageId = null
        if (resetProgress) progress = 0f
    }

    fun release() {
        stop()
        tone.release()
    }
}
