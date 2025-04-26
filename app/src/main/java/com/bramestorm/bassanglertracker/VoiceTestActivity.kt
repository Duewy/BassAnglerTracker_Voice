package com.bramestorm.bassanglertracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceTestActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private val requestAudioCode = 101

    private lateinit var btnVoiceMic: Button
    private lateinit var txtVoiceResponse: TextView
    private lateinit var txtVoiceInput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) Inflate your XML once:
        setContentView(R.layout.activity_voice_test)

        // 2) Wire up views
        btnVoiceMic      = findViewById(R.id.btnVoiceMic)
        txtVoiceResponse = findViewById(R.id.txtVoiceResponse)
        txtVoiceInput    = findViewById(R.id.txtVoiceInput)

        // 3) Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestAudioCode
            )
        }

        // 4) Create the recognizer and its intent
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        // Immediately tear down any leftover session:
        speechRecognizer.stopListening()
        speechRecognizer.cancel()

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        // 5) Hook up listener
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE_TEST", "✅ Ready for speech")
                txtVoiceResponse.text = "🎧 Listening…"
            }

            override fun onBeginningOfSpeech() {
                Log.d("VOICE_TEST", "🎙️ Speech started")
                txtVoiceResponse.text = "🎙️ Speak now…"
            }

            override fun onEndOfSpeech() {
                Log.d("VOICE_TEST", "🛑 Speech ended")
                txtVoiceResponse.text = "Processing…"
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO                       -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT                      -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS    -> "Permission error"
                    SpeechRecognizer.ERROR_NETWORK                     -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH                    -> "No match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY             -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER                      -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT              -> "Speech timeout"
                    else                                                -> "Unknown error"
                }
                Log.e("VOICE_TEST", "❌ onError: $msg ($error)")
                txtVoiceResponse.text = "❌ $msg"
                Toast.makeText(this@VoiceTestActivity, msg, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?: "Nothing recognized"
                Log.d("VOICE_TEST", "🗣️ Recognized: $spoken")
                txtVoiceInput.text    = spoken
                txtVoiceResponse.text = "✅ Done!"
            }

            // unused:
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
        })

        // 6) Start fresh on every click
        btnVoiceMic.setOnClickListener {
            txtVoiceResponse.text = "⏳ Preparing recognizer…"
            Log.d("VOICE_TEST", "🎬 Button clicked — startListening()")

            // *** CLEAN BREAK ***
            speechRecognizer.stopListening()
            speechRecognizer.cancel()

            // *** NEW SESSION ***
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
