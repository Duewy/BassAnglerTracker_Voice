package com.bramestorm.bassanglertracker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bramestorm.bassanglertracker.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("ALARM_DEBUG", "🚨 AlarmReceiver triggered!")

        try {
            // Optional: Set volume to max (media stream only)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = true
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
            )

            // Play alarm sound
            val mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
            mediaPlayer?.setOnPreparedListener {
                Log.d("ALARM_DEBUG", "🔊 MediaPlayer prepared. Playing sound.")
                it.start()
            }
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("ALARM_DEBUG", "❌ MediaPlayer error: what=$what, extra=$extra")
                true
            }
        } catch (e: Exception) {
            Log.e("ALARM_DEBUG", "❌ Exception while playing alarm sound", e)
        }

        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val channelId = "alarm_channel"
        val channelName = "Tournament Alarm"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎣 Tournament Alarm")
            .setContentText("Time to check in or log your next catch!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}
