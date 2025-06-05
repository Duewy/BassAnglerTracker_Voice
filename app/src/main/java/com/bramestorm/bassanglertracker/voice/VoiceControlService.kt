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

        // variable and value for ensuring no double loop of Vcc
private var lastWakeTimeMs = 0L
private const val MIN_WAKE_INTERVAL_MS = 2500L
    // for ensuring this app gets control of Bluetooth by being last audio player (Android thing)
private var isReclaimingAudio = false

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
    // for Incoming Telephone Calls
    private lateinit var telephonyManager: TelephonyManager
    private val callListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.w(TAG, "📞 Phone call detected — stopping voice session if active")
                    stopVoiceSessionIfActive()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.i(TAG, "📞 Call ended — restoring Bluetooth listener")
                    restartVoiceControlBluetoothListener()
                }
            }
        }
    }
    // after Phone call restart VCC again.
    private fun restartVoiceControlBluetoothListener() {
        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "🎙️ Media button ACTION_DOWN → starting onWake()")
                    onWake()
                } else {
                    Log.d(TAG, "🎙️ Media button ignored: action=${keyEvent?.action}")
                }
                return true
            }
        })

        mediaSession?.isActive = true
        Log.d(TAG, "✅ Media session reactivated")
    }

    private var sessionActive = false
    private var activeVoiceSession: VoiceInteractionManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var audioManager: AudioManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var mediaButtonReceiver: PendingIntent
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()
        // Audio & Power managers
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        try {       // Lets Phone Call take over Bluetooth and Listener
            telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            Log.e(TAG, "📵 PhoneStateListener blocked: ${e.message}")
        }

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
                Log.d(TAG, "🎙️ Media button event received!")
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
    }// ==== END == On Create ========================

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SharedPreferencesManager.isVccEnabled(this)) {
            Log.d(TAG, "Voice control is disabled — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START_VOICE,
            Intent.ACTION_MEDIA_BUTTON -> {
                Log.d(TAG, "🔥 Media button pressed — invoking onWake()")
                // ✅ START reclaim loop even if we got control briefly
                reclaimAudioFocusIfIdle()
                onWake()
            }
        }
        return START_STICKY
    }

    private fun onWake() {

        // ENSURES only one call happens (typically happens at first time VCC)
        val now = System.currentTimeMillis()
        if (now - lastWakeTimeMs < MIN_WAKE_INTERVAL_MS) {
            Log.w(TAG, "⛔ Suppressing duplicate wake — fired too soon.")
            return
        }
        lastWakeTimeMs = now

        Log.d(TAG, "🔥 onWake(): starting voice sequence")

        if (sessionActive) return  // Already running
        sessionActive = true

        // Allow 📞Phone CAll while using VCC Mode So the VCC Will not Start 🚫
        if (isInCall()) {
            Log.w(TAG, "📞 Phone call detected — suppressing voice control startup")
            Toast.makeText(this, "Call in progress — voice control paused.", Toast.LENGTH_SHORT).show()
            return
        }
        // Acquire brief wake lock
        wakeLock.acquire(5_000L)

        // Determine mode: Fun-Day or Tournament with Value from SetUp btnStartFishing saved in SharedPreferencesManager  getCatchEntryType()
        val type = SharedPreferencesManager.getCatchEntryType(applicationContext)
        // todo build a small UI helper: routes TTS through VoiceResponseManager
        // todo and toasts via Android Toast on the main thread
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
                // Value from SetUp btnStartFishing sent to SharedPreferences getCatchEntryType
        if (type in 5..8) {     // 1-4 Fund Day 5-8 Tournament CatchEntry files
                      //  pass both context and uiHelper
            val sharedPref = applicationContext.getSharedPreferences("catch_and_call_prefs", Context.MODE_PRIVATE)

            TournamentVoiceHandler.getInstance(
                context = applicationContext,
                uiHelper = uiHelper,
                alarmHour = sharedPref.getInt("ALARM_HOUR", -1),
                alarmMinute = sharedPref.getInt("ALARM_MINUTE", -1)
            ) .apply { setSessionRef {
                activeVoiceSession = it
                sessionActive = false // ✅ Allow next tap to start a session again
            } }

                .onWake()
        } else {
            // Fun Day mode → use FunDayVoiceHandler
            FunDayVoiceHandler.getInstance(
                context = applicationContext,
                uiHelper = uiHelper
            ).onWake()
        }

        // Release wake lock
        wakeLock.release()

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
        telephonyManager.listen(callListener, PhoneStateListener.LISTEN_NONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        abandonAudioFocusCompat(audioManager)
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiver)
        mediaSession?.release()
    }


    override fun onBind(intent: Intent?) = null

    private fun isInCall(): Boolean {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return if (
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
        } else {
            Log.w(TAG, "📵 READ_PHONE_STATE permission not granted — assuming no call")
            false
        }
    }


    private fun stopVoiceSessionIfActive() {
        try {
            activeVoiceSession?.shutdown()
            activeVoiceSession = null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel voice session cleanly: ${e.message}")
        }
        // Show a Message toast 📃
        Toast.makeText(this, "Call started — voice session canceled.", Toast.LENGTH_SHORT).show()
        // Speak the status via TTS 🔊
        val tts = VoiceResponseManager(applicationContext)
        tts.speak("You have an incoming call. Voice control has ended. Please re-enter your catch afterward. Over and Out.")
    }

        // to recapture the Last Audio Use so we have Bluetooth button input (Android thing)
        private fun reclaimAudioFocusIfIdle() {
            if (isReclaimingAudio) return
            isReclaimingAudio = true

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val focusResult = requestAudioFocusCompat(audioManager)

            if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                try {
                    val afd = assets.openFd("silence_0_1s.m4a")
                    val player = MediaPlayer()
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    player.prepare()
                    player.setOnCompletionListener {
                        it.release()
                        isReclaimingAudio = false
                    }
                    player.start()
                } catch (e: Exception) {
                    isReclaimingAudio = false
                    Log.e("VCC_AUDIO", "❌ Failed to play silent audio: ${e.message}")
                }
            } else {
                isReclaimingAudio = false
                Log.d("VCC_AUDIO", "⚠️ Audio still in use by another app")
            }
        }

    // For Android to work with various OS Versions
    private fun requestAudioFocusCompat(audioManager: AudioManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener({}, handler)
                    .build()
            }
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

            // to clear out of the controlling Bluetooth when app is closed
    private fun abandonAudioFocusCompat(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                Log.d("VCC_AUDIO", "🛑 Abandoned audio focus (API 26+)")
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
            Log.d("VCC_AUDIO", "🛑 Abandoned audio focus (legacy API)")
        }
    }



}// ====== END = Voice Control Service ================
