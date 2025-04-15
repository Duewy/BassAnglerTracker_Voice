package com.bramestorm.bassanglertracker.training

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.os.Bundle
import ai.picovoice.porcupine.PorcupineManager

import android.content.pm.PackageManager
import android.widget.Toast


class VoiceInputHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onWakeUp: () -> Unit,
    private val onFinish: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isActiveMode = false
    private var isCooldownActive = false

    private val handler = Handler(Looper.getMainLooper())
    private val wakePhrase = "wake up casper"
    private val overPhrase = "over"
    private val endPhrase = "over and out"

    private val restartDelayMs = 1500L
    private val errorCooldownMs = 5000L
    private var restartRunnable: Runnable? = null

    private var porcupineManager: PorcupineManager? = null


    fun initWakeWordDetection() {
        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey("6jSwOwgQR/mFYJJz4pAF7qhGvDV9Aonyr2FJJyh63FFBPFhoCPSikA==")
                .setKeywordPath("wake_up_casper_android.ppn")
                .build(context) { _ ->
                    Log.d("VoiceInput", "🎤 Wake word detected!")
                    isActiveMode = true
                    onWakeUp()
                    startListening()
                }

            porcupineManager?.start()
            Log.d("VoiceInput", "🚀 Porcupine wake word detection started")

        } catch (e: Exception) {
            Log.e("VoiceInput", "❌ Error initializing Porcupine: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        isListening = false
    }

    fun destroy() {
        shutdownWakeWordDetection()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }


    fun shutdownWakeWordDetection() {
        porcupineManager?.stop()
        porcupineManager?.delete()
        porcupineManager = null
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceInput", "❌ Speech recognition not available")
            return
        }

        if (isListening || isCooldownActive) {
            Log.w("VoiceInput", "⚠️ Already listening or in cooldown")
            return
        }

        val delay = if (speechRecognizer == null) 500L else 0L

        handler.postDelayed({
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}

                        override fun onError(error: Int) {
                            Log.e("VoiceInput", "❌ Error: $error")
                            isListening = false

                            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                                isCooldownActive = true
                                speechRecognizer?.cancel()
                                speechRecognizer?.destroy()
                                speechRecognizer = null
                                handler.postDelayed({
                                    isCooldownActive = false
                                    startListening()
                                }, errorCooldownMs)
                            } else {
                                scheduleRestart()
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull()?.lowercase()?.trim() ?: return
                            Log.d("VoiceInput", "🎤 Heard: $spokenText")
                            isListening = false

                            when {
                                isActiveMode && spokenText.contains(endPhrase) -> {
                                    isActiveMode = false
                                    onFinish()
                                }
                                isActiveMode && spokenText.contains(overPhrase) -> {
                                    onResult(spokenText)
                                }
                                isActiveMode -> {
                                    onResult(spokenText)
                                }
                            }

                            scheduleRestart()
                        }
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                Log.d("VoiceInput", "🎧 Started speech recognizer")

            } catch (e: Exception) {
                Log.e("VoiceInput", "❌ Exception: ${e.message}")
                isListening = false
                scheduleRestart()
            }
        }, delay)
    }

    private fun scheduleRestart() {
        if (isCooldownActive) return
        restartRunnable?.let { handler.removeCallbacks(it) }

        restartRunnable = Runnable {
            startListening()
        }

        handler.postDelayed(restartRunnable!!, restartDelayMs)
    }
}
