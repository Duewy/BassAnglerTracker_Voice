package com.bramestorm.bassanglertracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.utils.positionedToast
import java.util.Locale


class PopupVccLengthEntryInches: Activity() {

    // Flags and extras
    private var selectedSpecies: String = ""
    private var speechRecognizer: SpeechRecognizer? = null
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null
    private lateinit var tts: TextToSpeech

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var edtLengthInchesTens: Spinner
    private lateinit var edtLengthInchesOnes: Spinner
    private lateinit var edtLengthInchesQuarters: Spinner
    private lateinit var btnCancel: Button


    companion object {
        const val EXTRA_LENGTH_INCHES     = "lengthTotalInches"
        const val EXTRA_SPECIES       = "selectedSpecies"
    }


    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_vcc_funday_inches)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        // 🔊 !!!!!!!!!!!!! VCC Says !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 🔊
        speak("Please say the Length and the Species of the Catch Over")


        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesVCCInchesFD)
        edtLengthInchesTens = findViewById(R.id.spinnerInchesTensFD)
        edtLengthInchesOnes = findViewById(R.id.spinnerInchesOnesFD)
        edtLengthInchesQuarters = findViewById(R.id.spinnerInchesQuartersFD)
        btnCancel = findViewById(R.id.btnCancel)


        // ++++++++++++ Setup Weight Spinners ++++++++++++++++++++++++++
        // Tens place: 0–9 (represents 0–90 inches)
        val tensOptions = (0..9).map { it.toString() }
        val tensAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tensOptions)
        tensAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtLengthInchesTens.adapter = tensAdapter

        // Ones place: 0–9 (represents 0–9 inches)
        val onesOptions = (0..9).map { it.toString() }
        val onesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, onesOptions)
        onesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtLengthInchesOnes.adapter = onesAdapter

        // Quarters: 0–3
        val quartersOptions = (0..3).map { it.toString() }
        val quartersAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, quartersOptions)
        quartersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtLengthInchesQuarters.adapter = quartersAdapter

        // Optionally set default selection to 0
        edtLengthInchesTens.setSelection(0)
        edtLengthInchesOnes.setSelection(0)
        edtLengthInchesQuarters.setSelection(0)


        // ````````` CANCEL btn ```````````````````
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//==============  END ON CREATE =================================

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }


    private fun speak(message: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "TTS_DONE")
            }
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "TTS_DONE") {
                    runOnUiThread {
                        startListening()  // 🎤 Start listening after TTS
                    }
                }
            }
            override fun onError(utteranceId: String?) {}   // todo do we need something for Error catching?
        })
    }

    //------------------- startListening --------------------------
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            // Give the user up to 2.5 seconds of silence to finish speaking
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2500L
            )
            // Require at least 1 second of speech before thinking user is done
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1000L
            )
            // Ask for partial results (optional)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Only need your best match
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: "No input"
                Log.d("VCC", "🎤 User said: $spokenText")
                handleVoiceInput(spokenText)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {                                               //todo fix up ERROR Catching...
                Log.e("VCC", "Speech recognition error: $error")
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Let the user know and try again
                        tts.speak(
                            "I did not catch that. Please say yes or no after the beep.",
                            TextToSpeech.QUEUE_FLUSH, null, "TTS_REPEAT"
                        )
                        Handler(mainLooper).postDelayed({ startListening() }, 1200)
                    }
                    else -> {
                        // Fallback for other errors
                        tts.speak(
                            "Something went wrong with speech recognition. Please try again.",
                            TextToSpeech.QUEUE_FLUSH, null, "TTS_ERROR"
                        )
                    }
                }
            }

        })
        speechRecognizer?.startListening(intent)
    }//------------------- End of startListening --------------------------

    //============= 👂Gets Users Info 📖 and Puts Everything for  Database and Listing =======================

    data class ConfirmedCatch(val totalLengthQuarters: Int, val species: String)

    //===============================================
    private fun handleVoiceInput(input: String) {
        val lower = input.lowercase(Locale.getDefault()).trim()
        Log.d("VCC", "🎤 Raw speech: $lower")

        // 1) If we’re waiting for a “yes”/“no” confirmation, handle it first:
        if (awaitingConfirmation) {
            when {
                lower.contains("yes") -> {
                    Log.d("VCC", "👂 User input was confirmed now sending to CatchEntryTournamentInches.kt ")

                    lastConfirmedCatch?.let { (totalLengthQuarters, species) ->

                        returnTournamentResult(totalLengthQuarters, species)
                    }

                }

                lower.contains("no") -> {
                    awaitingConfirmation = false
                    tts.speak(
                        "Okay, let's start over. Please say the length species and clip then say over when you are done.",
                        TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY"
                    )
                    Handler(mainLooper).postDelayed({ startListening() }, 1500)
                }
                else -> {
                    tts.speak(
                        "Please say 'yes' to confirm or 'no' to start over.",
                        TextToSpeech.QUEUE_FLUSH, null, "TTS_REPEAT_CONFIRM"
                    )
                    Handler(mainLooper).postDelayed({ startListening() }, 1200)
                }
            }
            return
        }

        // 2) Normal flow: user must say “over” to finish their catch entry
        if (!lower.contains("over")) {
            speak("You must say the key word OVER to finish your catch entry voice command.")

            positionedToast("⚠️ Say the key word 'OVER' ‼️ to finish your catch entry voice command.")

            return
        }

        // 3) Strip off “over” and clean up
        val cleaned = lower
            .replace("over and out", "")
            .replace("over", "")
            .trim()
        Log.d("VCC", "🧹 Cleaned input: $cleaned")

        // 4) Cancel keywords
        val cancelPhrases = listOf("cancel", "that is wrong", "start over", "not right")
        if (cancelPhrases.any { cleaned.contains(it) }) {
            Log.d("VCC", "❌ Cancel voice command detected. Restarting...")
            tts.speak(
                "Okay, let's try again. Please say the weight and say over when done.",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 2000)
            return
        }

        // 5) Parse length (inches and quarters)
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15
        )
        var inches = -1
        var quarters = -1
        val words = cleaned.split("\\s+".toRegex())
        for ((i, word) in words.withIndex()) {
            val num = numberWords[word] ?: word.toIntOrNull()
            if (num != null) {
                when {
                    i + 1 < words.size && words[i + 1].contains("inches")  -> inches = num
                    i + 1 < words.size && words[i + 1].contains("quarters")  -> quarters = num
                    inches < 0                                            -> inches = num
                    quarters < 0                                            -> quarters = num
                }
            }
        }
        if (inches < 0) inches = 0
        if (quarters < 0) quarters = 0
        val totalLengthQuarters = ((inches * 4) + quarters)
        if (totalLengthQuarters == 0) {
            Toast.makeText(this, "🚫 Length cannot be 0 inches 0 quarters!", Toast.LENGTH_SHORT).show()
            return
        }
        // ENSURE Quarters is 0 to 3
        if (quarters > 3) {
            tts.speak(
                "Sorry, quarters can only be zero to three. Please repeat your length.",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 1500)
            return
        }


// 6) Parse and normalize species via the user-defined mapper
        val rawSpeciesPhrase = cleaned    // e.g. "lark mouth" or "perch"
        val normalizedSpecies = VoiceInputMapper.normalizeSpecies(rawSpeciesPhrase)
        if (normalizedSpecies == null)  {
            // Did not recognize a valid species—prompt again
            tts.speak(
                "I didn't catch the species. Please say the species name over.",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_ASK_SPECIES"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }
        selectedSpecies = normalizedSpecies


        // 7)  Setup Species Spinner from User Input
        val onlyThatList: List<String> = listOf(selectedSpecies)
        val speciesAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, onlyThatList).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerSpecies.adapter = speciesAdapter


        // 8) Update your UI spinners immediately
        edtLengthInchesTens.setSelection(inches / 10)
        edtLengthInchesOnes.setSelection(inches % 10)
        edtLengthInchesQuarters.setSelection(quarters)
        spinnerSpecies.setSelection((spinnerSpecies.adapter as ArrayAdapter<String>)
            .getPosition(selectedSpecies))

        // 9) Ask for confirmation, echoing back exactly what we think we heard
        val lengthPhrase = when (quarters) {
            0 -> "$inches inches"
            1 -> "$inches and a quarter inches"
            2 -> "$inches and a half inches"
            3 -> "$inches and three quarter inches"
            else -> "$inches inches"
        }

        val question = "You said $lengthPhrase of the $selectedSpecies, is that correct Over"

        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

        // 10) Save state and flip the flag
        lastConfirmedCatch = ConfirmedCatch(totalLengthQuarters, selectedSpecies)
        awaitingConfirmation = true

        // 11) Immediately start listening for that “yes” or “no”
        Handler(mainLooper).postDelayed({ startListening() }, 2500)

    } //===================END handle Voice Input  ====================

    // ^^^^^^^^^^ Sending Data to CatchEntryTournament ^^^^^^^^^^^^^^^^
    private fun Activity.returnTournamentResult(
        totalLengthQuarters: Int, species: String) {
        Intent(this, CatchEntryInches::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_LENGTH_INCHES, totalLengthQuarters)
            putExtra(EXTRA_SPECIES,       species)
        }.also {
            startActivity(it)
            finish()
        }
    }

}//================== END  ==========================
