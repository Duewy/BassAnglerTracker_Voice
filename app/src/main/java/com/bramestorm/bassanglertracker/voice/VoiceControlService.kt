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
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.positionedToast


class VoiceControlService : Service() {
    companion object {
        private const val CHANNEL_ID = "vc_channel"
        private const val NOTIFY_ID  = 1
        private const val TAG        = "VoiceCtrlSvc"
        private const val ACTION_VOICE_WAKE ="com.bramestorm.bassanglertracker.VOICE_WAKE"
    }

    private var mediaSession: MediaSessionCompat?=null
    private lateinit var audioManager: AudioManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var mediaButtonReceiverIntent: PendingIntent


    // focusRequest now nullable since used only on O+
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                mediaSession?.isActive = false

            AudioManager.AUDIOFOCUS_GAIN ->
                mediaSession?.isActive = true
        }
    }
    
    //============================ onCreate ===============================================
    override fun onCreate() {
        super.onCreate()

        // 1) Grab AudioManager & PowerManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CatchAndCall:VoiceWakeLock"
        )

        // 2) Request audio focus so other apps duck or pause
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(afChangeListener, handler)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("deprecation")
            audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }


        // 3) Create notification channel & go foreground
        createChannel()
        startForeground(NOTIFY_ID, buildNotification())

        // 4) Build your MediaSession
        val session = MediaSessionCompat(this, "VoiceCtrl")
        mediaSession = session
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: Intent): Boolean {
                val key = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
                if (key.action == KeyEvent.ACTION_DOWN) {
                    when (key.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_NEXT,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            onWake()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(intent)
            }
        })
        session.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .build()
        )
        session.isActive = true

        // 5) Register your media-button PendingIntent
        mediaButtonReceiverIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        session.setMediaButtonReceiver(mediaButtonReceiverIntent)
        audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverIntent)

        // 6) Play a very brief silent clip to lock in media focus immediately
        val player = MediaPlayer.create(this, R.raw.silence_0_1s)
        player?.setOnCompletionListener { it.release() }
        player?.start()
    }


    //======================== END onCreate ==========================

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_VOICE_SEQUENCE" -> {
                startVoiceSequence()  // your function to begin voice capture
            }
        }
        return START_STICKY
    }

    private fun startVoiceSequence() {
        Log.d("VCC", "🎙️ Voice Sequence Started (stub)")

        // TODO: Trigger your voice interaction here
        // This could open a CatchEntry popup, start SpeechRecognizer, etc.
        positionedToast( "🎤 Starting Voice Capture")
    }


    private fun onWake() {
        Log.d(TAG, "🔥 onWake(): broadcasting VOICE_WAKE")

        // 1) wake the CPU
        wakeLock.acquire(5_000L)

        // 2) tell whichever Activity is in front that “voice woke”
        val wakeIntent = Intent(VoiceControlService.ACTION_VOICE_WAKE)
        sendBroadcast(wakeIntent,"${applicationContext.packageName}.permission.VOICE_WAKE")

        // 3) clean up
        wakeLock.release()

        // pause & re-activate your MediaSession so you don’t double-fire
        mediaSession?.isActive = false
        handler.postDelayed({ mediaSession?.isActive = true }, 2000)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Control Active")
            .setContentText("Double-tap to wake voice input")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Control",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        // Abandon audio focus appropriately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("deprecation")
            audioManager.abandonAudioFocus(afChangeListener)
        }
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiverIntent)
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
