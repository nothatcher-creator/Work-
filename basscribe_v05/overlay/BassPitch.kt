package com.github.sevagh.demucs_android

import android.content.Context
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Real on-device Basic Pitch inference. Notes come from the neural model; fret choice and
 * articulation labels are bass-specific post-processing. */
class BasicPitchTranscriber(private val ctx: Context) : Closeable {
    companion object {
        const val SR = 22050
        const val N_SAMPLES = 43844
        const val N_FRAMES = 172
        const val N_NOTES = 88
        const val FFT_HOP = 256
        const val OVERLAP = 30 * FFT_HOP
        const val HOP = N_SAMPLES - OVERLAP
        const val MIDI_OFFSET = 21
        const val FRAME_SEC = FFT_HOP.toDouble() / SR
    }

    data class NeuralNote(
        val startSec: Double,
        val endSec: Double,
        val midi: Int,
        val confidence: Float,
        val onsetStrength: Float
    )

    private val model: CompiledModel
    private val inBuf: List<com.google.ai.edge.litert.TensorBuffer>
    private val outBuf: List<com.google.ai.edge.litert.TensorBuffer>

    init {
        val modelFile = File(ctx.filesDir, "basic_pitch.tflite")
        if (!modelFile.exists() || modelFile.length() < 100_000L) {
            ctx.assets.open("basic_pitch.tflite").use { input ->
                modelFile.outputStream().use { input.copyTo(it) }
            }
        }
        model = try {
            CompiledModel.create(modelFile.absolutePath, CompiledModel.Options(Accelerator.GPU), null)
        } catch (_: Throwable) {
            CompiledModel.create(modelFile.absolutePath, CompiledModel.Options(Accelerator.CPU), null)
        }
        inBuf = model.createInputBuffers()
        outBuf = model.createOutputBuffers()
    }

    fun transcribe(pcm: FloatArray, onProgress: (Int, Int) -> Unit): List<NeuralNote> {
        val nWin = if (pcm.size <= N_SAMPLES) 1 else 1 + ((pcm.size - N_SAMPLES) + HOP - 1) / HOP
        val framesTotal = (pcm.size.toDouble() / FFT_HOP).toInt() + 1
        val notes = Array(framesTotal) { FloatArray(N_NOTES) }
        val onsets = Array(framesTotal) { FloatArray(N_NOTES) }
        val x = FloatArray(N_SAMPLES)

        for (w in 0 until nWin) {
            onProgress(w + 1, nWin)
            val start = w * HOP
            java.util.Arrays.fill(x, 0f)
            val n = min(N_SAMPLES, pcm.size - start)
            if (n > 0) System.arraycopy(pcm, start, x, 0, n)
            inBuf[0].writeFloat(x)
            model.run(inBuf, outBuf)
            val noteW = outBuf[1].readFloat()
            val onsetW = outBuf[2].readFloat()
            val globalFrame = start / FFT_HOP
            val keepFrom = if (w == 0) 0 else OVERLAP / FFT_HOP / 2
            val keepTo = if (w == nWin - 1) N_FRAMES else N_FRAMES - OVERLAP / FFT_HOP / 2
            for (t in keepFrom until keepTo) {
                val g = globalFrame + t
                if (g >= framesTotal) break
                val src = t * N_NOTES
                for (k in 0 until N_NOTES) {
                    notes[g][k] = max(notes[g][k], noteW[src + k])
                    onsets[g][k] = max(onsets[g][k], onsetW[src + k])
                }
            }
        }
        return decode(notes, onsets)
    }

    private fun decode(note: Array<FloatArray>, onset: Array<FloatArray>): List<NeuralNote> {
        val onsetTh = 0.42f
        val frameTh = 0.26f
        val minFrames = 3
        val active = IntArray(N_NOTES) { -1 }
        val peak = FloatArray(N_NOTES)
        val onsetPeak = FloatArray(N_NOTES)
        val events = ArrayList<NeuralNote>()

        for (t in note.indices) {
            for (k in 0 until N_NOTES) {
                if (active[k] < 0) {
                    if (onset[t][k] >= onsetTh && note[t][k] >= frameTh) {
                        active[k] = t
                        peak[k] = note[t][k]
                        onsetPeak[k] = onset[t][k]
                    }
                } else {
                    peak[k] = max(peak[k], note[t][k])
                    onsetPeak[k] = max(onsetPeak[k], onset[t][k])
                    if (note[t][k] < frameTh) {
                        val s = active[k]
                        if (t - s >= minFrames) {
                            events.add(NeuralNote(s * FRAME_SEC, t * FRAME_SEC, k + MIDI_OFFSET, peak[k], onsetPeak[k]))
                        }
                        active[k] = -1
                    }
                }
            }
        }
        for (k in 0 until N_NOTES) {
            val s = active[k]
            if (s >= 0 && note.size - s >= minFrames) {
                events.add(NeuralNote(s * FRAME_SEC, note.size * FRAME_SEC, k + MIDI_OFFSET, peak[k], onsetPeak[k]))
            }
        }
        return events.sortedBy { it.startSec }
    }

    override fun close() {
        inBuf.forEach { it.close() }
        outBuf.forEach { it.close() }
        model.close()
    }
}

data class TabNote(
    val startSec: Double,
    val endSec: Double,
    val midi: Int,
    val stringIndex: Int,
    val fret: Int,
    val confidence: Float,
    val technique: String = ""
)

object BassTabEngine {
    private val openMidi = intArrayOf(28, 33, 38, 43) // E1 A1 D2 G2

    fun makeTab(raw: List<BasicPitchTranscriber.NeuralNote>): List<TabNote> {
        val filtered = raw.filter {
            it.midi in 28..67 && it.endSec - it.startSec >= 0.045 && it.confidence >= 0.20f
        }.sortedBy { it.startSec }
        if (filtered.isEmpty()) return emptyList()

        // Bass is effectively monophonic after stem separation. Collapse near-simultaneous harmonic
        // detections, with a mild fundamental preference when confidences are close.
        val collapsed = ArrayList<BasicPitchTranscriber.NeuralNote>()
        var i = 0
        while (i < filtered.size) {
            val group = ArrayList<BasicPitchTranscriber.NeuralNote>()
            val base = filtered[i].startSec
            var j = i
            while (j < filtered.size && filtered[j].startSec - base <= 0.055) {
                group.add(filtered[j]); j++
            }
            val strongest = group.maxByOrNull { it.confidence }!!
            val fundamentalCandidate = group.filter { it.confidence >= strongest.confidence * 0.76f }.minByOrNull { it.midi }
            collapsed.add(fundamentalCandidate ?: strongest)
            i = j
        }

        // Merge duplicate notes and remove obvious octave-harmonic ghosts.
        val mono = ArrayList<BasicPitchTranscriber.NeuralNote>()
        for (n in collapsed) {
            if (mono.isEmpty()) { mono.add(n); continue }
            val p = mono.last()
            if (n.midi == p.midi && n.startSec <= p.endSec + 0.085) {
                mono[mono.lastIndex] = p.copy(
                    endSec = max(p.endSec, n.endSec),
                    confidence = max(p.confidence, n.confidence),
                    onsetStrength = max(p.onsetStrength, n.onsetStrength)
                )
                continue
            }
            if (abs(n.midi - p.midi) == 12 && n.startSec < p.endSec - 0.02) {
                val lower = if (n.midi < p.midi) n else p
                val higher = if (n.midi > p.midi) n else p
                if (higher.confidence <= lower.confidence * 1.35f) {
                    if (lower === n) mono[mono.lastIndex] = n
                    continue
                }
            }
            if (n.startSec < p.endSec - 0.025) {
                if (n.startSec - p.startSec >= 0.065) {
                    mono[mono.lastIndex] = p.copy(endSec = max(p.startSec + 0.05, n.startSec))
                    mono.add(n)
                } else if (n.confidence > p.confidence * 1.2f) {
                    mono[mono.lastIndex] = n
                }
            } else mono.add(n)
        }

        val positions = choosePositions(mono)
        val out = ArrayList<TabNote>(mono.size)
        for (idx in mono.indices) {
            val n = mono[idx]
            val pos = positions[idx]
            var tech = ""
            if (idx > 0) {
                val prev = out[idx - 1]
                val gap = n.startSec - prev.endSec
                val d = pos.second - prev.fret
                if (pos.first == prev.stringIndex && gap <= 0.080 && abs(d) in 1..7) {
                    val weakAttack = n.onsetStrength < 0.64f
                    tech = when {
                        abs(d) >= 4 && weakAttack -> if (d > 0) "/" else "\\"
                        d > 0 && abs(d) <= 3 && weakAttack -> "h"
                        d < 0 && abs(d) <= 3 && weakAttack -> "p"
                        else -> ""
                    }
                }
            }
            out.add(TabNote(n.startSec, n.endSec, n.midi, pos.first, pos.second, n.confidence, tech))
        }
        return out
    }

    private fun candidates(midi: Int): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (s in 0..3) {
            val fret = midi - openMidi[s]
            if (fret in 0..24) out.add(Pair(s, fret))
        }
        return out
    }

    private fun choosePositions(notes: List<BasicPitchTranscriber.NeuralNote>): List<Pair<Int, Int>> {
        if (notes.isEmpty()) return emptyList()
        val cs = notes.map { candidates(it.midi) }
        val costs = cs.map { DoubleArray(it.size) { Double.POSITIVE_INFINITY } }
        val prev = cs.map { IntArray(it.size) { -1 } }
        for (j in cs[0].indices) {
            val (_, fret) = cs[0][j]
            costs[0][j] = fret * 0.035 + if (fret == 0) -0.10 else 0.0
        }
        for (i in 1 until notes.size) {
            for (j in cs[i].indices) {
                val (s, f) = cs[i][j]
                var best = Double.POSITIVE_INFINITY
                var bestK = -1
                for (k in cs[i - 1].indices) {
                    val (ps, pf) = cs[i - 1][k]
                    val fretJump = abs(f - pf).toDouble()
                    val stringJump = abs(s - ps).toDouble()
                    val gap = notes[i].startSec - notes[i - 1].endSec
                    var c = costs[i - 1][k] + fretJump * 0.20 + stringJump * 0.36 + f * 0.018
                    if (gap < 0.12) c += fretJump * 0.14
                    if (fret == 0) c -= 0.05
                    if (s == ps) c -= 0.05
                    if (c < best) { best = c; bestK = k }
                }
                costs[i][j] = best
                prev[i][j] = bestK
            }
        }
        var j = costs.last().indices.minByOrNull { costs.last()[it] } ?: 0
        val result = MutableList(notes.size) { Pair(0, 0) }
        for (i in notes.lastIndex downTo 0) {
            result[i] = cs[i][j]
            j = if (i > 0) max(0, prev[i][j]) else 0
        }
        return result
    }
}

object WavBassDecoder {
    fun readMono22050(file: File): FloatArray {
        val data = file.readBytes()
        require(data.size >= 44 && String(data, 0, 4, Charsets.US_ASCII) == "RIFF") { "Invalid WAV" }
        fun u16(o: Int) = (data[o].toInt() and 255) or ((data[o + 1].toInt() and 255) shl 8)
        fun i32(o: Int) = (data[o].toInt() and 255) or ((data[o + 1].toInt() and 255) shl 8) or
            ((data[o + 2].toInt() and 255) shl 16) or (data[o + 3].toInt() shl 24)

        var fmt = 1; var channels = 2; var sr = 44100; var bits = 16
        var dataOff = -1; var dataLen = 0; var p = 12
        while (p + 8 <= data.size) {
            val id = String(data, p, 4, Charsets.US_ASCII)
            val len = i32(p + 4).coerceAtLeast(0)
            val body = p + 8
            if (id == "fmt " && body + 16 <= data.size) {
                fmt = u16(body); channels = u16(body + 2); sr = i32(body + 4); bits = u16(body + 14)
            } else if (id == "data") { dataOff = body; dataLen = min(len, data.size - body); break }
            p = body + len + (len and 1)
        }
        require(dataOff >= 0 && channels > 0) { "WAV data chunk missing" }
        val bytesPerSample = max(1, bits / 8)
        val frameBytes = bytesPerSample * channels
        val frames = dataLen / frameBytes
        val mono = FloatArray(frames)
        var off = dataOff
        for (f in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                val sample = when {
                    fmt == 3 && bits == 32 -> Float.fromBits(i32(off))
                    fmt == 1 && bits == 16 -> {
                        val v = u16(off)
                        val signed = if (v >= 32768) v - 65536 else v
                        signed / 32768f
                    }
                    fmt == 1 && bits == 24 -> {
                        var v = (data[off].toInt() and 255) or ((data[off + 1].toInt() and 255) shl 8) or ((data[off + 2].toInt() and 255) shl 16)
                        if ((v and 0x800000) != 0) v = v or -0x1000000
                        v / 8388608f
                    }
                    fmt == 1 && bits == 32 -> i32(off) / 2147483648f
                    else -> throw IllegalArgumentException("Unsupported WAV format $fmt/$bits")
                }
                if (sample.isFinite()) sum += sample
                off += bytesPerSample
            }
            mono[f] = (sum / channels).coerceIn(-1f, 1f)
        }
        if (sr == 22050) return mono
        val outN = floor(mono.size.toDouble() * 22050.0 / sr).toInt().coerceAtLeast(1)
        val out = FloatArray(outN)
        val ratio = sr.toDouble() / 22050.0
        for (i in out.indices) {
            val x = i * ratio
            val a = floor(x).toInt().coerceIn(0, mono.lastIndex)
            val b = min(a + 1, mono.lastIndex)
            val t = (x - a).toFloat()
            out[i] = mono[a] * (1f - t) + mono[b] * t
        }
        return out
    }
}
