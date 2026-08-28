package com.github.sevagh.demucs_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class DemucsAndroidForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioFilePath = intent?.getStringExtra("audioFilePath") ?: ""
        val selectedModel = intent?.getStringExtra("model") ?: "free-4s"
        val modelFilePaths = intent?.getStringArrayExtra("modelFilePaths") ?: emptyArray()
        val outDir = intent?.getStringExtra("outDir") ?: ""
        startForeground(42, createNotification())

        Thread {
            try {
                val stems = demucsInference(audioFilePath, selectedModel, modelFilePaths, outDir) ?: emptyArray()
                LocalBroadcastManager.getInstance(this).sendBroadcast(
                    Intent("ACTION_DEMIX_JOB_COMPLETED").putExtra("writtenStems", stems)
                )
            } catch (t: Throwable) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(
                    Intent("ACTION_DEMIX_JOB_FAILED").putExtra("error", t.message ?: t.javaClass.simpleName)
                )
            } finally {
                stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }

    fun inferenceProgressUpdate(progress: Float, message: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("ACTION_DEMIX_PROGRESS_UPDATE")
                .putExtra("EXTRA_PROGRESS", progress)
                .putExtra("EXTRA_MESSAGE", message)
        )
    }

    override fun onDestroy() {
        runCatching { stopInference() }
        super.onDestroy()
    }

    private fun createNotification(): android.app.Notification {
        val id = "BasscribeNeural"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(id, "Basscribe transcription", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, id)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Basscribe is separating bass")
            .setContentText("Demucs neural processing is running")
            .setOngoing(true)
            .build()
    }

    companion object {
        init { System.loadLibrary("demucs_ndk") }
    }

    private external fun stopInference()
    private external fun demucsInference(
        audioFilePath: String,
        modelName: String,
        modelFilePaths: Array<String>,
        outDir: String
    ): Array<String>?
}
