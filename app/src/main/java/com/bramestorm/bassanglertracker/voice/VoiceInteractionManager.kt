package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import java.util.Locale

/**
 * UI abstraction for feedback: TTS and Toasts.
 */
interface VoiceUiHelper {
    /** Speak the given text via TTS. */
    fun speak(text: String)
    /** Show a brief toast with the given message. */
    fun showToast(message: String)
}

/**
 * Manages the full TTS→listen→parse→confirm flow for voice catch entry.
 */
class VoiceInteractionManager(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,

    /**
     * Inject a parser that knows how to extract weight/species/clipColor/etc
     * from raw speech and produce a ConfirmedCatch or retry/fallback prompts.
     */
    var parser: VoiceCommandParser
) {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0      // Hold Error Loop to 3 then exit Vcc
    private val maxRetries = 3
    private val sessionId = System.currentTimeMillis()


    /**
     * Start a new voice session: speak the initial prompt, then listen.
     * @param prompt the TTS prompt to ask the user
     * @param onCatchConfirmed callback when parsing yields a confirmed catch
     */
    fun startSession(
        prompt: String,
        onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit
    ) {
        // initialize TTS
        tts?.stop()
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts!!.language = Locale.getDefault()

                val isConfirmation = prompt.contains("is that correct?", ignoreCase = true)
                val utteranceId = if (isConfirmation) "TTS_CONFIRM" else "TTS_PROMPT"
                Log.d("VCC_MANAGER", "🎬 [$sessionId] startSession() called")

                Log.d("VCC_TTS", "🗣️ Speaking with ID: $utteranceId → \"$prompt\"")
                tts!!.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            }
        }

        tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) {}
            override fun onError(uttId: String?) {}
            override fun onDone(uttId: String?) {
                when (uttId) {
                    "TTS_PROMPT", "TTS_CONFIRM", "TTS_SAVED", "TTS_RETRY"  -> {
                        Log.d("VCC_TTS", "✅ TTS finished: $uttId — starting STT...")
                        Log.d("VCC_MANAGER", "✅ [$sessionId] TTS done — triggering STT")
                        handler.postDelayed({ startListening(onCatchConfirmed) }, 500)
                    }
                }
            }
        })
    }
    fun listenForConfirmation(onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit) {
        startListening(onCatchConfirmed)
    }

    private fun speakWithId(text: String, utteranceId: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun startListening(onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit) {
        Log.d("VCC_MANAGER", "▶️ [$sessionId] startListening() entered")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {       //todo There is no sign of this toast on the screen??? I may be wrong but ..
            val toast = Toast.makeText(context, "🎤 Listening now —\nsay 'Yes Over' or 'No Over'", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            toast.show()
        }

        // reset previous recognizer
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    retryCount++
                    Log.w("VoiceSession", "⚠️ Voice retry #$retryCount (error=$error)")
                    Log.d("VCC_MANAGER", "❌ [$sessionId] onError $error — retry #$retryCount")

                    if (retryCount >= maxRetries) {
                        uiHelper.speak("Sorry, I'm having trouble hearing you. Try again later.")
                        Log.e("VoiceSession", "❌ Max retries reached. Ending voice session.")
                        shutdown()
                    } else {
                        uiHelper.speak("Sorry, I didn't catch that. Please try again. Over.")
                        handler.postDelayed({ startListening(onCatchConfirmed) }, 1500)
                    }
                }


                override fun onResults(results: Bundle?) {

                    retryCount = 0  // ✅ Reset on successful voice result
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    Log.d("VCC_MANAGER", "📥 [$sessionId] onResults() received: $spoken")

                    if (!transcript.lowercase().contains("over")) {
                        tts?.speak("Did you forget to say over?", TextToSpeech.QUEUE_FLUSH, null, "TTS_MISSING_OVER")
                        startListening(onCatchConfirmed)// restart listening
                        return
                    }

                    when (val result = parser.parse(spoken)) {
                        is VoiceCommandParser.ParseResult.Confirm -> {
                            Log.d("VCC_MANAGER", "✅ [$sessionId] ParseResult.Confirm — speaking confirmation")
                            speakWithId(result.confirmationPrompt, "TTS_CONFIRM")
                            parser.awaitConfirmation(result.catch, onCatchConfirmed)
                        }
                        is VoiceCommandParser.ParseResult.Retry -> {
                            uiHelper.speak(result.retryPrompt)
                            Log.d("VCC_MANAGER", "🔄 [$sessionId] ParseResult.Retry — prompting retry")
                            handler.postDelayed({ startListening(onCatchConfirmed) }, 1000)
                        }
                        is VoiceCommandParser.ParseResult.Failure -> {
                            Log.d("VCC_MANAGER", "🛑 [$sessionId] ParseResult.Failure — prompting retry")
                            uiHelper.speak(result.fallbackPrompt)
                        }
                    }
                }

                override fun onPartialResults(partial: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }.also { recognizer?.startListening(it) }

    }// == END == Start Listening =================

    /**
     * Clean up TTS & recognizer when shutting down the service.
     */
    fun shutdown() {
        Log.d("VCC_MANAGER", "🧹 [$sessionId] shutdown() called")
        tts?.stop()
        tts?.shutdown()
        recognizer?.destroy()
    }
} // === END== Voice Interaction Manager =========================


/*****  Parser Interface & Data Class  *****/

interface VoiceCommandParser {
    /**
     * Parse raw user speech into one of the outcomes:
     * - Confirm with a catch object + prompt
     * - Retry with a retry-prompt
     * - Failure with a fallback-prompt
     */
    fun parse(input: String): ParseResult

    /**
     * After speaking the confirmation, listen for yes/no and invoke onConfirmed.
     */
    fun awaitConfirmation(
        lastCatch: ConfirmedCatch,  // todo should we add the "over" to this??? It works just not patterned
        onConfirmed: (ConfirmedCatch) -> Unit
    )

    sealed class ParseResult {
        data class Confirm(
            val catch: ConfirmedCatch,
            val confirmationPrompt: String
        ) : ParseResult()

        data class Retry(val retryPrompt: String) : ParseResult()
        data class Failure(val fallbackPrompt: String) : ParseResult()
    }

    /**
     * Unified catch representation, supports multiple measure types:
     */
    data class ConfirmedCatch(
        val weightOz: Int? = null,
        val weightKgs: Int? = null,
        val lengthQuarters: Int? = null,
        val lengthTenths: Int? = null,
        val species: String,
        val clipColor: String
    )

} //=== END == Voice Command Parser =========
