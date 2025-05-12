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
import com.bramestorm.bassanglertracker.util.positionedToast
import java.util.Locale


class PopupVccWeightEntryKgs: Activity() {

    // Flags and extras
    private var catchType: String = ""
    private var selectedSpecies: String = ""
    private var speechRecognizer: SpeechRecognizer? = null
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null
    private lateinit var tts: TextToSpeech

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var edtWeightTensKgs: Spinner
    private lateinit var edtWeightKgs: Spinner
    private lateinit var edtWeightKgsTenths: Spinner
    private lateinit var edtWeightKgsHundreds: Spinner
    private lateinit var btnCancel: Button

    companion object {
        const val EXTRA_WEIGHT_KGS = "weightTotalKgs"
        const val EXTRA_SPECIES = "selectedSpecies"
    }


    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_vcc_funday_kgs)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }


        // !!!!!!!!!!!!! VCC Says !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        speak("Please say the weight and the species used for the catch Over")

        catchType = intent.getStringExtra("catchType") ?: ""

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesVCCKgsFD)
        edtWeightTensKgs = findViewById(R.id.spinnerKgsTensFD)
        edtWeightKgs = findViewById(R.id.spinnerKgsOnesFD)
        edtWeightKgsTenths = findViewById(R.id.spinnerKgsTenthsFD)
        edtWeightKgsHundreds = findViewById(R.id.spinnerKgsHundredsFD)
        btnCancel = findViewById(R.id.btnCancel)


        // ++++++++++++ Setup Weight Spinners ++++++++++++++++++++++++++

        // Tens place: 0–9 (represents 0–90 lbs)
        val tensOptions = (0..9).map { it.toString() }
        val tensAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tensOptions)
        tensAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightTensKgs.adapter = tensAdapter

        // Ones place: 0–9 (represents 0–9 lbs)
        val onesOptions = (0..9).map { it.toString() }
        val onesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, onesOptions)
        onesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightKgs.adapter = onesAdapter

        // grams: 0.0–0.9
        val tenthsOptions = (0..9).map { it.toString() }
        val tenthsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tenthsOptions)
        tenthsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightKgsTenths.adapter = tenthsAdapter

        // grams: 0.00–0.09
        val hundredsOptions = (0..9).map { it.toString() }
        val hundredsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hundredsOptions)
        hundredsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightKgsTenths.adapter = hundredsAdapter

        // Optionally set default selection to 0
        edtWeightTensKgs.setSelection(0)
        edtWeightKgs.setSelection(0)
        edtWeightKgsTenths.setSelection(0)
        edtWeightKgsHundreds.setSelection(0)


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
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
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

            override fun onError(error: Int) {
                Log.e("VCC", "Speech recognition error: $error")
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    -> {
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


    data class ConfirmedCatch(val weightTotalKgsFD: Int, val species: String)

    //===============================================
    private fun handleVoiceInput(input: String) {
        val lower = input.lowercase(Locale.getDefault()).trim()
        Log.d("VCC", "🎤 Raw speech: $lower")

        // 1) If we’re waiting for a “yes”/“no” confirmation, handle it first:
        if (awaitingConfirmation) {
            when {
                lower.contains("yes") -> {
                    Log.d(
                        "VCC",
                        "👂 User input was confirmed now sending to CatchEntryTournament.kt "
                    )

                    lastConfirmedCatch?.let { (weightOz, species) ->

                        returnTournamentResult(weightOz, species)
                    }

                }

                lower.contains("no") -> {
                    awaitingConfirmation = false
                    tts.speak(
                        "Okay, let's start over. Please say the weight and the species then say over when you are done.",
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


        // 4) Cancel keywords
        val cancelPhrases = listOf("cancel", "that is wrong", "start over", "not right")
        if (cancelPhrases.any { cleaned.contains(it) }) {

            tts.speak(
                "Okay, let's try again. Please say the weight and say over when done.",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 2000)
            return
        }

        // 5) Parse weight (Kgs + Grams)
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15
        )
        var kilograms = -1
        var grams = -1
        val words = cleaned.split("\\s+".toRegex())
        for ((i, word) in words.withIndex()) {
            val num = numberWords[word] ?: word.toIntOrNull()
            if (num != null) {
                when {
                    i + 1 < words.size && words[i + 1].contains("kilogram") -> kilograms = num
                    i + 1 < words.size && words[i + 1].contains("grams") -> grams = num
                    kilograms < 0 -> kilograms = num
                    grams < 0 -> grams = num
                }
            }
        }
        if (kilograms < 0) kilograms = 0
        if (grams < 0) grams = 0
        val weightTotalKgsFD= kilograms * 100 + grams
        if (weightTotalKgsFD == 0) {
            Toast.makeText(this, "🚫 Weight cannot be 0.00 Kgs!", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // 6) Parse species
        val speciesCode = when {
            cleaned.contains("smallmouth") -> "Small Mouth"
            cleaned.contains("largemouth") -> "Large Mouth"
            else -> null
        }
        selectedSpecies = speciesCode ?: run {
            tts.speak(
                "What species was the $kilograms point $grams kilograms catch?",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_ASK_SPECIES"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }

        // 7)  Setup Species Spinner
        val onlyThatList: List<String> = listOf(selectedSpecies)
        val speciesAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, onlyThatList).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerSpecies.adapter = speciesAdapter

        // 8) Update your UI spinners
        edtWeightTensKgs.setSelection(kilograms/ 10)
        edtWeightKgs.setSelection(kilograms % 10)
        edtWeightKgsTenths.setSelection(grams / 10)
        edtWeightKgsHundreds.setSelection(grams % 10)
        spinnerSpecies.setSelection(
            (spinnerSpecies.adapter as ArrayAdapter<String>)
                .getPosition(selectedSpecies)
        )

        // 8) Ask for confirmation, echoing back exactly what we think we heard
        // after parsing pounds & ounces:

        val question =
            "You said a $kilograms point $grams kilograms for a $selectedSpecies, is that correct Over"

        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

        // 9) Save state and flip the flag
        lastConfirmedCatch = ConfirmedCatch(weightTotalKgsFD, selectedSpecies)
        awaitingConfirmation = true

        // 10) Immediately start listening for that “yes” or “no”
        Handler(mainLooper).postDelayed({ startListening() }, 2500)

    } //===================END handle Voice Input  ====================

    // ^^^^^^^^^^ Sending Data to CatchEntryTournament ^^^^^^^^^^^^^^^^
    private fun Activity.returnTournamentResult(weightTotalKgsFD: Int, species: String) {
        Intent(this, CatchEntryKgs::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WEIGHT_KGS, weightTotalKgsFD)
            putExtra(EXTRA_SPECIES, species)
        }.also {
            startActivity(it)
            finish()
        }
    }


}//================== END  ==========================
