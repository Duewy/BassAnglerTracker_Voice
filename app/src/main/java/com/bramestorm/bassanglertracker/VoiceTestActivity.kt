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

    private val REQUEST_AUDIO = 101

    private lateinit var btnMic: Button
    private lateinit var txtStatus: TextView
    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_test)

        btnMic      = findViewById(R.id.btnVoiceMic)
        txtStatus   = findViewById(R.id.txtVoiceResponse)
        txtResult   = findViewById(R.id.txtVoiceInput)

        // 1) — Runtime permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO
            )
        }

        // 2) — Create recognizer + intent once
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        // 3) — Attach listener (logs every step)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE_TEST","✅ onReadyForSpeech")
                txtStatus.text = "🎧 Ready to listen…"
            }
            override fun onBeginningOfSpeech() {
                Log.d("VOICE_TEST","🎙 onBeginningOfSpeech")
                txtStatus.text = "🎙 Speak now…"
            }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() {
                Log.d("VOICE_TEST","🛑 onEndOfSpeech")
                txtStatus.text = "⏳ Processing…"
            }
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO                -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT               -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing permission"
                    SpeechRecognizer.ERROR_NETWORK              -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT      -> "Timeout"
                    SpeechRecognizer.ERROR_NO_MATCH             -> "No match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY      -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER               -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT       -> "No speech"
                    else                                       -> "Unknown ($error)"
                }
                Log.e("VOICE_TEST","❌ onError: $msg")
                txtStatus.text = "❌ $msg"
                Toast.makeText(this@VoiceTestActivity, msg, Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.getOrNull(0)
                    ?: "— nothing —"
                Log.d("VOICE_TEST","🗣 onResults: $spoken")
                txtResult.text = spoken
                txtStatus.text = "✅ Done"
            }
            override fun onPartialResults(partial: Bundle?) { }
            override fun onEvent(eventType: Int, params: Bundle?) { }
        })

        // 4) — Kick off listening (always cancel first)
        btnMic.setOnClickListener {
            Log.d("VOICE_TEST","▶️ Button clicked — startListening()")
            txtStatus.text = "⏳ Initializing…"
            speechRecognizer.cancel()
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    // 5) — If you need to handle the permission result:
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) {
            val ok = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            Log.d("VOICE_TEST","🎤 Audio permission granted? $ok")
            if (!ok) {
                Toast.makeText(this, "Audio permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }
}
