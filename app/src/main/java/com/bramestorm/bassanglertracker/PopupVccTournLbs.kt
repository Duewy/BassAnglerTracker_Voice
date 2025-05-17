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
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.util.positionedToast
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager.validateClipColorFromVoice
import java.util.Locale


class PopupVccTournLbs: Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""
    private lateinit var availableClipColors: Array<String>
    private var speechRecognizer: SpeechRecognizer? = null
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null
    private lateinit var tts: TextToSpeech

    // 3333333333333 Error Handling Helpers  33333333333333
    private var retryCount = 0
    private val MAX_RETRIES = 3


    // UI Components
    private lateinit var tvSpecies: TextView
    private lateinit var tvClipColor: TextView
    private lateinit var edtWeightTensLbs: TextView
    private lateinit var edtWeightLbs: TextView
    private lateinit var edtWeightOz: TextView
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
        isTournament   = intent.getBooleanExtra(EXTRA_IS_TOURNAMENT, false)
        catchType      = intent.getStringExtra(EXTRA_CATCH_TYPE) ?: ""
        selectedSpecies = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES) ?: ""
        availableClipColors = intent.getStringArrayExtra(EXTRA_AVAILABLE_CLIP_COLORS) ?:  arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")


        // UI Components
        tvSpecies = findViewById(R.id.tvSpeciesVCCLbs)
        tvClipColor = findViewById(R.id.tvClipColorVCCLbs)
        edtWeightTensLbs = findViewById(R.id.tvLbsTens)
        edtWeightLbs = findViewById(R.id.tvLbsOnes)
        edtWeightOz = findViewById(R.id.tvOunces)
        btnCancel = findViewById(R.id.btnCancel)



            // ````````` CANCEL btn ```````````````````
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }//==============  END ON CREATE =================================

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    //-------- User Speaking -------------------------

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
            @Deprecated("Deprecated in Java")
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
            // Only need the best match
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

            override fun onError(error: Int) {
                Log.e("VCC", "Speech recognition error: $error")

                retryCount++
                if (retryCount >= MAX_RETRIES) {
                    tts.speak("There was a serious problem understanding you. For now please enter the information manually.Over", TextToSpeech.QUEUE_FLUSH, null, "TTS_FAIL")
                    return
                }

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        tts.speak(
                            "I did not catch that. Please say your catch information again after the beep. Over",
                            TextToSpeech.QUEUE_FLUSH, null, "TTS_REPEAT"
                        )
                        Handler(mainLooper).postDelayed({ startListening() }, 1200)
                    }
                    else -> {
                        tts.speak(
                            "Something went wrong with speech recognition. Please say your catch information again after the beep. Over",
                            TextToSpeech.QUEUE_FLUSH, null, "TTS_ERROR"
                        )
                        Handler(mainLooper).postDelayed({ startListening() }, 2000)
                    }
                }
            }
        })
        speechRecognizer?.startListening(intent)
    }//------------------- End of startListening --------------------------

    //============= 👂Get the User's Info 📖 and Puts Everything for Database and Listing =======================

    data class ConfirmedCatch(val weightOz: Int, val species: String, val clipColor: String)

    //==================Sort Out the User's Information ==========

    private fun handleVoiceInput(input: String) {
        val lower = input.lowercase(Locale.getDefault()).trim()

        // === 1) Confirmation Flow ===
        if (awaitingConfirmation) {
            when {
                lower.contains("yes") -> {
                    lastConfirmedCatch?.let {
                        returnTournamentResult(it.weightOz, it.species, it.clipColor)
                    }
                }
                lower.contains("no") -> {
                    awaitingConfirmation = false
                    speak("Okay, let's try again. Please say the weight, species, and clip color. Say OVER when you're finished. Over")
                }
                else -> {
                    speak("Please say 'yes' to confirm or 'no' to re enter your catch. Over")
                }
            }
            return
        }

        // === 2) Require "OVER" ===
        if (!lower.contains("over")) {
            positionedToast("⚠️ Say the word 'OVER' to finish your voice command. over")
            speak("Say OVER when you are finished with your voice command. over")
            return
        }

        // === 3) Clean the input ===
        val cleaned = lower.replace("over and out", "")
            .replace("over", "")
            .replace(Regex("[^a-z0-9\\s]"), "")  // Strip punctuation
            .trim()

        // === 4) Cancel phrases ===
        val cancelPhrases = listOf("cancel", "start over", "that is wrong", "not right", "try again")
        if (cancelPhrases.any { cleaned.contains(it) }) {
            speak("Okay, let's try again. Please say the full catch entry and say OVER.")
            Handler(mainLooper).postDelayed({ startListening() }, 2000)
            return
        }

        // === 5) Parse weight ===
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15
        )

        var pounds = -1
        var ounces = -1
        val words = cleaned.split("\\s+".toRegex())

        for ((i, word) in words.withIndex()) {
            val num = numberWords[word] ?: word.toIntOrNull()
            if (num != null) {
                when {
                    i + 1 < words.size && words[i + 1].contains("pound") -> pounds = num
                    i + 1 < words.size && words[i + 1].contains("ounce") -> ounces = num
                    pounds < 0 -> pounds = num
                    ounces < 0 -> ounces = num
                }
            }
        }

        if (pounds < 0) pounds = 0
        if (ounces < 0) ounces = 0
        val totalOz = pounds * 16 + ounces

        // === 6) Parse species ===
        val matchedSpecies = VoiceInputMapper.baseSpeciesVoiceMap
            .entries
            .firstOrNull { (key, _) -> cleaned.contains(key) }
            ?.value

        val speciesCode = when {        // for any BASS both Large and Small Mouth Bass are good to enter
            selectedSpecies.contains("bass", ignoreCase = true) &&
                    (matchedSpecies == "Small Mouth" || matchedSpecies == "Largemouth") -> matchedSpecies
            matchedSpecies == selectedSpecies -> matchedSpecies
            else -> selectedSpecies
        }

        Log.d("VCC", "Parsed speciesCode = $speciesCode (Matched: $matchedSpecies, Tournament = $selectedSpecies)")


        // === 7) Parse clip color ===
        val selectedClip: String? = validateClipColorFromVoice(cleaned, availableClipColors)
        if (selectedClip == null) {
            val colorList = availableClipColors.joinToString(", ").lowercase()
            speak("That clip color is not available. Please choose one of the following: $colorList. Then say over.")
            Log.w("VCC", "Invalid clip color spoken. Available colors: ${availableClipColors.joinToString()}")
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }


        // === 8) 🚨🚨 Fail if any critical info missing  🚨🚨  ===

       // 1) Check for too-many ounces
        if (ounces > 15) {
            retryCount++
            if (retryCount >= MAX_RETRIES) {
                speak("I'm still having trouble. Please enter your catch manually. Over and Out")
                return
            }
            speak("Ounces must be under 16. Please try again. Over.")
            Handler(mainLooper).postDelayed({ startListening() }, 2000)
            return
        }

        // 2) Now catch the other “nothing understood” cases
        if (totalOz == 0 || speciesCode == null || selectedClip == null) {
            retryCount++
            if (retryCount >= MAX_RETRIES) {
                speak("I'm still having trouble. Please enter your catch manually. Over and Out")
                return
            }
            speak("I could not understand everything. Please say the full catch again, including weight, species, and clip color, then say OVER.")
            Handler(mainLooper).postDelayed({ startListening() }, 2000)
            return
        }

        // 3) When we reach here, everything parsed is OK — reset retry counter
        retryCount = 0


        // === 9) Update UI for visual feedback ===

        edtWeightTensLbs.text  = (pounds / 10).toString()
        edtWeightLbs.text     = (pounds % 10).toString()
        edtWeightOz.text = ounces.toString()
        tvSpecies.text   = speciesCode
        tvClipColor.text = selectedClip


        // === 10) Confirm with user ===
        val question = "You said a $pounds pound $ounces ounce $speciesCode on the $selectedClip clip. Is that correct? Over."
        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

        lastConfirmedCatch = ConfirmedCatch(totalOz, speciesCode, selectedClip)
        awaitingConfirmation = true

        Handler(mainLooper).postDelayed({ startListening() }, 2500)

    }   //===================END handle Voice Input  ====================


   // ^^^^^^^^^^ SENDING DATA back to CatchEntryTournament ^^^^^^^^^^^^^^^^
    private fun Activity.returnTournamentResult(
        weightOz: Int, species: String, clipColor: String) {
       Intent(this, CatchEntryTournament::class.java).apply {
           flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
           putExtra(EXTRA_WEIGHT_OZ, weightOz)
           putExtra(EXTRA_SPECIES, species)
           putExtra(EXTRA_CLIP_COLOR, clipColor)
       }.also {
           startActivity(it)
           speak( "Catch Saved, Over and Out")
           finish()
         }
    }

}//================== END  ==========================
