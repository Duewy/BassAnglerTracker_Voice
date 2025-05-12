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


class PopupVccTournCms: Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""
    private var speechRecognizer: SpeechRecognizer? = null
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null
    private lateinit var tts: TextToSpeech

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtLengthCmsTens: Spinner
    private lateinit var edtLengthCmsOnes: Spinner
    private lateinit var edtLengthCmsTenths: Spinner
    private lateinit var btnCancel: Button


    companion object {
        const val EXTRA_LENGTH_CMS     = "lengthTotalCms"
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
        setContentView(R.layout.popup_vcc_tourn_cms)

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
        speak("Please say the Length Species and Clip Color used for the Catch Over")


        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesVCCCms)
        spinnerClipColor = findViewById(R.id.spinnerClipColorVCCCms)
        edtLengthCmsTens = findViewById(R.id.spinnerCmsTens)
        edtLengthCmsOnes = findViewById(R.id.spinnerCmsOnes)
        edtLengthCmsTenths = findViewById(R.id.spinnerCmsTenths)
        btnCancel = findViewById(R.id.btnCancel)


        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra(EXTRA_IS_TOURNAMENT, false)
        catchType = intent.getStringExtra(EXTRA_CATCH_TYPE) ?: ""

        // ************  Setup Species Spinner *********************        // if Small Mouth is selected then Small Mouth is at top of Spinner
        val tournamentSpecies = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES)?.trim() ?: "Unknown"
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
        val availableColorNames = intent.getStringArrayExtra(EXTRA_AVAILABLE_CLIP_COLORS) ?:  arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter


        // ++++++++++++ Setup Weight Spinners ++++++++++++++++++++++++++
        // Tens place: 0–9 (represents 0–90 Centimeters)
        val tensOptions = (0..9).map { it.toString() }
        val tensAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tensOptions)
        tensAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtLengthCmsTens.adapter = tensAdapter

        // Ones place: 0–9 (represents 0–9 Centimeters)
        val onesOptions = (0..9).map { it.toString() }
        val onesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, onesOptions)
        onesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtLengthCmsOnes.adapter = onesAdapter

        // tenths: 0–9 millimeters
        val tenthsOptions = (0..9).map { it.toString() }
        val tenthsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tenthsOptions)
        tenthsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtLengthCmsTenths.adapter = tenthsAdapter

        // Optionally set default selection to 0
        edtLengthCmsTens.setSelection(0)
        edtLengthCmsOnes.setSelection(0)
        edtLengthCmsTenths.setSelection(0)


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

    data class ConfirmedCatch(val totalLengthTenths: Int, val species: String, val clipColor: String)

    //===============================================
    private fun handleVoiceInput(input: String) {
        val lower = input.lowercase(Locale.getDefault()).trim()
        Log.d("VCC", "🎤 Raw speech: $lower")

        // 1) If we’re waiting for a “yes”/“no” confirmation, handle it first:
        if (awaitingConfirmation) {
            when {
                lower.contains("yes") -> {
                    Log.d("VCC", "👂 User input was confirmed now sending to CatchEntryTournamentCms.kt ")

                    lastConfirmedCatch?.let { (totalLengthTenths, species, clipColor) ->

                        returnTournamentResult(totalLengthTenths, species, clipColor)
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
        var centimeters = -1
        var tenths= -1
        val words = cleaned.split("\\s+".toRegex())
        for ((i, word) in words.withIndex()) {
            val num = numberWords[word] ?: word.toIntOrNull()
            if (num != null) {
                when {
                    i + 1 < words.size && words[i + 1].contains("centimeter")  -> centimeters= num
                    i + 1 < words.size && words[i + 1].contains("point")  -> tenths = num
                    centimeters < 0                                            -> centimeters = num
                    tenths < 0                                            -> tenths = num
                }
            }
        }
        if (centimeters < 0) centimeters = 0
        if (tenths < 0) tenths = 0
        val totalLengthTenths= ((centimeters * 10) + tenths)
        if (totalLengthTenths== 0) {
            Toast.makeText(this, "🚫 Length cannot be 0 Cms 0 mm!", Toast.LENGTH_SHORT).show()
            return
        }
        // ENSURE millimeters is 0 to 9
        if (tenths > 9) {
            tts.speak(
                "Sorry, millimeters can only be zero to nine. Please repeat your length.",
                TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY"
            )
            Handler(mainLooper).postDelayed({ startListening() }, 1500)
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
                "What species was the $centimeters point $tenths centimeters catch?",
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
        edtLengthCmsTens.setSelection(centimeters/ 10)
        edtLengthCmsOnes.setSelection(centimeters % 10)
        edtLengthCmsTenths.setSelection(tenths)
        spinnerSpecies.setSelection((spinnerSpecies.adapter as ArrayAdapter<String>)
            .getPosition(selectedSpecies))
        spinnerClipColor.setSelection((spinnerClipColor.adapter as ArrayAdapter<String>)
            .getPosition(selectedClip))

        val question = "You said $centimeters point $tenths centimeters $selectedSpecies on the $selectedClip clip, is that correct Over"

        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

        // 10) Save state and flip the flag
        lastConfirmedCatch = ConfirmedCatch(totalLengthTenths, selectedSpecies, selectedClip)
        awaitingConfirmation = true


        // 11) Immediately start listening for that “yes” or “no”
        Handler(mainLooper).postDelayed({ startListening() }, 2500)

    } //===================END handle Voice Input  ====================

    // ^^^^^^^^^^ Sending Data to CatchEntryTournament ^^^^^^^^^^^^^^^^
    private fun Activity.returnTournamentResult(
        totalLengthTenths: Int, species: String, clipColor: String
    ) {
        Intent(this, CatchEntryTournamentCentimeters::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_LENGTH_CMS,    totalLengthTenths)
            putExtra(EXTRA_SPECIES,       species)
            putExtra(EXTRA_CLIP_COLOR,    clipColor)
        }.also {
            startActivity(it)
            finish()
        }
    }

}//================== END  ==========================
