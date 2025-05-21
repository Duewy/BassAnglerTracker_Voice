package com.bramestorm.bassanglertracker.base

import android.Manifest
import android.annotation.SuppressLint
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
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.utils.FishSpecies
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
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

    companion object {
        private const val AUDIO_REQUEST_CODE = 42
        const val VOICE_WAKE_ACTION = "com.bramestorm.bassanglertracker.VOICE_WAKE"
    }

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            // whenever VOICE_WAKE arrives, trigger the UI flow:
            onVoiceWake()
        }
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

    @SuppressLint("SuspiciousIndentation")
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
        if (getSharedPreferences("catch_and_call_prefs", MODE_PRIVATE)
                .getBoolean("voice_enabled", false)
        ) {
            // 1) start the foreground service
            ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceControlService::class.java)
            )
            // 2) dynamically listen for your VOICE_WAKE broadcast
            ContextCompat.registerReceiver(
                this,
                wakeReceiver,
                IntentFilter(VOICE_WAKE_ACTION),
                "$packageName.permission.VOICE_WAKE",  // enforce your signature permission
                null,
                ContextCompat.RECEIVER_NOT_EXPORTED    // only your own app can send
            )
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
        val prefs = getSharedPreferences("catch_and_call_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("voice_enabled", false)) {
            ContextCompat.startForegroundService(
                this,Intent(this, VoiceControlService::class.java))
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(wakeReceiver) } catch(_: Exception) {}
        recognizer.destroy()
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
    protected open fun onVoiceWake() { /* default no-op */ }

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
