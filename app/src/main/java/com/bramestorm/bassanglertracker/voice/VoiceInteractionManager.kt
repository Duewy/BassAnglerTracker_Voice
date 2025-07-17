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


    fun startSession(
        prompt: String,
        onResult: (String) -> Unit,
        onFailure: (() -> Unit)? = null )
    {
        this.onFailureCallback = onFailure
        tts?.stop()
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts!!.language = Locale.getDefault()

                val utteranceId = if (prompt.contains("is that correct?", true)) "TTS_CONFIRM" else "TTS_PROMPT"
                Log.d("VCC_MANAGER", "🎬 [$sessionId] startSession() called")
                Log.d("VCC_TTS", "🗣️ Speaking with ID: $utteranceId → \"$prompt\"")

                tts!!.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }
        onTranscriptResult = onResult

        tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) {}
            override fun onError(uttId: String?) {}
            override fun onDone(uttId: String?) {
                when (uttId) {
                    "TTS_PROMPT", "TTS_CONFIRM", "TTS_SAVED", "TTS_RETRY" -> {
                        Log.d("VCC_TTS", "✅ TTS finished: $uttId — starting STT...")
                        handler.post { startListening()}
                    }
                }
            }
        })
    }

            // Set Up Times for VCC Interactions... To ensure the User has ample time to say the Catch Information
    private fun startListening() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000)
        }

        Log.d("VCC_STT", "🎤 STT timeout settings applied (Complete=6000ms, Possible=4000ms, Min=4000ms)")


        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                retryOrFail()
            }

            override fun onResults(results: Bundle?) {
                val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (transcript != null) {
                    onTranscriptResult?.invoke(transcript)
                } else {
                    retryOrFail()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(intent)
    }


    private fun retryOrFail() {
        retryCount++
        if (retryCount > maxRetries) {
            uiHelper.speak("Too many errors. Please try again later.", "TTS_FAIL")
            onFailureCallback?.invoke()
        } else {
            uiHelper.speak("Sorry, please repeat your catch. Over.", "TTS_RETRY")
            handler.postDelayed({
                startSession("Please say your catch details again. Over.", onTranscriptResult ?: return@postDelayed)
            }, 3500)
        }
    }

    fun shutdown() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        onTranscriptResult = null
        Log.d("VCC_SESSION", "🛑 VoiceInteractionManager shut down.")
    }

}//=== END == Voice Interaction Manager =======================================