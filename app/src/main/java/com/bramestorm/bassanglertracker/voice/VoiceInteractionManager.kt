package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceInteractionManager(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,
    var parser: VoiceParser
) {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var onTranscriptResult: ((String) -> Unit)? = null
    private var retryCount = 0
    private val maxRetries = 3
    private val sessionId = System.currentTimeMillis()
    private var onFailureCallback: (() -> Unit)? = null
    private var firstListenAttempt = true
    private var isListening = false
    private var isShuttingDown = false

    // ── Reusable recognizer intent ──
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
    }

    // ── Reusable recognition listener ──
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VCC_STT", "🎤 Ready for speech")
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            isListening = false
            Log.d("VCC_STT", "❌ STT error code: $error")

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    retryOrFail()
                }

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    Log.w("VCC_STT", "🌐 Network STT error — one delayed retry path")
                    handler.postDelayed({ retryOrFail() }, 1800)
                }

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    Log.w("VCC_STT", "⛔ Non-retryable STT error: $error")
                    onFailureCallback?.invoke()
                }

                else -> {
                    retryOrFail()
                }
            }
        }
        override fun onResults(results: Bundle?) {
            isListening = false
            val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (transcript != null) {
                Log.d("VCC_STT", "✅ STT result: '$transcript'")
                onTranscriptResult?.invoke(transcript)
            } else {
                retryOrFail()
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startSession(
        prompt: String,
        onResult: (String) -> Unit,
        onFailure: (() -> Unit)? = null
    ) {
        this.onFailureCallback = onFailure
        retryCount = 0
        firstListenAttempt = true
        onTranscriptResult = onResult

        // ✅ Reset per new session
        isShuttingDown = false
        isListening = false
        handler.removeCallbacksAndMessages(null)

        // ✅ Ensure recognizer is ready BEFORE TTS can trigger onDone -> startListening()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        tts?.stop()
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(uttId: String?) {}
                    override fun onError(uttId: String?) {}
                    override fun onDone(uttId: String?) {
                        when (uttId) {
                            "TTS_PROMPT", "TTS_CONFIRM", "TTS_SAVED", "TTS_RETRY", "TTS_TIED_ASK" -> {
                                Log.d("VCC_TTS", "✅ TTS finished: $uttId — starting STT...")
                                handler.postDelayed({ startListening() }, 800)
                            }
                        }
                    }
                })

                val utteranceId =
                    if (prompt.contains("is that correct?", true)) "TTS_CONFIRM" else "TTS_PROMPT"

                Log.d("VCC_MANAGER", "🎬 [$sessionId] startSession() called")
                Log.d("VCC_TTS", "🗣️ Speaking with ID: $utteranceId → \"$prompt\"")

                tts?.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                Log.w("VCC_TTS", "❌ TTS init failed: status=$status")
                onFailureCallback?.invoke()
            }
        }
    }

    private fun startListening() {
        // ── Just start listening — recognizer already created ──
        if (isShuttingDown || isListening) return
        isListening = true
        recognizer?.startListening(recognizerIntent)
        Log.d("VCC_STT", "🎤 startListening() called")
    }

    private fun retryOrFail() {
        if (firstListenAttempt) {
            firstListenAttempt = false
            Log.d("VCC_MANAGER", "🔄 First listen attempt failed (TTS→STT handoff) — retrying without counting")
            handler.postDelayed({ startListening() }, 1200)
            return
        }

        retryCount++
        if (retryCount > maxRetries) {
            Log.w("VCC_MANAGER", "❌ Too many STT errors — invoking failure callback")
            onFailureCallback?.invoke()
            retryCount = 0
        } else {
            Log.d("VCC_MANAGER", "🔄 STT retry $retryCount/$maxRetries — restarting listener")
            handler.postDelayed({ startListening() }, 1200)
        }
    }

    fun shutdown() {
        isShuttingDown = true
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        onTranscriptResult = null
        Log.d("VCC_SESSION", "🛑 VoiceInteractionManager shut down.")
    }

}//=== END == Voice Interaction Manager =======================================