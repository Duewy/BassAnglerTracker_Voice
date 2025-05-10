package com.bramestorm.bassanglertracker.base

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.utils.FishSpecies
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.voice.VoiceAudioUtils
import com.bramestorm.bassanglertracker.voice.VoiceControlService

/**
 * A base activity for all CatchEntry screens that handles:
 *  - Voice toggle & service start/stop
 *  - BroadcastReceiver for double-tap wake events
 *  - Runtime RECORD_AUDIO permission
 *  - SpeechRecognizer initialization & lifecycle
 *  - Delivery of recognized speech via onSpeechResult()
 */
abstract class BaseCatchEntryActivity : AppCompatActivity() {


    private var lastVolDownTap = 0L
    private var lastVolUpTap   = 0L
    private var isWakeReceiverRegistered = false

    protected open val catchReceiver: BroadcastReceiver
            = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) { /* no-op */ }
    }

    companion object {
        private const val AUDIO_REQUEST_CODE = 42
        private const val VOICE_WAKE_ACTION = "com.bramestorm.bassanglertracker.VOICE_WAKE"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val now = SystemClock.elapsedRealtime()
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (now - lastVolUpTap < 400) {
                    onVoiceWake()  // Combo detected
                    lastVolUpTap = 0L
                    lastVolDownTap = 0L
                    return true
                }
                lastVolDownTap = now
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (now - lastVolDownTap < 400) {
                    onVoiceWake()  // Combo detected
                    lastVolUpTap = 0L
                    lastVolDownTap = 0L
                    return true
                }
                lastVolUpTap = now
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    abstract val dialog: Any
    open lateinit var recognizer: SpeechRecognizer
    open lateinit var recognizerIntent: Intent
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == VOICE_WAKE_ACTION) {
                onVoiceWake()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // ✅ Register species for voice recognition
        FishSpecies.allSpeciesList.forEach {
            VoiceInputMapper.registerUserSpecies(it)
        }

        val userSpecies = SharedPreferencesManager.getUserAddedSpeciesList(this)
        userSpecies.forEach {
            VoiceInputMapper.registerUserSpecies(it)
        }

        // Start/stop service and register receiver based on user preference
        val prefs = getSharedPreferences("catch_and_call_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("voice_enabled", false)) {
            // 👇 Play silent audio to claim Bluetooth media button control
            VoiceAudioUtils.playSilentAudio(this)
            ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceControlService::class.java)
            )
            ContextCompat.registerReceiver(
                this,
                wakeReceiver,
                IntentFilter(VOICE_WAKE_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            isWakeReceiverRegistered = true
        }

        // Request microphone permission and initialize SpeechRecognizer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_REQUEST_CODE
            )
        } else {
            initSpeechRecognizer()
        }
    }//=============== END onCreate ====================================

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val voiceControlEnabled = prefs.getBoolean("VOICE_CONTROL_ENABLED", false)
        if (voiceControlEnabled) {
            VoiceAudioUtils.playSilentAudio(this)
        }
    }

    override fun onDestroy() {
        recognizer.destroy()
        try {
            if (isWakeReceiverRegistered) {
                unregisterReceiver(wakeReceiver)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("VCC", "Receiver not registered — skipping unregister.")
        }
        super.onDestroy()
    }

    private fun initSpeechRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_REQUEST_CODE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            initSpeechRecognizer()
        } else {
            Toast.makeText(
                this,
                "Microphone permission is required for voice input",
                Toast.LENGTH_LONG
            ).show()
        }
    }



    /**
     * Called when the BroadcastReceiver sees a VOICE_WAKE action.
     * Default behavior: start listening if initialized.
     */
    protected open fun onVoiceWake() {
        if (::recognizer.isInitialized) {
            recognizer.startListening(recognizerIntent)
        }
    }

    /**
     * Called on a Volume-Up double-tap.
     * By default: if your `dialog` is a Dialog (or PopupWindow) instance, show it.
     * Subclasses can override this to call whatever “show manual catch” method they need.
     */
    protected open fun onManualWake() {
        when (dialog) {
            is Dialog       -> (dialog as Dialog).show()
            else            -> {
                // or call a method on your subclass, e.g. showWeightPopup()
            }
        }
    }



    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Toast.makeText(
                this@BaseCatchEntryActivity,
                "Recognition error $error",
                Toast.LENGTH_SHORT
            ).show()
        }
        override fun onResults(results: Bundle) {
            val transcript = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            onSpeechResult(transcript)
        }
        override fun onPartialResults(partial: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Subclasses implement this to receive recognized speech text.
     */
    protected abstract fun onSpeechResult(transcript: String)
    }


class ABroadcastReceiver {

}
