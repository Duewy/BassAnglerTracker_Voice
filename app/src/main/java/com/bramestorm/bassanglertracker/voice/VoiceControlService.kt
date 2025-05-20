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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.bramestorm.bassanglertracker.PopupVccTournLbs
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.positionedToast
import java.util.ArrayDeque


class VoiceControlService : Service() {
    companion object {
        private const val CHANNEL_ID = "vc_channel"
        private const val NOTIF_ID = 1
        private const val TAG = "VoiceCtrlSvc"
        const val EXTRA_START_FROM_HEADSET = "START_FROM_HEADSET"
    }

    private var mediaSession: MediaSessionCompat?=null
    private lateinit var audioManager: AudioManager
    // focusRequest now nullable since used only on O+
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tapTimestamps = ArrayDeque<Long>()

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

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Request audio focus
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

        // Post persistent notification and start foreground
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        // Initialize MediaSession
    val session = MediaSessionCompat(this, "VoiceCtrl")
    mediaSession = session

    session.apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: Intent): Boolean {
                val key = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    ?: return false

                Log.d(TAG, "MediaSession created and set to active: ${session.isActive}")

                if (key.action == KeyEvent.ACTION_DOWN) {
                    when (key.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_NEXT,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            Log.d(TAG, "Media button detected: ${key.keyCode}, triggering onWake()")
                            onWake()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(intent)
            }
        })

        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        setPlaybackState(
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

        isActive = true

        val player = android.media.MediaPlayer.create(this@VoiceControlService, R.raw.silence_0_1s)
        player?.setOnCompletionListener { mp ->
            mp.release()
        }
        player?.start()



        val focusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("deprecation")
            audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        Log.d(TAG, "Audio focus request result: $focusResult") // Should be 1
    }

    // Set media button receiver
        val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(packageName)
        val pi = PendingIntent.getBroadcast(
            this, 0, mediaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession?.setMediaButtonReceiver(null)
    }//======================== END onCreate ==========================

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
        Log.d("VCC", "🔥 onWake(): Directly launching voice popup")

        val popupIntent = Intent(this, PopupVccTournLbs::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(popupIntent)

        mediaSession?.let { session ->
            session.isActive = false
            sendBroadcast(Intent("com.bramestorm.bassanglertracker.VOICE_WAKE"))
            handler.postDelayed({ session.isActive = true }, 2000)
        }
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
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
