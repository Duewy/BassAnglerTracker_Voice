@file:Suppress("DEPRECATION")

package com.bramestorm.bassanglertracker.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.training.VoiceResponseManager
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager


/**
 * Foreground service that listens for Bluetooth/media button presses to start voice control.
 */
class VoiceControlService : Service() {
    companion object {
        private const val CHANNEL_ID = "vc_channel"
        private const val NOTIFY_ID = 1
        private const val TAG = "VoiceCtrlSvc"
        private const val ACTION_MEDIA_BUTTON = Intent.ACTION_MEDIA_BUTTON
        private const val ACTION_START_VOICE = "com.bramestorm.START_VOICE_SEQUENCE"
    }

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var audioManager: AudioManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var mediaButtonReceiver: PendingIntent
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()
        // Audio & Power managers
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CatchAndCall:VoiceWakeLock")

        // Request audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener({ change ->  // handle focus changes if needed
                }, handler)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        // Create notification channel & start foreground
        createChannel()
        startForeground(NOTIFY_ID, buildNotification())

        // Setup MediaSession for media-button events
        val session = MediaSessionCompat(this, TAG)

        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE
                )
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .build()
        )
        session.isActive = true

        // ← Re-attach the callback that turns key events into onWake()
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                onWake()
                return true
            }
        })


        mediaSession = session
        // PendingIntent for media button
        mediaButtonReceiver = PendingIntent.getBroadcast(
            this, 0,
            Intent(ACTION_MEDIA_BUTTON).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        session.setMediaButtonReceiver(mediaButtonReceiver)
        audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver)

        // Play silent clip to establish focus
        MediaPlayer.create(this, R.raw.silence_0_1s)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VOICE -> onWake()
        }
        return START_STICKY
    }

    private fun onWake() {
        Log.d(TAG, "🔥 onWake(): starting voice sequence")

        // Acquire brief wake lock
        wakeLock.acquire(5_000L)

        // Determine mode: Fun-Day or Tournament
        val type = SharedPreferencesManager.getCatchEntryType(applicationContext)
        if (type in 5..8) {
                       // build a small UI helper: routes TTS through VoiceResponseManager
                       // and toasts via Android Toast on the main thread
                       val uiHelper = object : VoiceUiHelper {
                           private val vrm = VoiceResponseManager(applicationContext)
                           private val mainHandler = Handler(Looper.getMainLooper())

                           override fun speak(text: String) {
                               vrm.speak(text)
                           }

                           override fun showToast(message: String) {
                               mainHandler.post {
                                   Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
                                       .show()
                                         }
                                 }
                        }

                      // now pass both context and uiHelper
            val sharedPref = applicationContext.getSharedPreferences("catch_and_call_prefs", Context.MODE_PRIVATE)

            TournamentVoiceHandler.getInstance(
                context = applicationContext,
                uiHelper = uiHelper,
                alarmHour = sharedPref.getInt("ALARM_HOUR", -1),
                alarmMinute = sharedPref.getInt("ALARM_MINUTE", -1)
            ).onWake()

        } else {
            // For fun-day: start manual or voice entry if still supported
            sendBroadcast(Intent("com.bramestorm.SHOW_VCC_POPUP"))//todo we do not have Vcc Popup any more....
        }

        // Release wake lock
        wakeLock.release()

        // Debounce media button presses
        mediaSession?.isActive = false
        handler.postDelayed({ mediaSession?.isActive = true }, 2000)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Control",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Control Active")
            .setContentText("Double-tap to start voice entry")  // todo it actually take just one tap of the play/pause button
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiver)
        mediaSession?.release()
    }


    override fun onBind(intent: Intent?) = null
}
