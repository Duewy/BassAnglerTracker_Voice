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
import java.util.concurrent.atomic.AtomicBoolean

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
    private var activeResponseManager: VoiceResponseManager? = null
    private var activeSessionToken = 0L
    private var nextSessionToken = 0L
    private var sessionStartupInProgress = false
    private var pendingCleanupReason: String? = null
    private val cleanupLock = Any()
    @Volatile
    private var isCleaningUpSession = false
    private var lastWakeAt = 0L

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

        // TODO look into the UPSIDE DOWN CAKE stuff THIS Winter.
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
    ): Boolean {
        val previousEngine: VoiceInteractionManager?
        val newEngine = VoiceInteractionManager(
            context = applicationContext,
            uiHelper = uiHelper,
            parser = VoiceParser
        )

        synchronized(cleanupLock) {
            if (!sessionActive || isCleaningUpSession || activeVoiceSession == null) {
                Log.d(TAG, "⛔ startVoiceSession() rejected — no active Tournament VC session")
                return false
            }
            previousEngine = voiceEngine
            voiceEngine = newEngine
        }

        previousEngine?.shutdown()

        newEngine.startSession(
            prompt,
            onResult = { result -> onResult(result) },
            onFailure = {
                Log.w(TAG, "Voice session failed or cancelled — cleaning up active session")
                cleanupActiveSession("voice engine failure")
            }
        )
        return true
    }

    /** 4️⃣ Exactly your old handleVoiceStart(), nothing auto-firing */
    private fun onWake() {
        val now = System.currentTimeMillis()
        if (now - lastWakeAt < 1200L) {
            Log.d(TAG, "⏱️ onWake throttled")
            return
        }
        lastWakeAt = now

        if (!SharedPreferencesManager.isVccEnabled(this)) {
            Log.d(TAG, "⛔ onWake() blocked — VCC disabled")
            return
        }

        if (isInCall()) {
            Log.d(TAG, "⛔ onWake() blocked — in call")
            return
        }

        val responseManager = VoiceResponseManager(applicationContext)
        val uiHelper = object : VoiceUiHelper {
            private val mainH = Handler(Looper.getMainLooper())

            override fun speak(text: String) {
                responseManager.speak(text)
            }

            override fun speak(text: String, utteranceId: String) {
                responseManager.speak(text, utteranceId)
            }

            override fun showToast(message: String) {
                mainH.post {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val sessionToken = synchronized(cleanupLock) {
            nextSessionToken += 1L
            nextSessionToken
        }
        val voiceSession: VoiceSessionHandler = if (SharedPreferencesManager.isTournamentCatchEntryType(this)) {
            TournamentVoiceHandler(
                context = this,
                uiHelper = uiHelper,
                sessionToken = sessionToken,
            )
        } else {
            FunDayVoiceHandler(this, uiHelper)
        }

        var startupFailure: Throwable? = null
        var deferredCleanupReason: String? = null
        synchronized(cleanupLock) {
            if (sessionActive || isCleaningUpSession ||
                activeVoiceSession != null || voiceEngine != null || activeResponseManager != null
            ) {
                responseManager.shutdown()
                Log.d(TAG, "⛔ onWake() blocked — previous voice state has not fully released yet")
                return
            }

            sessionActive = true
            activeSessionToken = sessionToken
            activeResponseManager = responseManager
            activeVoiceSession = voiceSession
            sessionStartupInProgress = true
        }

        Log.d(TAG, "🔁 onWake() called — sessionActive")
        wakeLock.acquire(60_000L)       // give the full 60 seconds to account for extended interactions or questions ....
        try {
            voiceSession.onWake()
        } catch (t: Throwable) {
            startupFailure = t
        } finally {
            synchronized(cleanupLock) {
                sessionStartupInProgress = false
                deferredCleanupReason = pendingCleanupReason
                pendingCleanupReason = null
            }
        }

        startupFailure?.let { failure ->
            Log.w(TAG, "❌ Voice session startup failed", failure)
            cleanupActiveSession("voice session startup failure")
            return
        }

        deferredCleanupReason?.let { pendingReason ->
            cleanupActiveSession(pendingReason)
        }
    }
        //==== END = on Wake =====================


    fun markSessionComplete() {
        cleanupActiveSession("session marked complete")
    }

    fun speakAndEndSession(
        message: String,
        reason: String
    ) {
        val timeoutHandler = Handler(Looper.getMainLooper())
        val cleanupTriggered = AtomicBoolean(false)
        val cleanupFallback = Runnable {
            if (!cleanupTriggered.compareAndSet(false, true)) {
                return@Runnable
            }
            Log.w(TAG, "Voice completion callback did not arrive; forcing cleanup for: $reason")
            cleanupActiveSession(reason)
        }
        val responseManager = synchronized(cleanupLock) {
            if (!sessionActive || isCleaningUpSession) {
                null
            } else {
                activeResponseManager
            }
        }

        if (responseManager == null) {
            cleanupActiveSession(reason)
            return
        }

        timeoutHandler.postDelayed(cleanupFallback, 8_000L)
        responseManager.speak(message) {
            if (!cleanupTriggered.compareAndSet(false, true)) {
                return@speak reason
            }
            timeoutHandler.removeCallbacks(cleanupFallback)
            cleanupActiveSession(reason)
            reason
        }
    }

    private fun isInCall(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            .let { perm ->
                perm == PackageManager.PERMISSION_GRANTED &&
                        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                            .callState != TelephonyManager.CALL_STATE_IDLE
            }

    private fun stopVoiceSessionIfActive() {
        cleanupActiveSession("call started")
        Toast.makeText(this, "Call started — voice session canceled.", Toast.LENGTH_SHORT).show()
    }

    private fun cleanupActiveSession(
        reason: String
    ) {
        var cleanupReason = reason

        while (true) {
            val voiceSession: VoiceSessionHandler?
            val engine: VoiceInteractionManager?
            val responseManager: VoiceResponseManager?

            synchronized(cleanupLock) {
                if (sessionStartupInProgress) {
                    if (pendingCleanupReason == null) {
                        pendingCleanupReason = cleanupReason
                    }
                    Log.d(TAG, "🧹 Delaying active voice session cleanup until startup completes: $cleanupReason")
                    return
                }
                if (isCleaningUpSession) {
                    Log.d(TAG, "🧹 Active voice session cleanup already in progress: $cleanupReason")
                    return
                }

                isCleaningUpSession = true
                voiceSession = activeVoiceSession
                engine = voiceEngine
                responseManager = activeResponseManager

                activeVoiceSession = null
                voiceEngine = null
                activeResponseManager = null
                activeSessionToken = 0L
                sessionActive = false
            }

            try {
                runCleanupStep("voice engine shutdown") {
                    engine?.shutdown()
                }
                runCleanupStep("voice session shutdown") {
                    voiceSession?.shutdown()
                }
                runCleanupStep("voice response manager shutdown") {
                    responseManager?.shutdown()
                }
                runCleanupStep("wake lock release") {
                    if (::wakeLock.isInitialized && wakeLock.isHeld) {
                        wakeLock.release()
                    }
                }
                Log.d(TAG, "🧹 Active voice session cleaned up: $cleanupReason")
            } finally {
                val hasMoreState = synchronized(cleanupLock) {
                    val hasMore =
                        activeVoiceSession != null ||
                                voiceEngine != null ||
                                activeResponseManager != null
                    isCleaningUpSession = false
                    hasMore
                }
                if (!hasMoreState) {
                    return
                }
            }

            cleanupReason = "$reason (continuing cleanup)"
        }
    }

    private fun runCleanupStep(
        label: String,
        action: () -> Unit
    ) {
        try {
            action()
        } catch (t: Throwable) {
            Log.w(TAG, "⚠️ Cleanup step failed: $label", t)
        }
    }

    fun isCurrentSession(
        sessionToken: Long,
        handler: VoiceSessionHandler? = null
    ): Boolean = synchronized(cleanupLock) {
        isCurrentSessionLocked(sessionToken, handler)
    }

    private fun isCurrentSessionLocked(
        sessionToken: Long,
        handler: VoiceSessionHandler? = null
    ): Boolean {
        return !isCleaningUpSession &&
                sessionActive &&
                activeSessionToken == sessionToken &&
                (handler == null || activeVoiceSession === handler)
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
        cleanupActiveSession("service destroyed")

        super.onDestroy()
    }


    override fun onBind(intent: Intent?) = null

}//=========== END == Voice Control Service =============
