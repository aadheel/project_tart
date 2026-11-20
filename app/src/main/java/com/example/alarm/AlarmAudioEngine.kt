package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class AlarmAudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var fadeJob: Job? = null
    private var synthJob: Job? = null

    private var currentRingtoneUri: String? = null
    private var gradualEnabled: Boolean = true
    private var vibrationEnabled: Boolean = true
    private var isPaused: Boolean = false

    private val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun start(
        ringtoneUri: String?,
        gradualVolume: Boolean,
        vibrate: Boolean
    ) {
        stop()
        currentRingtoneUri = ringtoneUri
        gradualEnabled = gradualVolume
        vibrationEnabled = vibrate
        isPaused = false

        val initialVolume = if (gradualVolume) INITIAL_FADE_VOLUME else 1.0f
        startAudioStream(ringtoneUri, initialVolume)
        if (gradualVolume) {
            startVolumeRamp()
        }
        if (vibrate) {
            startVibration()
        }
    }

    fun pauseForMission() {
        if (isPaused) return
        isPaused = true
        fadeJob?.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) {
                runCatching { it.pause() }
            }
        }
        synthJob?.cancel()
        synthTrack?.let {
            runCatching {
                it.pause()
                it.flush()
            }
        }
        stopVibration()
    }

    fun resumeAtFullVolume() {
        if (!isPaused) return
        isPaused = false
        val player = mediaPlayer
        if (player != null) {
            runCatching {
                player.setVolume(1.0f, 1.0f)
                player.start()
            }.onFailure {
                startAudioStream(currentRingtoneUri, 1.0f)
            }
        } else {
            startSynthesizedPulse(1.0f)
        }
        if (vibrationEnabled) {
            startVibration()
        }
    }

    fun stop() {
        isPaused = false
        fadeJob?.cancel()
        fadeJob = null
        synthJob?.cancel()
        synthJob = null

        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            }
        }
        mediaPlayer = null

        synthTrack?.let { track ->
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
        }
        synthTrack = null

        stopVibration()
    }

    private fun startAudioStream(ringtoneUri: String?, volume: Float) {
        val uri: Uri? = ringtoneUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val startedMediaPlayer = if (uri != null) {
            runCatching {
                val player = MediaPlayer().apply {
                    setAudioAttributes(alarmAudioAttributes)
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    setVolume(volume, volume)
                    start()
                }
                mediaPlayer = player
                true
            }.getOrDefault(false)
        } else {
            false
        }

        if (!startedMediaPlayer) {
            startSynthesizedPulse(volume)
        }
    }

    private fun startSynthesizedPulse(initialVolume: Float) {
        synthJob?.cancel()
        val sampleRate = 44100
        val pulseSamples = generateBraunPulsePcm(sampleRate)
        val bufferBytes = pulseSamples.size * 2

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(alarmAudioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        synthTrack = track
        runCatching {
            track.setVolume(initialVolume)
            track.play()
        }

        synthJob = scope.launch(Dispatchers.IO) {
            while (isActive && !isPaused) {
                runCatching {
                    track.write(pulseSamples, 0, pulseSamples.size)
                }.onFailure {
                    break
                }
            }
        }
    }

    private fun startVolumeRamp() {
        fadeJob?.cancel()
        fadeJob = scope.launch(Dispatchers.Main) {
            val steps = 60
            val stepDelayMs = FADE_DURATION_MS / steps
            for (step in 1..steps) {
                if (!isActive || isPaused) break
                val fraction = step.toFloat() / steps.toFloat()
                val volume = INITIAL_FADE_VOLUME + (1.0f - INITIAL_FADE_VOLUME) * fraction
                runCatching {
                    mediaPlayer?.setVolume(volume, volume)
                    synthTrack?.setVolume(volume)
                }
                delay(stepDelayMs)
            }
        }
    }

    private fun startVibration() {
        val vibrator = resolveVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val timings = longArrayOf(0L, 450L, 350L, 450L, 850L)
        val amplitudes = intArrayOf(0, 210, 0, 255, 0)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, alarmAudioAttributes)
            }
        }
    }

    private fun stopVibration() {
        runCatching { resolveVibrator()?.cancel() }
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Synthesizes a clean Braun-inspired 1-second rhythmic quartz alarm cadence
     * (4 crisp 880Hz pulses with soft envelope + quiet rest) as a guaranteed built-in fallback.
     */
    private fun generateBraunPulsePcm(sampleRate: Int): ShortArray {
        val totalSamples = sampleRate // 1.0 second loop
        val pcm = ShortArray(totalSamples)
        val freqHz = 880.0
        val beepWindows = listOf(
            0.00..0.09,
            0.14..0.23,
            0.28..0.37,
            0.42..0.51
        )
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val activeWindow = beepWindows.firstOrNull { t in it }
            if (activeWindow != null) {
                val localT = t - activeWindow.start
                val duration = activeWindow.endInclusive - activeWindow.start
                val env = when {
                    localT < 0.008 -> localT / 0.008
                    localT > duration - 0.01 -> ((duration - localT) / 0.01).coerceAtLeast(0.0)
                    else -> 1.0
                }
                val sample = sin(2.0 * PI * freqHz * t) * 0.72 * env
                pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            } else {
                pcm[i] = 0
            }
        }
        return pcm
    }

    companion object {
        private const val INITIAL_FADE_VOLUME = 0.10f
        private const val FADE_DURATION_MS = 30_000L
    }
}
