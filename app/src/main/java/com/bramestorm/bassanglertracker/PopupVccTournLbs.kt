package com.bramestorm.bassanglertracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.training.VoiceErrorHandler
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager.validateClipColorFromVoice
import com.bramestorm.bassanglertracker.utils.positionedToast
import java.util.Locale


class PopupVccTournLbs: Activity() {

    // Flags and extras

    private var selectedSpecies: String = ""
    private lateinit var availableClipColors: Array<String>
    private var clipIndex = 0
    private var speechRecognizer: SpeechRecognizer? = null
    private var awaitingConfirmation = false
    private var lastConfirmedCatch: ConfirmedCatch? = null
    private lateinit var tts: TextToSpeech
    private var silenceRunnable: Runnable? = null
    private val vccHandler = Handler(Looper.getMainLooper())

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
        // ← outputs from this popup
        const val EXTRA_WEIGHT_OZ              = "weightTotalOz"        // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"      // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"           // Send this

        // → inputs into this popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
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


        selectedSpecies = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES) ?: ""
        availableClipColors = intent.getStringArrayExtra(EXTRA_AVAILABLE_CLIP_COLORS) ?:  arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")

        Log.d("Available", "colors = ${availableClipColors.joinToString()}")

        // UI Components
        tvSpecies = findViewById(R.id.tvSpeciesVCCLbs)
        tvClipColor = findViewById(R.id.tvClipColorVCCLbs)
        edtWeightTensLbs = findViewById(R.id.tvLbsTens)
        edtWeightLbs = findViewById(R.id.tvLbsOnes)
        edtWeightOz = findViewById(R.id.tvOunces)
        btnCancel = findViewById(R.id.btnCancel)

        val firstClip = availableClipColors.first()
        setClipColor(firstClip)


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

    private fun speak(message: String, utteranceId: String = "TTS_DONE") {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "TTS_DONE" || utteranceId == "TTS_CONFIRM") {
                    vccHandler.post { startListening() }
                }
            }
            override fun onError(utteranceId: String?) {}
        })
    }

    private fun startListening() {

        // 1) Create / reset the recognizer
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // nothing special here
                }
                override fun onBeginningOfSpeech() { /* user started talking */ }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { /* Android will fire onResults or onError next */ }

                override fun onError(error: Int) {
                   Log.e("VCC", "Recognition error: $error")

                   VoiceErrorHandler.handleError(
                       activity   = this@PopupVccTournLbs,
                       errorCode = error,
                        retryCount = retryCount,
                       onRetry = { speak("Sorry, I did not get that. Please try again. Over.") },
                        onFallback = {
                          speak("I’m having trouble—please enter your catch manually. Over.")
                         finish()
                        }
                   )
                }

                override fun onResults(results: Bundle?) {
                        // reset *this* popup’s retry counter
                    retryCount = 0

                   val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                   val spokenText = matches?.firstOrNull() ?: ""
                    Log.d("VCC", "Got: $spokenText")
                   handleVoiceInput(spokenText)
                }

                override fun onPartialResults(partial: Bundle?) { /* optional */ }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        // 2) Build the intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            // Let Android wait up to 4s of silence at end-of-speech before firing onResults()
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                4000L)
            // Require at least 1s of speech before thinking we're done
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1000L)

            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // 3) Kick it off
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

        // === 6) Parse 🐟 SPECIES 🦈 ===

        val matchedSpecies = VoiceInputMapper.baseSpeciesVoiceMap
            .entries
            .firstOrNull { (key, _) -> cleaned.contains(key) }
            ?.value

        val finalSpecies = matchedSpecies ?: selectedSpecies

        Log.d("VCC", "Parsed speciesCode = $finalSpecies (Matched: $matchedSpecies, Tournament = $selectedSpecies)")


        // === 7) Parse clip color ===
        val selectedClip: String? = validateClipColorFromVoice(cleaned, availableClipColors)
        if (selectedClip == null) {
            val colorList = availableClipColors.joinToString(", ").lowercase()
            speak("That clip color is not available. Please choose one of the following: $colorList. Then say over.")
            Log.w("VCC", "Invalid clip color spoken. Available colors: ${availableClipColors.joinToString()}")
            Handler(mainLooper).postDelayed({ startListening() }, 2500)
            return
        }
        Log.w("VCC", " Available colors: ${availableClipColors.joinToString()}")

        // === 8) 🚨🚨 Fail if any critical info missing  🚨🚨  ===

       // 8a) Check for too-many ounces
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

        // 8b) Now catch the other “nothing understood” cases
        if (totalOz == 0 || finalSpecies.isBlank()) {
            retryCount++
            if (retryCount >= MAX_RETRIES) {
                speak("I'm still having trouble. Please enter your catch manually. Over and Out")
                return
            }
            speak("I could not understand everything. Please say the full catch again, including weight, species, and clip color, then say OVER.")
            Handler(mainLooper).postDelayed({ startListening() }, 2000)
            return
        }

        // 8c) When we reach here, everything parsed is OK — reset retry counter
        retryCount = 0


        // === 9) Update UI for visual feedback ===

        edtWeightTensLbs.text= (pounds / 10).toString()
        edtWeightLbs.text    = (pounds % 10).toString()
        edtWeightOz.text     = ounces.toString()
        tvSpecies.text       = finalSpecies
        tvClipColor.text     = selectedClip

        setClipColor(selectedClip)      // puts selectedClip Color into the background


        // === 10) Confirm with user ===
        val question = "You said a $pounds pound $ounces ounce $finalSpecies on the $selectedClip clip. Is that correct? Over."
        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "TTS_CONFIRM")

        lastConfirmedCatch = ConfirmedCatch(totalOz, finalSpecies, selectedClip)
        awaitingConfirmation = true

        Handler(mainLooper).postDelayed({ startListening() }, 2500)

    }   //===================END handle Voice Input  ====================


   // ^^^^^^^^^^ SENDING DATA back to CatchEntryTournament ^^^^^^^^^^^^^^^^
   // PopupVccTournLbs.kt
   private fun Activity.returnTournamentResult(
       weightOz: Int, finalSpecies: String, clipColor: String
   ) {
       // send a local broadcast, not a new Activity start
       val broadcastIntent = Intent("com.bramestorm.CATCH_TOURNAMENT").apply {
           putExtra(EXTRA_WEIGHT_OZ, weightOz)
           putExtra(EXTRA_SPECIES, finalSpecies)
           putExtra(EXTRA_CLIP_COLOR, clipColor)
       }
       LocalBroadcastManager.getInstance(this)
           .sendBroadcast(broadcastIntent)

       finish()
   }//============ DATA SENT ===========================

        // sets Background of Clip Color up
    private fun setClipColor(name: String) {
        // 1) Update the text
        tvClipColor.text = name

        // 2) Figure out the enum (defaulting to WHITE on error)
        val clipEnum = try {
            CatchEntryTournament.ClipColor.valueOf(name.uppercase())
        } catch (e: IllegalArgumentException) {
            CatchEntryTournament.ClipColor.WHITE
        }

        // 3) Grab the actual color int
        val bgColor = ContextCompat.getColor(this, clipEnum.resId)

        // 4) Build & apply the background drawable
        val gd = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            setColor(bgColor)
            setStroke(2, Color.BLACK)
        }
        tvClipColor.background = gd
    }


}//================== END  ==========================
