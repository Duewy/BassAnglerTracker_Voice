@file:Suppress("DEPRECATION")
package com.bramestorm.bassanglertracker.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.training.VoiceResponseManager
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class VoiceControlService : Service() {
    companion object {
        const val CHANNEL_ID       = "vc_channel"
        const val NOTIFY_ID        = 1
        private const val TAG      = "VoiceCtrlSvc"
        private const val ACTION_MEDIA_BUTTON = Intent.ACTION_MEDIA_BUTTON
        const val ACTION_START_VOICE = "com.bramestorm.START_VOICE_SEQUENCE"
    }

    private lateinit var telephonyManager: TelephonyManager
    private val audioManager by lazy {getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private lateinit var wakeLock: PowerManager.WakeLock
    private var focusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var mediaButtonReceiver: PendingIntent

    private var sessionActive = false
    private var activeVoiceSession: VoiceSessionHandler? = null
    private var voiceEngine: VoiceInteractionManager? = null


    /** 1️⃣ Only one callback, wired to call onWake() on ACTION_DOWN */
    private val mediaButtonCallback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(intent: Intent): Boolean {
            val ev = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (ev?.action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "🎙 Media button ACTION_DOWN → onWake()")
                onWake()
            }
            return true
        }
    }

    /** 2️⃣ Stop VCC on call start, re-enable on call end (and play a silent clip) */
    private val callListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.w(TAG, "📞 call in progress → cancelling voice session")
                    stopVoiceSessionIfActive()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.i(TAG, "📞 call ended → restoring media listener & silent clip")
                    mediaSession?.apply {
                        setCallback(mediaButtonCallback)
                        isActive = true
                    }
                    // silent clip to grab audio focus back
                    MediaPlayer.create(this@VoiceControlService, R.raw.silence_0_1s)?.apply {
                        setOnCompletionListener { mp -> mp.release() }
                        start()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
            super.onCreate()

            // 1️⃣ Promote to foreground immediately
            createChannel()
            startForeground(NOTIFY_ID, buildNotification())

            // 2️⃣ THEN do the rest of your initialization
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE)
            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:WakeLock")

        // 3️⃣ Single mediaSession, hooked to our callback
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_PAUSE
                    )
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                    .build()
            )
            setCallback(mediaButtonCallback)
            isActive = true

            mediaButtonReceiver = PendingIntent.getBroadcast(
                this@VoiceControlService, 0,
                Intent(ACTION_MEDIA_BUTTON).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setMediaButtonReceiver(mediaButtonReceiver)
            audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VOICE,
            ACTION_MEDIA_BUTTON -> {
                Log.d(TAG, "🔥 Media-button Intent → onWake()")
                onWake()
            }
        }
        return START_STICKY
    }

    /**
     * Kicks off a new STT/TTS session using our VoiceInteractionManager.
     */
    fun startVoiceSession(
        prompt: String,
        uiHelper: VoiceUiHelper,
        onResult: (String) -> Unit
    ) {
        // cancel in‐flight engine session (TTS/STT engine)
        voiceEngine?.shutdown()

        voiceEngine = VoiceInteractionManager(
            context = applicationContext,
            uiHelper = uiHelper,
            parser = VoiceParser
        ).also {
            it.startSession(
                prompt,
                onResult = { result -> onResult(result) },
                onFailure = {
                    sessionActive = false
                    Log.w(TAG, "Voice session failed or cancelled — resetting sessionActive")
                }
            )
        }
    }

    /** 4️⃣ Exactly your old handleVoiceStart(), nothing auto-firing */
    private fun onWake() {
        if (!SharedPreferencesManager.isVccEnabled(this) || sessionActive || isInCall()) {
            Log.d(TAG, "⛔ onWake() blocked — sessionActive=$sessionActive")
            return
        }

        sessionActive = true
        Log.d(TAG, "🔁 onWake() called — sessionActive=$sessionActive")
        wakeLock.acquire(60_000L)       // give the full 60 seconds to account for extended interactions or questions ....

        val uiHelper = object : VoiceUiHelper {
            private val vrm = VoiceResponseManager(applicationContext)
            private val mainH = Handler(Looper.getMainLooper())

            override fun speak(text: String) {
                vrm.speak(text)
            }

            override fun speak(text: String, utteranceId: String) {
                vrm.speak(text) { utteranceId }
            }

            override fun showToast(message: String) {
                mainH.post {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        when (SharedPreferencesManager.getCatchEntryType(this)) {
            in 5..8 -> TournamentVoiceHandler(
                context     = this,
                uiHelper    = uiHelper,
            ).onWake()

            else -> FunDayVoiceHandler(this, uiHelper).onWake()
        }

    }
        //==== END = on Wake =====================


    fun markSessionComplete() {
        sessionActive = false
        if (wakeLock.isHeld) wakeLock.release()
        Log.d(TAG, "✅ Voice session marked complete — wakeLock released")
    }

    private fun isInCall(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            .let { perm ->
                perm == PackageManager.PERMISSION_GRANTED &&
                        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                            .callState != TelephonyManager.CALL_STATE_IDLE
            }

    private fun stopVoiceSessionIfActive() {
        voiceEngine?.shutdown()
        voiceEngine = null
        activeVoiceSession?.shutdown()
        activeVoiceSession = null
        sessionActive = false
        if (wakeLock.isHeld) wakeLock.release()
        Toast.makeText(this, "Call started — voice session canceled.", Toast.LENGTH_SHORT).show()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "Voice Control", NotificationManager.IMPORTANCE_LOW)
                .also { ch -> getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch) }
        }
    }
    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Control Active")
            .setContentText("Press play/pause to start voice entry")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        telephonyManager.listen(callListener, PhoneStateListener.LISTEN_NONE)
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            ?: @Suppress("DEPRECATION") audioManager.abandonAudioFocus(null)
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiver)
        mediaSession?.release()

        // 🔐 Important cleanup
        voiceEngine?.shutdown()
        if (wakeLock.isHeld) wakeLock.release()

        super.onDestroy()
    }


    override fun onBind(intent: Intent?) = null

}//=========== END == Voice Control Service =============
