package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
import com.bramestorm.bassanglertracker.R
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

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0      // Hold Error Loop to 3 then exit Vcc
    private val maxRetries = 3
    private var isRetrying = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false


    /**
     * Start a new voice session: speak the initial prompt, then listen.
     * @param prompt the TTS prompt to ask the user
     * @param onCatchConfirmed callback when parsing yields a confirmed catch
     */
            // === CORE ENTRY-POINTS ===
    fun startSession(prompt: String, onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit) {
        ensureTtsReady {
            val isConfirmation = prompt.contains("is that correct?", ignoreCase = true)
            val utteranceId = if (isConfirmation) "TTS_CONFIRM" else "TTS_PROMPT"

            Log.d("VCC_TTS", "🗣️ Speaking with ID: $utteranceId → \"$prompt\"")

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uttId: String?) {}
                override fun onError(uttId: String?) {}
                override fun onDone(uttId: String?) {
                    if (uttId in listOf("TTS_PROMPT", "TTS_CONFIRM", "TTS_SAVED", "TTS_MISSING_OVER")) {
                        Log.d("VCC_TTS", "✅ TTS finished: $uttId — playing chime + STT")
                        playChimeThenListen(500, onCatchConfirmed)
                    }
                }
            })

            tts?.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }



    // Optional semantic wrapper — currently unused
    fun listenForConfirmation(onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit) {
        startListening(onCatchConfirmed)
    }

    private fun speakWithId(text: String, utteranceId: String) {
        if (tts?.isSpeaking == true) {
            Log.d("VCC_TTS", "🔇 Skipping speak — TTS still talking")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    // === LISTENING + PARSING ===
    private fun startListening(onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit) {

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

                    if (retryCount >= maxRetries) {
                        uiHelper.speak("Sorry, I'm having trouble hearing you. Try Entering your Catch again later. Or use the Manual Mode, Over and Out")
                        Log.e("VoiceSession", "❌ Max retries reached. Ending voice session.")
                        shutdown()
                    } else {
                        uiHelper.speak("Sorry, I didn't catch that. Please try again. Over.")
                        handler.postDelayed({
                            isRetrying = false
                            startListening(onCatchConfirmed)
                        }, 3000) //todo check that this is time for user not until start...
                    }
                }


                override fun onResults(results: Bundle?) {
                    retryCount = 0  // ✅ Reset on successful voice result
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return

                    if (!transcript.lowercase().contains("over")) {
                        Log.d("VCC_TTS", "⚠️ Missing 'over' — restarting with prompt")

                        val message = "Did you forget to say over? Please say your catch information again, over"

                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(uttId: String?) {}
                            override fun onError(uttId: String?) {}
                            override fun onDone(uttId: String?) {
                                if (uttId == "TTS_MISSING_OVER") {
                                    Log.d("VCC_TTS", "✅ Missing 'over' prompt finished — playing chime + STT")
                                    playChimeThenListen(500, onCatchConfirmed)
                                }
                            }
                        })

                        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "TTS_MISSING_OVER")
                        return
                    }


                    when (val result = parser.parse(spoken)) {
                                // ALL is good 👍 Catch Saved Waiting for the Next One
                        is VoiceCommandParser.ParseResult.Confirm -> {
                            speakWithId(result.confirmationPrompt, "TTS_CONFIRM")
                            parser.awaitConfirmation(result.catch, onCatchConfirmed)
                        }

                            // Need to Retry due to Poor STT results
                        is VoiceCommandParser.ParseResult.Retry -> {
                            Log.d("VCC_RETRY", "🔄 Retry requested — prompt: ${result.retryPrompt}")

                            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(uttId: String?) {}
                                override fun onError(uttId: String?) {}
                                override fun onDone(uttId: String?) {
                                    if (uttId == "TTS_RETRY") {
                                        Log.d("VCC_RETRY", "✅ Retry prompt finished — playing chime + STT")
                                        playChimeThenListen(500, onCatchConfirmed)
                                    }
                                }
                            })

                            tts?.speak(result.retryPrompt, TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY")
                        }

                            // Complete Failure on User STT
                        is VoiceCommandParser.ParseResult.Failure -> {
                            Log.d("VCC_FAILURE", "🛑 Failure fallback — prompt: ${result.fallbackPrompt}")

                            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(uttId: String?) {}
                                override fun onError(uttId: String?) {}
                                override fun onDone(uttId: String?) {
                                    if (uttId == "TTS_FAILURE") {
                                        Log.d("VCC_FAILURE", "✅ Failure prompt finished — playing chime + STT")
                                        playChimeThenListen(500, onCatchConfirmed)
                                    }
                                }
                            })

                            tts?.speak(result.fallbackPrompt, TextToSpeech.QUEUE_FLUSH, null, "TTS_FAILURE")
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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L) // sets up 3 seconds for User 🔊 to start speaking
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }.also { recognizer?.startListening(it) }

    }// == END == Start Listening =================

        // === TTS / STT UTILITIES ===

    /**
     * Ensures the TextToSpeech engine is ready before speaking.
     * If already initialized, immediately calls [onReady].
     */
    private fun ensureTtsReady(onReady: () -> Unit) {
        if (ttsReady) {
            onReady()
            return
        }

        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    ttsReady = true
                    Log.d("VCC_TTS", "✅ TTS initialized — ready to speak")
                    onReady()
                } else {
                    Log.e("VCC_TTS", "❌ TTS failed to initialize (status=$status)")
                }
            }
        }
    }

    /** Plays chime, then delays before starting STT */
    private fun playChimeThenListen(delayMs: Long = 100, onCatchConfirmed: (VoiceCommandParser.ConfirmedCatch) -> Unit) {
        val player = MediaPlayer.create(context, R.raw.chime_sound)
        if (player == null) {                                               // just in case the media file is corrupt
            Log.e("VCC_CHIME", "❌ Failed to load chime — skipping to STT")
            handler.postDelayed({ startListening(onCatchConfirmed) }, delayMs)
            return
        }

        player.setOnCompletionListener {
            it.release()
            Log.d("VCC_CHIME", "🎵 Chime complete — starting STT in ${delayMs}ms")
            handler.postDelayed({ startListening(onCatchConfirmed) }, delayMs)
        }
        player.start()
    }

    /**
     * Clean up TTS & recognizer when shutting down the service.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
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
            lastCatch: ConfirmedCatch,
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
