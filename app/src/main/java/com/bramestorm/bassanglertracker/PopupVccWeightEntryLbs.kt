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
import com.bramestorm.bassanglertracker.util.positionedToast
import java.util.Locale


class PopupVccWeightEntryLbs: Activity() {

    // Flags and extras
    private var catchType: String = ""
    private var selectedSpecies: String = ""
    private var speechRecognizer: SpeechRecognizer? = null
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null
    private lateinit var tts: TextToSpeech

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var edtWeightTensLbs: Spinner
    private lateinit var edtWeightLbs: Spinner
    private lateinit var edtWeightOz: Spinner
    private lateinit var btnCancel: Button

    companion object {
        const val EXTRA_WEIGHT_OZ = "weightTotalOz"
        const val EXTRA_SPECIES = "selectedSpecies"
    }


    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_vcc_funday_lbs)

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
        spinnerSpecies = findViewById(R.id.spinnerSpeciesVCCFDLbs)
        edtWeightTensLbs = findViewById(R.id.spinnerFDLbsTens)
        edtWeightLbs = findViewById(R.id.spinnerFDLbsOnes)
        edtWeightOz = findViewById(R.id.spinnerFDOunces)
        btnCancel = findViewById(R.id.btnCancel)


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


            data class ConfirmedCatch(val weightOz: Int, val species: String)

            //===============================================
            private fun handleVoiceInput(input: String) {
                val lower = input.lowercase(Locale.getDefault()).trim()

                // 1) If we’re waiting for a “yes”/“no” confirmation, handle it first:
                if (awaitingConfirmation) {
                    when {
                        lower.contains("yes") -> {
                            lastConfirmedCatch?.let { (weightOz, species) ->
                                returnFunDayResult(weightOz, species)
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

                // 4) Parse weight (lbs + oz)
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
                if (totalOz == 0) {
                    Toast.makeText(this, "🚫 Weight cannot be 0 lbs 0 oz!", Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                // 5) ENSURE Ounces is 0 to 15 only
                if (ounces > 15) {
                    tts.speak(
                        "Sorry, ounces can only be zero to fifteen. Please repeat your length.",
                        TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY"
                    )
                    Handler(mainLooper).postDelayed({ startListening() }, 1500)
                    return
                }


                // 6) Parse and normalize species via the user-defined mapper
                val rawSpeciesPhrase = cleaned    // e.g. "lark mouth" or "perch"F
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

                // 7)  Setup Species Spinner

                val onlyThatList: List<String> = listOf(selectedSpecies)
                val speciesAdapter = ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item, onlyThatList).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                spinnerSpecies.adapter = speciesAdapter

                // 8) Update your UI spinners immediately
                edtWeightTensLbs.setSelection(pounds / 10)
                edtWeightLbs.setSelection(pounds % 10)
                edtWeightOz.setSelection(ounces)
                spinnerSpecies.setSelection(
                    (spinnerSpecies.adapter as ArrayAdapter<String>)
                        .getPosition(selectedSpecies)
                )

                // 8) Ask for confirmation, echoing back exactly what we think we heard
                // after parsing pounds & ounces:

                val question =
                    "You said a $pounds-lb $ounces-oz $selectedSpecies, is that correct Over"

                tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

                // 10) Save state and flip the flag
                lastConfirmedCatch = ConfirmedCatch(totalOz, selectedSpecies)
                awaitingConfirmation = true

                // 9) Immediately start listening for that “yes” or “no”
                Handler(mainLooper).postDelayed({ startListening() }, 2500)

            } //===================END handle Voice Input  ====================

            // ^^^^^^^^^^ Sending Data to CatchEntryLbsOzs ^^^^^^^^^^^^^^^^
            private fun returnFunDayResult(weightOz: Int, species: String) {
                val data = Intent().apply {
                    putExtra(EXTRA_WEIGHT_OZ, weightOz)
                    putExtra(EXTRA_SPECIES,  species)
                }
                setResult(Activity.RESULT_OK, data)
                finish()
            }

    }//================== END  ==========================
