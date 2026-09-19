package com.nothatcher.sproutbook

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundMachine {
    private var track: AudioTrack? = null
    @Volatile private var running = false
    private var worker: Thread? = null

    fun play(kind: String) {
        stop()
        val sampleRate = 22050
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
            .setBufferSizeInBytes(minBuffer * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track = audioTrack
        running = true
        audioTrack.play()

        worker = thread(name = "SproutSound", isDaemon = true) {
            val chunk = ShortArray(2048)
            var brown = 0.0
            var phase = 0.0
            val rng = Random(System.nanoTime())
            while (running) {
                for (i in chunk.indices) {
                    val white = rng.nextDouble(-1.0, 1.0)
                    val sample = when (kind) {
                        "Brown noise" -> {
                            brown = (brown + white * 0.035).coerceIn(-1.0, 1.0) * 0.995
                            brown * 0.42
                        }
                        "Fan" -> {
                            phase += 2.0 * PI * 72.0 / sampleRate
                            (sin(phase) * 0.18) + (white * 0.08)
                        }
                        "Rain" -> {
                            val drop = if (rng.nextInt(120) == 0) rng.nextDouble(0.35, 0.8) else 0.0
                            white * 0.16 + drop
                        }
                        else -> white * 0.20
                    }
                    chunk[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                val current = track ?: break
                current.write(chunk, 0, chunk.size)
            }
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
    }
}
