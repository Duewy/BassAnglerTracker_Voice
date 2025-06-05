package com.bramestorm.bassanglertracker.training

// This is for the User Training Section where we evaluate the User's ability to have the words/phrase recognized by the STT function.

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.utils.positionedToast
import java.util.Locale

class VoiceInteractionHelper(
    private val activity: AppCompatActivity,
    private val measurementUnit: MeasurementUnit,
    private val isTournament: Boolean,
    private val onCommandAction: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false
    private var lastIntent: Intent? = null

    private var retryCount = 0
    private val maxRETRIES = 3

    private var awaitingConfirmation = false
    private var pendingCatch: CatchData? = null
    private val sessionId = System.currentTimeMillis()


    data class CatchData(val pounds: Int, val ounces: Int, val species: String, val clipColor: String)//todo why only pounds ounces?????

    enum class MeasurementUnit {
        LBS_OZ, KG_G, INCHES, CM        //todo why is CM not Used???
    }

    init {
        setupTTS()
        setupRecognizer()
    }

    private fun setupTTS() {
        tts = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
    }

    private fun setupRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("VCC_HELPER", "🎙️ [$sessionId] Ready for speech...")

                    speak("I'm listening. Over")
                }

                override fun onResults(results: Bundle?) {
                    Log.d("VCC_HELPER", "📥 [$sessionId] onResults() fired in VoiceInteractionHelper")

                    // existing onResults logic remains unchanged
                    onCommandAction(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) ?: "")
                }

                override fun onError(error: Int) {
                    Log.d("VCC_HELPER", "❌ onError() — retry $retryCount in VoiceInteractionHelper")

                    if (retryCount < maxRETRIES) {
                        retryCount++
                        speak("Sorry, I didn't catch that. Please try again. Over")
                        restartListening()
                    } else {
                        speak("Voice input failed multiple times—please try manual entry. Over and out")
                        activity.finish()
                    }
                    isListening = false
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening(onResult: (String) -> Unit = {}) {

        Log.d("VCC_HELPER", "▶️ startListening() called in VoiceInteractionHelper")

        if (isListening) {
            activity.positionedToast("Already listening..👍.")
            return
        }
        retryCount = 0
        lastIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra("android.speech.extra.MINIMUM_LENGTH_MILLIS", 5000L)
        }

        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        speechRecognizer?.startListening(lastIntent)
        isListening = true
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    private fun restartListening(delay: Long = 1000) {
        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, delay)
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        Log.d("VCC_HELPER", "🧹 VoiceInteractionHelper shutdown() called")

        stopListening()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
