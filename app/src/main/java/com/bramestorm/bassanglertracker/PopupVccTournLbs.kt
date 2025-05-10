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


class PopupVccTournLbs: Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null


    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightTensLbs: Spinner
    private lateinit var edtWeightLbs: Spinner
    private lateinit var edtWeightOz: Spinner
    private lateinit var btnCancel: Button


    companion object {
        const val EXTRA_WEIGHT_OZ     = "weightTotalOz"
        const val EXTRA_SPECIES       = "selectedSpecies"
        const val EXTRA_CLIP_COLOR    = "clip_color"
        const val EXTRA_CATCH_TYPE    = "catchType"
        const val EXTRA_IS_TOURNAMENT = "isTournament"
        const val EXTRA_AVAILABLE_CLIP_COLORS = "availableClipColors"
        const val EXTRA_TOURNAMENT_SPECIES = "tournamentSpecies"
    }


    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_vcc_tourn_lbs)

        Log.d("VCC", "🎯 setting up SpeechRecognizer listener")

        val receivedColors = intent.getStringArrayExtra(EXTRA_AVAILABLE_CLIP_COLORS)
        Log.d("POPUP-DEBUG", "onCreate: receivedColors=${receivedColors?.toList()}")

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
        speak("Please say the weight species and clip color used for the catch Over")

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES) ?: ""
        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")
        val incomingSpecies = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES) ?: ""


        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesVCCLbs)
        spinnerClipColor = findViewById(R.id.spinnerClipColorVCCLbs)
        edtWeightTensLbs = findViewById(R.id.spinnerLbsTens)
        edtWeightLbs = findViewById(R.id.spinnerLbsOnes)
        edtWeightOz = findViewById(R.id.spinnerOunces)
        btnCancel = findViewById(R.id.btnCancel)

        // ************  Setup Species Spinner *********************        // if Small Mouth is selected then Small Mouth is at top of Spinner
        val tournamentSpecies = intent.getStringExtra("tournamentSpecies")?.trim() ?: "Unknown"
        val speciesList: Array<String> = when {
            isTournament && tournamentSpecies.equals("Large Mouth Bass", ignoreCase = true) -> {
                arrayOf("Large Mouth", "Small Mouth")
            }
            isTournament && tournamentSpecies.equals("Small Mouth Bass", ignoreCase = true) -> {
                arrayOf("Small Mouth", "Large Mouth")
            }
            isTournament -> {
                arrayOf(tournamentSpecies)
            }
            else -> {
                arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
            }
        }
        val speciesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = speciesAdapter


        // ****************  Setup Clip Color Spinner ****************
        val availableColorNames = intent.getStringArrayExtra("availableClipColors") ?:  arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter


       // ++++++++++++ Setup Weight Spinners ++++++++++++++++++++++++++

            // Tens place: 0–9 (represents 0–90 lbs)
        val tensOptions = (0..9).map { it.toString() }
        val tensAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tensOptions)
        tensAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightTensLbs.adapter = tensAdapter

            // Ones place: 0–9 (represents 0–9 lbs)
        val onesOptions = (0..9).map { it.toString() }
        val onesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, onesOptions)
        onesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightLbs.adapter = onesAdapter

            // Ounces: 0–15
        val ounceOptions = (0..15).map { it.toString() }
        val ounceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ounceOptions)
        ounceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtWeightOz.adapter = ounceAdapter

            // Optionally set default selection to 0
        edtWeightTensLbs.setSelection(0)
        edtWeightLbs.setSelection(0)
        edtWeightOz.setSelection(0)


            // ````````` CANCEL btn ```````````````````
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//==============  END ON CREATE =================================

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
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

            // Give the user up to 2 seconds of silence to finish speaking
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000L
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
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
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

            override fun onError(error: Int) {
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
        speechRecognizer.startListening(intent)
    }//------------------- End of startListening --------------------------

    //============= 👂Gets Users Info 📖 and Puts Everything for  Database and Listing =======================


    data class ConfirmedCatch(val weightOz: Int, val species: String, val clipColor: String)

    //===============================================
    private fun handleVoiceInput(input: String) {
        val lower = input.lowercase(Locale.getDefault()).trim()
        Log.d("VCC", "🎤 Raw speech: $lower")

        // 1) If we’re waiting for a “yes”/“no” confirmation, handle it first:
        if (awaitingConfirmation) {
            when {
                lower.contains("yes") -> {
                    Log.d("VCC", "👂 User input was confirmed now sending to CatchEntryTournament.kt ")

                    lastConfirmedCatch?.let { (weightOz, species, clipColor) ->

                        returnTournamentResult(weightOz, species, clipColor)
                    }

                }

                lower.contains("no") -> {
                    awaitingConfirmation = false
                    tts.speak(
                        "Okay, let's start over. Please say the weight species and clip then say over when you are done.",
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

        // 5) Parse weight (lbs + oz)
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15
        )
        var pounds = -1
        var ounces = -1
        val words = cleaned.split("\\s+".toRegex())
        for ((i, word) in words.withIndex()) {
            val num = numberWords[word] ?: word.toIntOrNull()
            if (num != null) {
                when {
                    i + 1 < words.size && words[i + 1].contains("pound")  -> pounds = num
                    i + 1 < words.size && words[i + 1].contains("ounce")  -> ounces = num
                    pounds < 0                                            -> pounds = num
                    ounces < 0                                            -> ounces = num
                }
            }
        }
        if (pounds < 0) pounds = 0
        if (ounces < 0) ounces = 0
        val totalOz = pounds * 16 + ounces
        if (totalOz == 0) {
            Toast.makeText(this, "🚫 Weight cannot be 0 lbs 0 oz!", Toast.LENGTH_SHORT).show()
            return
        }

        // 6) Parse species
        val speciesCode = when {
            cleaned.contains("smallmouth")   -> "Small Mouth"
            cleaned.contains("largemouth")   -> "Large Mouth"
            else                             -> null
        }
        val selectedSpecies = speciesCode ?: run {
            tts.speak(
                "What species was the $pounds pound $ounces ounce catch?",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_ASK_SPECIES"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }

        // 7) Parse clip color
        val clipColors = listOf("red","blue","green","yellow","orange","white")
        val spokenColor = clipColors.firstOrNull { cleaned.contains(it) }
        val selectedClip = spokenColor?.uppercase()
            ?: spinnerClipColor.selectedItem?.toString()?.uppercase()
            ?: "white"
        if (spokenColor == null) {
            tts.speak(
                "What clip color did you put the fish on?",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_ASK_CLIP"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }

        // 8) Update your UI spinners immediately
        edtWeightTensLbs.setSelection(pounds / 10)
        edtWeightLbs.setSelection(pounds % 10)
        edtWeightOz.setSelection(ounces)
        spinnerSpecies.setSelection((spinnerSpecies.adapter as ArrayAdapter<String>)
            .getPosition(selectedSpecies))
        spinnerClipColor.setSelection((spinnerClipColor.adapter as ArrayAdapter<String>)
            .getPosition(selectedClip))

        // 9) Ask for confirmation, echoing back exactly what we think we heard
        val question = "You said a $pounds-lb $ounces-oz $selectedSpecies on the $selectedClip clip, is that correct Over"

        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

        // 10) Save state and flip the flag
        lastConfirmedCatch = ConfirmedCatch(totalOz, selectedSpecies, selectedClip)
        awaitingConfirmation = true


        // 11) Immediately start listening for that “yes” or “no”
        Handler(mainLooper).postDelayed({ startListening() }, 2500)

    } //===================END handle Voice Input  ====================

   // ^^^^^^^^^^ Sending Data to CatchEntryTournament ^^^^^^^^^^^^^^^^
    private fun Activity.returnTournamentResult(
        weightOz: Int, species: String, clipColor: String
    ) {
       Intent(this, CatchEntryTournament::class.java).apply {
           flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
           putExtra(EXTRA_WEIGHT_OZ,     weightOz)
           putExtra(EXTRA_SPECIES,       species)
           putExtra(EXTRA_CLIP_COLOR,    clipColor)
       }.also {
           startActivity(it)
           finish()
       }
   }

}//================== END  ==========================
