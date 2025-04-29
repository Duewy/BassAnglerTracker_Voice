package com.bramestorm.bassanglertracker.base

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    companion object {
        private const val AUDIO_REQUEST_CODE = 42
        private const val VOICE_WAKE_ACTION = "com.bramestorm.bassanglertracker.VOICE_WAKE"
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

        // Start/stop service and register receiver based on user preference
        val prefs = getSharedPreferences("catch_and_call_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("voice_enabled", false)) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceControlService::class.java)
            )
            registerReceiver(wakeReceiver, IntentFilter(VOICE_WAKE_ACTION))
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

    override fun onDestroy() {
        recognizer.destroy()
        unregisterReceiver(wakeReceiver)
        super.onDestroy()
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
