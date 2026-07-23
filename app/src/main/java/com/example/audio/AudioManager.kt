package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.save.SaveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioManager(context: Context, private val saveManager: SaveManager) {

    enum class SoundType {
        PADDLE_HIT,
        BRICK_BREAK,
        POWERUP,
        EXPLOSION,
        DEATH,
        VICTORY,
        LASER
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var musicJob: kotlinx.coroutines.Job? = null
    
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        // Start background music loop if enabled
        startMusic()
    }

    private fun triggerVibration(type: SoundType) {
        if (!saveManager.isVibeEnabled()) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    SoundType.PADDLE_HIT -> VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE)
                    SoundType.BRICK_BREAK -> VibrationEffect.createOneShot(35L, 180)
                    SoundType.LASER -> VibrationEffect.createOneShot(12L, VibrationEffect.DEFAULT_AMPLITUDE)
                    SoundType.EXPLOSION -> VibrationEffect.createOneShot(150L, 255)
                    SoundType.POWERUP -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40), intArrayOf(0, 255, 0, 255), -1)
                    SoundType.DEATH -> VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 200), intArrayOf(0, 255, 0, 255), -1)
                    SoundType.VICTORY -> VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50, 50, 100), intArrayOf(0, 255, 0, 255, 0, 255), -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    SoundType.PADDLE_HIT -> vib.vibrate(25L)
                    SoundType.BRICK_BREAK -> vib.vibrate(35L)
                    SoundType.LASER -> vib.vibrate(12L)
                    SoundType.EXPLOSION -> vib.vibrate(150L)
                    SoundType.POWERUP -> vib.vibrate(longArrayOf(0, 30, 60, 40), -1)
                    SoundType.DEATH -> vib.vibrate(longArrayOf(0, 100, 80, 200), -1)
                    SoundType.VICTORY -> vib.vibrate(longArrayOf(0, 50, 50, 50, 50, 100), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSfx(type: SoundType) {
        triggerVibration(type)

        if (!saveManager.isSoundEnabled()) return

        scope.launch {
            try {
                when (type) {
                    SoundType.PADDLE_HIT -> playTone(600f, 800f, 80) // Short upward blip
                    SoundType.BRICK_BREAK -> playTone(300f, 150f, 100, true) // Descending rumble
                    SoundType.POWERUP -> playPowerUpTone() // Ascending scale
                    SoundType.EXPLOSION -> playTone(150f, 50f, 250, true) // Heavy bass explosion noise
                    SoundType.DEATH -> playDeathTone() // Sad sliding down tone
                    SoundType.VICTORY -> playVictoryMelody() // Nice upbeat melody
                    SoundType.LASER -> playTone(1200f, 400f, 70) // Fast laser sweep
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    fun startMusic() {
        if (!saveManager.isMusicEnabled()) {
            stopMusic()
            return
        }
        if (musicJob != null && musicJob?.isActive == true) return

        musicJob = scope.launch {
            // Progression: C - G - Am - F
            val progressions = listOf(
                // C major chord arpeggio (C3, G3, C4, E4)
                listOf(130.81f, 196.00f, 261.63f, 329.63f),
                // G major chord arpeggio (G2, D3, G3, B3)
                listOf(98.00f, 146.83f, 196.00f, 246.94f),
                // A minor chord arpeggio (A2, E3, A3, C4)
                listOf(110.00f, 164.81f, 220.00f, 261.63f),
                // F major chord arpeggio (F2, C3, F3, A3)
                listOf(87.31f, 130.81f, 174.61f, 220.00f)
            )

            while (true) {
                for (chord in progressions) {
                    for (note in chord) {
                        if (!saveManager.isMusicEnabled()) {
                            stopMusic()
                            return@launch
                        }
                        playMusicNote(note, 200)
                        kotlinx.coroutines.delay(240L)
                    }
                }
            }
        }
    }

    @Synchronized
    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun setMusicEnabled(enabled: Boolean) {
        if (enabled) {
            startMusic()
        } else {
            stopMusic()
        }
    }

    private fun playMusicNote(freq: Float, durationMs: Int, volume: Float = 0.08f) {
        if (!saveManager.isMusicEnabled()) return

        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val generatedSfx = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val angle = 2.0 * Math.PI * i / (sampleRate / freq)
            
            // Volume envelope with fade-in and fade-out to prevent popping
            val fadeVal = if (t < 0.1f) t / 0.1f else if (t > 0.8f) (1f - t) / 0.2f else 1f
            val value = (sin(angle) * 32767 * volume * fadeVal).toInt().toShort()

            generatedSfx[2 * i] = (value.toInt() and 0x00ff).toByte()
            generatedSfx[2 * i + 1] = ((value.toInt() and 0xff00) ushr 8).toByte()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSfx.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSfx, 0, generatedSfx.size)
            audioTrack.play()
            
            scope.launch {
                kotlinx.coroutines.delay(durationMs + 50L)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playTone(startFreq: Float, endFreq: Float, durationMs: Int, useNoise: Boolean = false) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val generatedSfx = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * t
            val angle = 2.0 * Math.PI * i / (sampleRate / currentFreq)
            
            val value = if (useNoise) {
                // Generate raw noise mixed with frequency
                val noise = (Math.random() * 2.0 - 1.0) * 0.4
                val wave = sin(angle) * 0.6
                ((wave + noise) * 32767).toInt().toShort()
            } else {
                // Pure sine wave with volume decay
                val fadeVal = if (t > 0.7f) (1f - t) / 0.3f else 1f
                (sin(angle) * 32767 * fadeVal).toInt().toShort()
            }

            // High byte and low byte
            generatedSfx[2 * i] = (value.toInt() and 0x00ff).toByte()
            generatedSfx[2 * i + 1] = ((value.toInt() and 0xff00) ushr 8).toByte()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSfx.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(generatedSfx, 0, generatedSfx.size)
        audioTrack.play()
        
        // Clean up after playing
        scope.launch {
            kotlinx.coroutines.delay(durationMs.toLong() + 50L)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun playPowerUpTone() {
        val notes = floatArrayOf(440f, 554f, 659f, 880f) // A major chord arpeggio
        for (i in notes.indices) {
            playTone(notes[i], notes[i] + 50f, 80)
            Thread.sleep(70)
        }
    }

    private fun playDeathTone() {
        playTone(350f, 100f, 300)
    }

    private fun playVictoryMelody() {
        val melody = floatArrayOf(523f, 587f, 659f, 698f, 784f, 880f, 987f, 1047f) // C major scale up
        val timing = intArrayOf(100, 100, 100, 100, 100, 100, 100, 250)
        for (i in melody.indices) {
            playTone(melody[i], melody[i], timing[i])
            Thread.sleep(timing[i].toLong() - 20L)
        }
    }
}
