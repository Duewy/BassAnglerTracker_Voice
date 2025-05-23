package com.bramestorm.bassanglertracker.training

import android.app.Activity
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.util.Locale

class VoiceInteractionHelper(

    private val activity: AppCompatActivity,
    private val measurementUnit: MeasurementUnit,           //todo for later
    private val isTournament: Boolean,
    private val onCommandAction: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false
    private var lastIntent: Intent? = null

    private var awaitingConfirmation = false
    private var pendingCatch: CatchData? = null
    private var onTranscriptReady: ((String) -> Unit)? = null

    data class CatchData(val pounds: Int, val ounces: Int, val species: String, val clipColor: String)

    init {
        setupTTS()
        setupRecognizer()
    }

    enum class MeasurementUnit {        //todo ... for when we add the other CatchEntry files
        LBS_OZ,
        KG_G,
        INCHES,
        CM
    }

    private fun setupTTS() {
        tts = TextToSpeech(activity) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    private fun setupRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("Voice", "🎙️ Ready for speech...")
                speak("I'm listening.")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) ?: ""
                val transcript = matches?.firstOrNull()?.trim() ?: ""

                Log.d("VCC", "Transcript: $transcript")

                onCommandAction(transcript)

                Log.d("Voice", "✅ Result: $spokenText")

                // 1) Try correcting to a known tournament command
                VoiceCommandCorrector
                    .bestMatch(transcript, VoiceCommandList.knownTournamentCommands)
                    ?.let { cmd ->
                        onCommandAction(cmd)    // e.g. “add a catch”
                        return
                    }

                // 2) Otherwise proceed with weight/species/clip parsing…
                if (awaitingConfirmation) {

                    val species = extractSpecies(spokenText)
                    val clipColor = extractClipColor(spokenText)
                    Log.d("VCC_TRANSCRIPT", "Full transcript: $spokenText")
                    Log.d("VCC_PARSING", "→ Species: $species, Clip Color: $clipColor")
                    handleConfirmation(spokenText)
                    return
                }

                // Parse weight
                val weight = extractWeight(spokenText)
                if (weight == null || (weight.first == 0 && weight.second == 0)) {
                    speak("I couldn't get the weight. Please say the weight in pounds and ounces.")
                    restartListening()
                    return
                }

                // Parse species
                val species = extractSpecies(spokenText)
                if (species.isEmpty()) {
                    speak("What species of fish did you catch?")
                    restartListening()
                    return
                }

                // Parse clip color
                val clipColor = extractClipColor(spokenText)
                if (clipColor.isEmpty()) {
                    speak("What clip color did you use?")
                    restartListening()
                    return
                }
                // Fill In Species if Missing
                if (species.isEmpty()) {            //todo Why are we duplicating this???
                    Log.w("VCC", "⚠️ No species detected in input: $spokenText")
                    speak("What species of fish did you catch?")
                    restartListening(2500)
                    return
                }

                // Fill In Clip_Color if Missing
                if (clipColor.isEmpty()) {
                    Log.w("VCC", "⚠️ No clip color detected in input: $spokenText")
                    speak("What clip color did you use?")
                    restartListening(2500)
                    return
                }


                // Store temporary catch
                pendingCatch = CatchData(weight.first, weight.second, species, clipColor)
                awaitingConfirmation = true


                speak("You just caught a $species that weighs ${weight.first} pounds and ${weight.second} ounces and you put it on the $clipColor. Is that correct?")
                restartListening(4000)
            }

            private var vccRetryCount = 0

            override fun onError(error: Int) {
                isListening = false
                VoiceErrorHandler.handleError(
                    activity = activity,
                    errorCode = error,
                    retryCount = vccRetryCount,
                    onRetry = {
                        vccRetryCount++
                        speechRecognizer?.startListening(lastIntent)
                    },
                    onFallback = {
                        activity.finish() // drop back to your manual flow
                    }
                )
            }


            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleConfirmation(input: String) {

        if (!awaitingConfirmation) {
            Log.w("VCC", "⚠️ Ignored stale confirmation. Already processed.")
            return
        }

        Log.d("VCC", "🧠 User confirmation received: \"$input\"")
        awaitingConfirmation = false

        val c = pendingCatch
        if (c == null) {
            Log.e("VCC", "❌ Cannot finalize catch – pendingCatch is null")
            speak("I lost the catch details. Let's try again.")
            restartListening(2500)
            return
        }

        pendingCatch = null // ✅ Prevent accidental reuse
        val totalOz = c.pounds * 16 + c.ounces

        Log.d("VCC", "✅ Finalizing catch: ${c.pounds} lbs ${c.ounces} oz | ${c.species} | Clip=${c.clipColor} | totalOz=$totalOz")

        val resultIntent = Intent().apply {
            putExtra("weightTotalOz", totalOz)
            putExtra("selectedSpecies", c.species)
            putExtra("clip_color", c.clipColor)
            putExtra("catchType", "Tournament")
            putExtra("isTournament", true)
        }

        speak("Catch saved.")
        Handler(Looper.getMainLooper()).postDelayed({
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }, 2000)

        VoiceCommandCorrector
            .bestMatch(input, VoiceCommandList.knownTournamentCommands)
            ?.let { cmd ->
                onCommandAction(cmd)
                return
            }

    }



    private fun extractWeight(text: String): Pair<Int, Int>? {
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15
        )

        var pounds = -1
        var ounces = -1
        val words = text.split(" ")

        for (i in words.indices) {
            val number = numberWords[words[i]] ?: words[i].toIntOrNull() ?: continue
            when {
                i + 1 < words.size && words[i + 1].contains("pound") -> pounds = number
                i + 1 < words.size && words[i + 1].contains("ounce") -> ounces = number
                pounds == -1 -> pounds = number
                ounces == -1 -> ounces = number
            }
        }

        if (pounds == -1) pounds = 0
        if (ounces == -1) ounces = 0
        return Pair(pounds, ounces)
    }

    private fun extractSpecies(text: String): String {
        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(activity)  // or your source
        val species = VoiceInputMapper.getSpeciesFromVoice(text, speciesList)

        return if (species == "Unrecognized") "" else species
    }


    private fun extractClipColor(text: String): String {
        return VoiceInputMapper.getClipColorFromVoice(text)
    }


    fun startListening(onResult: (String) -> Unit = {}) {
        if (isListening) {
            Toast.makeText(activity, "Already listening...", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        onTranscriptReady = onResult
        // force the audio path for noise-reduced model
        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION


        lastIntent = intent
        onTranscriptReady = onResult

        speechRecognizer?.startListening(intent)
        isListening = true
    }


    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }


    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        stopListening()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    private fun restartListening(delay: Long = 1500) {
        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, delay)
    }
}
