package com.bramestorm.bassanglertracker.training

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceCommandManager(
    private val activity: Activity,
    private val onCommandReceived: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onAlreadyListening: (() -> Unit)? = null
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isRestarting = false
    private var canStartListening = true
    private var consecutiveFailures = 0
    private val maxFailures = 5

    private var speechRecognizer: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(activity)
    private val speechIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }

    init {
        setupListener()
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("Voice", "✅ Ready for speech — start talking now...")
            }

            override fun onBeginningOfSpeech() {
                Log.d("Voice", "🎙️ Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("Voice", "🛑 Detected end of speech — processing...")
            }

            override fun onError(error: Int) {
                isListening = false
                canStartListening = true

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    else -> "Unknown error code: $error"
                }

                Log.e("Voice", "❌ Error: $errorMsg")
                onError(errorMsg)

                consecutiveFailures++
                if (consecutiveFailures >= maxFailures) {
                    Log.e("Voice", "💣 Too many failures. Stopping voice recognition.")
                    stopListening()
                    return
                }

                handler.postDelayed({
                    restartListening()
                }, 1500)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                canStartListening = true
                consecutiveFailures = 0

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: ""
                Log.d("Voice", "🎤 Heard: $spokenText")

                onCommandReceived(spokenText)

                if (!spokenText.lowercase().contains("over and out")) {
                    restartListening()
                } else {
                    Log.d("Voice", "👋 Conversation ended by user")
                    stopListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            onError("Speech recognition not available on this device.")
            return
        }

        if (isListening || !canStartListening) {
            Log.w("Voice", "🔁 Recognizer is busy or locked out")
            onAlreadyListening?.invoke()
            return
        }

        isListening = true
        canStartListening = false

        handler.postDelayed({
            canStartListening = true
        }, 3000)

        Log.d("Voice", "🎤 Starting speech recognition...")
        speechRecognizer?.startListening(speechIntent)
    }

    private fun restartListening() {
        if (isRestarting) return
        isRestarting = true

        handler.postDelayed({
            try {
                speechRecognizer?.stopListening()
                handler.postDelayed({
                    Log.d("Voice", "🔁 Restarted listening")
                    speechRecognizer?.startListening(speechIntent)
                    isRestarting = false
                }, 800)
            } catch (e: Exception) {
                Log.e("Voice", "❌ Failed to restart recognizer", e)
                isRestarting = false
            }
        }, 800)
    }


    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        isListening = false
        Log.d("Voice", "🎤 Stopped listening")
    }

    fun shutdown() {
        Log.d("Voice", "🔻 VoiceCommandManager shutting down recognizer")
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }




}
