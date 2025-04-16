package com.bramestorm.bassanglertracker.training

import ai.picovoice.porcupine.PorcupineManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bramestorm.bassanglertracker.R


class VoiceService : Service() {

    private lateinit var porcupineManager: PorcupineManager

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification("Catch Caddy is listening..."))

        val accessKey = "sk-TawEvd8BQnSwLVve851+KaCo7U1H7uOVEHsbWKZrz28flmwGzHTX3w=="


        porcupineManager = PorcupineManager.Builder()
            .setAccessKey(accessKey)
            .setKeywordPath("Catch-Caddy_en_android_v3_0_0.ppn")
            .setModelPath("porcupine_params.pv")
            .setSensitivity(0.5f)
            .build(applicationContext) { wakeWordDetected() }

        porcupineManager.start()
    }

    private fun wakeWordDetected() {
        Log.d("VoiceService", "Wake word heard 🎤")
        showOverlay()
    }

    private fun showOverlay() {
        val intent = Intent(this, VoiceOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }


    private fun createNotification(content: String): Notification {
        val channelId = "voice_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Voice Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("BassAnglerTracker")
            .setContentText(content)
            .setSmallIcon(R.drawable.icon_mic)
            .build()
    }

//
    override fun onDestroy() {
        porcupineManager.stop()
        porcupineManager.delete()
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
