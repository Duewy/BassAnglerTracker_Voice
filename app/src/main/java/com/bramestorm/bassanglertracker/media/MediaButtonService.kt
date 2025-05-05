package com.bramestorm.bassanglertracker.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.voice.VoiceControlService

class MediaButtonService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "CatchAndCallSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean {
                    Log.d("VCC", "📥 Media button pressed")

                    val keyEvent = mediaButtonIntent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {

                        // ✅ Check if voice mode is ON
                        if (!SharedPreferencesManager.isVoiceControlEnabled(this@MediaButtonService)) {
                            Log.d("VCC", "❌ VCC off — ignoring media button")
                            return true
                        }

                        // ✅ Wake screen
                        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                        val wakeLock = powerManager.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "CatchAndCall::WakeLock"
                        )
                        wakeLock.acquire(3000)

                        // ✅ Start VoiceControlService
                        val vccIntent = Intent(this@MediaButtonService, VoiceControlService::class.java).apply {
                            action = "START_VOICE_SEQUENCE"
                        }
                        ContextCompat.startForegroundService(this@MediaButtonService, vccIntent)
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })

            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("CatchAndCallRoot", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(emptyList())
    }
}
