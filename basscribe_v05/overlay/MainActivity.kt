package com.github.sevagh.demucs_android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private var selectedUri: Uri? = null
    private var selectedLocalFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var chooseButton: Button
    private lateinit var transcribeButton: Button
    private lateinit var playButton: Button
    private lateinit var seek: SeekBar
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tabView: BassTabView
    private lateinit var tabScroll: ScrollView

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectAudio(it) }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val p = intent?.getFloatExtra("EXTRA_PROGRESS", -1f) ?: -1f
            val msg = intent?.getStringExtra("EXTRA_MESSAGE") ?: ""
            if (p >= 0f) progress.progress = (p.coerceIn(0f, 1f) * 1000).toInt()
            if (msg.isNotBlank()) status.text = cleanProgress(msg)
        }
    }

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val stems = intent?.getStringArrayExtra("writtenStems") ?: emptyArray()
            val bass = stems.map(::File).firstOrNull { it.name.equals("bass.wav", true) || it.name.contains("bass", true) }
            if (bass == null || !bass.exists()) {
                status.text = "Bass separation failed. Try WAV, MP3, FLAC, or another audio file."
                progress.progress = 0
                transcribeButton.isEnabled = true
                return
            }
            stems.map(::File).filter { it.absolutePath != bass.absolutePath }.forEach { runCatching { it.delete() } }
            status.text = "Bass isolated. Running Basic Pitch neural transcription…"
            progress.progress = 760
            transcribeBass(bass)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 11, 16)
        window.navigationBarColor = Color.rgb(8, 11, 16)
        buildUi()

        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, IntentFilter("ACTION_DEMIX_PROGRESS_UPDATE"))
        LocalBroadcastManager.getInstance(this).registerReceiver(completionReceiver, IntentFilter("ACTION_DEMIX_JOB_COMPLETED"))

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 11, 16))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val title = TextView(this).apply {
            text = "BASSCRIBE"
            textSize = 25f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
        val sub = TextView(this).apply {
            text = "Neural bass transcription  •  Demucs → Basic Pitch"
            textSize = 12f
            setTextColor(Color.rgb(145, 158, 179))
            setPadding(0, 0, 0, dp(10))
        }
        root.addView(title)
        root.addView(sub)

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        chooseButton = button("Choose audio") { pickAudio.launch("audio/*") }
        transcribeButton = button("Transcribe") { startTranscription() }.apply { isEnabled = false }
        controls.addView(chooseButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(7) })
        controls.addView(transcribeButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(7) })
        root.addView(controls)

        status = TextView(this).apply {
            text = "Choose a song to begin."
            textSize = 13f
            setTextColor(Color.rgb(210, 217, 228))
            setPadding(dp(2), dp(11), dp(2), dp(5))
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000; progress = 0 }
        root.addView(status)
        root.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5)))

        tabScroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(11, 15, 21))
        }
        tabView = BassTabView(this)
        tabScroll.addView(tabView, ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT))
        root.addView(tabScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10) })

        val transport = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        playButton = button("▶") { togglePlayback() }.apply { isEnabled = false; textSize = 20f }
        seek = SeekBar(this).apply {
            max = 1000
            progress = 0
            isEnabled = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) mediaPlayer?.let { mp ->
                        val pos = ((value / 1000.0) * mp.duration).toInt().coerceIn(0, max(0, mp.duration))
                        mp.seekTo(pos)
                        tabView.setPlayhead(pos / 1000.0)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        transport.addView(playButton, LinearLayout.LayoutParams(dp(62), dp(46)))
        transport.addView(seek, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) })
        root.addView(transport)

        setContentView(root)
    }

    private fun button(label: String, click: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(31, 40, 53))
        setOnClickListener { click() }
    }

    private fun selectAudio(uri: Uri) {
        selectedUri = uri
        selectedLocalFile = null
        status.text = "Selected: ${displayName(uri)}"
        transcribeButton.isEnabled = true
        setupPlayer(uri)
        tabView.setTab(emptyList(), mediaPlayer?.duration?.div(1000.0) ?: 1.0)
    }

    private fun setupPlayer(uri: Uri) {
        runCatching { mediaPlayer?.release() }
        mediaPlayer = runCatching { MediaPlayer.create(this, uri) }.getOrNull()
        val mp = mediaPlayer
        playButton.isEnabled = mp != null
        seek.isEnabled = mp != null
        if (mp != null) {
            mp.setOnCompletionListener {
                playButton.text = "▶"
                seek.progress = 1000
                tabView.setPlayhead(mp.duration / 1000.0)
            }
        }
    }

    private fun startTranscription() {
        val uri = selectedUri ?: return
        transcribeButton.isEnabled = false
        chooseButton.isEnabled = false
        progress.progress = 20
        status.text = "Preparing audio…"

        Thread {
            try {
                val input = copyUriToCache(uri)
                selectedLocalFile = input
                val modelDir = File(getExternalFilesDir(null), "model_weights").apply { mkdirs() }
                val model = File(modelDir, "ggml-model-htdemucs-4s-f16.bin")
                if (!model.exists() || model.length() < 1_000_000L) {
                    assets.open("ggml-model-htdemucs-4s-f16.bin").use { src -> FileOutputStream(model).use { src.copyTo(it) } }
                }
                val outDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "basscribe_stems").apply {
                    mkdirs(); listFiles()?.forEach { it.delete() }
                }
                runOnUiThread {
                    status.text = "Separating bass with Demucs… this is the slow part."
                    progress.progress = 50
                }
                val service = Intent(this, DemucsAndroidForegroundService::class.java).apply {
                    putExtra("audioFilePath", input.absolutePath)
                    putExtra("model", "free-4s")
                    putExtra("modelFilePaths", arrayOf(model.absolutePath))
                    putExtra("outDir", outDir.absolutePath)
                }
                ContextCompat.startForegroundService(this, service)
            } catch (t: Throwable) {
                runOnUiThread { fail("Could not start transcription: ${t.message ?: t.javaClass.simpleName}") }
            }
        }.start()
    }

    private fun transcribeBass(bassFile: File) {
        Thread {
            try {
                val pcm = WavBassDecoder.readMono22050(bassFile)
                val duration = pcm.size / 22050.0
                val neural = BasicPitchTranscriber(this).use { model ->
                    model.transcribe(pcm) { done, total ->
                        val pct = 760 + (220.0 * done / total).toInt()
                        runOnUiThread {
                            progress.progress = pct.coerceAtMost(980)
                            status.text = "Basic Pitch: window $done of $total"
                        }
                    }
                }
                val tab = BassTabEngine.makeTab(neural)
                runOnUiThread {
                    tabView.setTab(tab, duration)
                    progress.progress = 1000
                    status.text = if (tab.isEmpty()) "No confident bass notes found." else "${tab.size} bass notes • neural transcription complete"
                    transcribeButton.isEnabled = true
                    chooseButton.isEnabled = true
                    tabScroll.scrollTo(0, 0)
                }
            } catch (t: Throwable) {
                runOnUiThread { fail("Transcription failed: ${t.message ?: t.javaClass.simpleName}") }
            }
        }.start()
    }

    private fun fail(message: String) {
        status.text = message
        progress.progress = 0
        transcribeButton.isEnabled = selectedUri != null
        chooseButton.isEnabled = true
    }

    private fun togglePlayback() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause(); playButton.text = "▶"
        } else {
            mp.start(); playButton.text = "❚❚"; handler.post(playerTick)
        }
    }

    private val playerTick = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return
            if (mp.duration > 0) {
                val sec = mp.currentPosition / 1000.0
                seek.progress = ((mp.currentPosition.toDouble() / mp.duration) * 1000).toInt().coerceIn(0, 1000)
                tabView.setPlayhead(sec)
                val y = tabView.playheadY()
                val top = tabScroll.scrollY
                val bottom = top + tabScroll.height
                if (y < top + dp(30) || y > bottom - dp(50)) tabScroll.smoothScrollTo(0, (y - tabScroll.height / 3).coerceAtLeast(0))
            }
            if (mp.isPlaying) handler.postDelayed(this, 50)
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val name = displayName(uri).ifBlank { "input_audio" }
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(cacheDir, "basscribe_${System.currentTimeMillis()}_$safe")
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open audio" }
            out.outputStream().use { input.copyTo(it) }
        }
        return out
    }

    private fun displayName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "song"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0) ?: name
        }
        return name
    }

    private fun cleanProgress(s: String): String = s.substringAfter("] ", s).replace("Demucs.cpp", "Demucs")
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { mediaPlayer?.release() }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(completionReceiver)
        super.onDestroy()
    }
}
