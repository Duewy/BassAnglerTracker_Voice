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

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDead = false

    private val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("Voice", "✅ Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("Voice", "🎙️ Speech detected")
                }

                override fun onEndOfSpeech() {
                    Log.d("Voice", "🛑 End of speech")
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false

                    val msg = when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            "Recognizer busy"
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            "Client error"
                        }
                        else -> "Error $error"
                    }

                    Log.e("Voice", "❌ $msg")

                    // DO NOT RESTART IF BUSY OR CLIENT
                    if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        Log.e("Voice", "💣 Critical error. Voice shut down.")
                        isDead = true
                        stopListening()
                        return
                    }

                    // Other errors (like timeout) can be user-managed
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    Log.d("Voice", "🎤 Heard: $result")

                    if (!result.isNullOrBlank()) {
                        onCommandReceived(result)
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (isDead) {
            Log.w("Voice", "⚠️ Recognizer disabled due to previous fatal error.")
            return
        }

        if (isListening) {
            onAlreadyListening?.invoke()
            Log.w("Voice", "⚠️ Already listening")
            return
        }

        try {
            Log.d("Voice", "🎤 Starting recognition...")
            speechRecognizer?.startListening(speechIntent)
            isListening = true
        } catch (e: Exception) {
            Log.e("Voice", "❌ Failed to start listening", e)
            isListening = false
        }
    }

    fun stopListening() {
        try {
            Log.d("Voice", "🎤 Stopping recognition")
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e("Voice", "❌ Stop error", e)
        }
        isListening = false
    }

    fun shutdown() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
