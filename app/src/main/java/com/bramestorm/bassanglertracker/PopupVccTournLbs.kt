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

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightTensLbs: Spinner
    private lateinit var edtWeightLbs: Spinner
    private lateinit var edtWeightOz: Spinner
    private lateinit var btnSaveWeight: Button
    private lateinit var btnCancel: Button

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
        speak("Please say the weight, species, and the clip color you used for your catch.")

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""
        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")

        Log.d("PopupWeightEntry", "isTournament: $isTournament | catchType: $catchType | selectedSpecies: $selectedSpecies")
        Log.d("PopupWeightEntry", "Available clip colors: ${colorNames.joinToString()}")

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesVCCLbs)
        spinnerClipColor = findViewById(R.id.spinnerClipColorVCCLbs)
        edtWeightTensLbs = findViewById(R.id.spinnerLbsTens)
        edtWeightLbs = findViewById(R.id.spinnerLbsOnes)
        edtWeightOz = findViewById(R.id.spinnerOunces)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)
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


        // `````````` SAVE btn If User wants to enter Manually ````````````````
        btnSaveWeight.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED" //todo why are we having two places for clip_color variables...line 100
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val weightTensLbs = edtWeightTensLbs.selectedItem.toString().toIntOrNull() ?: 0
            val weightLbs     = edtWeightLbs.selectedItem.toString().toIntOrNull() ?: 0
            val weightOz      = edtWeightOz.selectedItem.toString().toIntOrNull() ?: 0

            val totalWeightOz = ((((weightTensLbs * 10) + weightLbs) * 16) + weightOz)

            if (totalWeightOz == 0) {
                positionedToast("🚫 Weight cannot be 0 lbs 0 oz!",)
                return@setOnClickListener
            }

            Log.d("CLIPS", "✅ Sending Result - weightTotalOz: $totalWeightOz, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")

            val resultIntent = Intent().apply {
                putExtra("weightTotalOz", totalWeightOz)
                putExtra("selectedSpecies", selectedSpeciesValue)
                putExtra("clip_color", selectedClipColor)
                putExtra("catchType", catchType)
                putExtra("isTournament", isTournament)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
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
            override fun onError(error: Int) {
                Log.e("VCC", "Speech recognition error: $error")
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }//------------------- End of startListening --------------------------

    //============= 👂Gets Users Info 📖 and Puts Everything for  Database and Listing =======================

    private fun handleVoiceInput(input: String) {
        val lowerInput = input.lowercase(Locale.getDefault())
        Log.d("VCC", "🎤 Raw speech: $lowerInput")

        if (!lowerInput.contains("over")) {     // todo why are we sending toast for VCC interaction?
            Toast.makeText(this, "Say the key word 'over' to finish your catch entry.", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanedInput = lowerInput
            .replace("over and out", "")
            .replace("over", "")
            .trim()

        Log.d("VCC", "🧹 Cleaned input: $cleanedInput")

        // === Cancel phrases ===
        val cancelPhrases = listOf("cancel", "that is wrong", "no", "wrong", "start over", "not right")
        if (cancelPhrases.any { cleanedInput.contains(it) }) {
            val cancelMessage = "Okay, let's try again. Please say the weight and say over when done."
            Log.d("VCC", "❌ Cancel voice command detected. Restarting...")
            tts.speak(cancelMessage, TextToSpeech.QUEUE_FLUSH, null, "TTS_RETRY")

            Handler(mainLooper).postDelayed({
                startListening()
            }, 2000)

            return
        }

        // === Weight parsing ===
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15
        )

        var pounds = -1
        var ounces = -1
        val words = cleanedInput.split(" ")

        for (i in words.indices) {
            val word = words[i]
            val number = numberWords[word] ?: word.toIntOrNull()
            if (number != null) {
                if (i + 1 < words.size && words[i + 1].contains("pound")) {  //todo what about pounds  the "s"
                    pounds = number
                } else if (i + 1 < words.size && words[i + 1].contains("ounce")) {  //todo what about ounces  the "s"
                    ounces = number
                } else if (pounds == -1) {
                    pounds = number
                } else if (ounces == -1) {
                    ounces = number
                }
            }
        }

        if (pounds == -1) pounds = 0
        if (ounces == -1) ounces = 0

        val tens = pounds / 10
        val ones = pounds % 10
        val totalWeightOz = pounds * 16 + ounces

        if (totalWeightOz == 0) {       // todo why a toast for VCC activity
            Toast.makeText(this, "🚫 Weight cannot be 0 lbs 0 oz!", Toast.LENGTH_SHORT).show()
            return
        }

        // === Parse species from input if present ===
        val speciesCode = when {
            cleanedInput.contains("smallmouth") -> "Small Mouth"
            cleanedInput.contains("largemouth") -> "Large Mouth"
            else -> null
        }

        val spinnerSpeciesText = spinnerSpecies.selectedItem?.toString() ?: ""
        val selectedSpecies = speciesCode ?: spinnerSpeciesText

        if (speciesCode == null) {
            tts.speak("What species was the $pounds pound $ounces ounce catch?", TextToSpeech.QUEUE_FLUSH, null, "TTS_ASK_SPECIES")
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }

        // === Parse clip color from input if present ===
        val clipColors = listOf("red", "blue", "green", "yellow", "orange", "white")
        val clipColorSpoken = clipColors.firstOrNull { cleanedInput.contains(it) }
        val selectedClipColor = clipColorSpoken?.uppercase() ?: spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"

        if (clipColorSpoken == null) {
            tts.speak("What clip color did you put the fish on?", TextToSpeech.QUEUE_FLUSH, null, "TTS_ASK_CLIP")
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }

        // Update UI spinners
        edtWeightTensLbs.setSelection(tens)
        edtWeightLbs.setSelection(ones)
        edtWeightOz.setSelection(ounces)

        val spokenConfirm = "You just caught a $selectedSpecies that weighs $pounds pounds and $ounces ounces and you put it on the $selectedClipColor. Catch saved."

        Log.d("VCC", "✅ Catch confirmed: $pounds lbs $ounces oz | $selectedSpecies | Clip=$selectedClipColor")
        tts.speak(spokenConfirm, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")
        tts.speak(spokenConfirm, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRMATION")

        // 🔁 Save context for re-use on "yes"
        lastConfirmedCatch = ConfirmedCatch(totalWeightOz, selectedSpecies, selectedClipColor)
        awaitingConfirmation = true

        val resultIntent = Intent().apply {
            putExtra("weightTotalOz", totalWeightOz)
            putExtra("selectedSpecies", selectedSpecies)
            putExtra("clip_color", selectedClipColor)
            putExtra("catchType", catchType)
            putExtra("isTournament", isTournament)
        }

        Handler(mainLooper).postDelayed({
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }, 1800)

    }//=========== END handleVoiceInput ==================================

private var awaitingConfirmation = false

data class ConfirmedCatch(val weightOz: Int, val species: String, val clipColor: String)
private var lastConfirmedCatch: ConfirmedCatch? = null      //todo Need to finish off the Confirmation with Yes/No ???


}//================== END  ==========================
