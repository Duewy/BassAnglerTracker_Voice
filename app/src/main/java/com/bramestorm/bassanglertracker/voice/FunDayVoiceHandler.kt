package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.getComparisonValueByMode
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FunDayVoiceHandler(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,
    private val dbHelper: CatchDatabaseHelper = CatchDatabaseHelper(context)
) : VoiceSessionHandler {

    companion object {
        private const val TAG = "FunDayVoiceHandler"
        private const val ACTION_CATCH_SAVED = "com.bramestorm.VOICE_CATCH_SAVED"
    }

    private val measurementMode = SharedPreferencesManager.getFunDayUnit(context)
    private val typeEntry = measurementMode.name.lowercase(Locale.getDefault())

    private val voiceManager = VoiceInteractionManager(
        context = context,
        uiHelper = uiHelper,
        parser = VoiceParser
    )

    private val maxParseRetries = 3
    private var parseRetryCount = 0

    private var inQuestionMode = false
    private val maxQuestionRetries = 3
    private var questionRetryCount = 0

    override fun onWake() {
        Log.d(TAG, "onWake() called")
        startSession()
    }

    private fun startSession() {
        if (inQuestionMode) {
            Log.d(TAG, "In question mode, skipping catch flow")
            return
        }
        val prompt = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "Please say the pounds, ounces, and species of your catch. Over."
            MeasurementMode.KG -> "Please say the kilograms, grams, and species of your catch. Over."
            MeasurementMode.INCHES -> "Please say the inches, quarters, and species of your catch. Over."
            MeasurementMode.CM -> "Please say the centimeters and species of your catch. Over."
        }

        voiceManager.startSession(
            prompt = prompt,
            onResult = { transcript ->
                Log.d(TAG, "Transcript: '$transcript'")
                val clean = transcript.trim().lowercase(Locale.getDefault())
                if (clean.contains("question")) {
                    handleQuestionMode()
                } else {
                    onCatchConfirmed(transcript)
                }
            },
            onFailure = {
                (context as? VoiceControlService)?.markSessionComplete()
            }
        )
    }

    private fun onCatchConfirmed(transcript: String) {
        Log.d(TAG, "onCatchConfirmed('$transcript')")
        val parsed = when (measurementMode) {
            MeasurementMode.LBS_OZ -> VoiceParser.parseImperialCatchSimple(transcript)
            MeasurementMode.KG -> VoiceParser.parseMetricCatchSimple(transcript)
            MeasurementMode.INCHES -> VoiceParser.parseImperialLengthSimple(transcript)
            MeasurementMode.CM -> VoiceParser.parseMetricLengthSimple(transcript)
        }

        val missingSpecies = parsed.species.isBlank()
        val missingValue = when (measurementMode) {
            MeasurementMode.LBS_OZ -> parsed.totalWeightOzs == 0
            MeasurementMode.KG -> parsed.totalWeightHundredthKg == 0
            MeasurementMode.INCHES -> parsed.totalLengthQuarters == 0
            MeasurementMode.CM -> parsed.totalLengthTenths == 0
        }

        val oz = parsed.weightOz
        val grams = parsed.weightGrams
        val quarters = parsed.lengthQuarters
        val tenths = parsed.lengthTenths

        if ((measurementMode == MeasurementMode.LBS_OZ && oz > 15) ||
            (measurementMode == MeasurementMode.KG && grams > 99) ||
            (measurementMode == MeasurementMode.INCHES && quarters > 3) ||
            (measurementMode == MeasurementMode.CM && tenths > 9)) {

            Log.w(TAG, "❌ Invalid unit detected → oz=$oz, grams=$grams, quarters=$quarters, tenths=$tenths")
            uiHelper.speak("You said an inaccurate value. Let's try that again.", "TTS_INVALID_UNIT")
            Handler(Looper.getMainLooper()).postDelayed({ startSession() }, 1500)
            return
        }

        if (missingSpecies || missingValue) {
            parseRetryCount++
            if (parseRetryCount > maxParseRetries) {
                uiHelper.speak("Sorry, I still can’t understand—let’s try again later. Over.", "TTS_FAIL")
                (context as? VoiceControlService)?.markSessionComplete()
                parseRetryCount = 0
                return
            }
            uiHelper.speak("I missed some of that—please say pounds, ounces, and species again. Over.", "TTS_RETRY")
            Handler(Looper.getMainLooper()).postDelayed({ startSession() }, 1500)
            return
        }
        parseRetryCount = 0

        val confirmPrompt = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "To confirm, your ${parsed.species} is ${parsed.weightLbs} pounds and ${parsed.weightOz} ounces. Is that correct? Over."
            MeasurementMode.KG -> "To confirm, your ${parsed.species} is ${parsed.weightKgWhole} point ${parsed.weightGrams} kilograms. Is that correct? Over."
            MeasurementMode.INCHES -> "To confirm, your ${parsed.species} is ${parsed.lengthInches} inches and ${parsed.lengthQuarters} quarters. Is that correct? Over."
            MeasurementMode.CM -> "To confirm, your ${parsed.species} is ${parsed.lengthCm} point ${parsed.lengthTenths} centimeters. Is that correct? Over."
        }
        uiHelper.speak(confirmPrompt, "TTS_CONFIRM")

        Handler(Looper.getMainLooper()).postDelayed({
            voiceManager.startSession(
                prompt = "Please say yes over, no over, or cancel that. Over.",
                onResult = { response ->
                    when {
                        response.contains("yes over", ignoreCase = true) -> saveCatch(parsed)
                        response.contains("no over", ignoreCase = true) -> startSession()
                        response.contains("cancel", ignoreCase = true) -> {
                            parseRetryCount = 0
                            uiHelper.speak("Okay, canceling. Over and Out.")
                            (context as? VoiceControlService)?.markSessionComplete()
                        }
                        else -> startSession()
                    }
                },
                onFailure = {
                    (context as? VoiceControlService)?.markSessionComplete()
                }
            )
        }, 3500)
    }

    override fun shutdown() {
        Log.d(TAG, "🔻 shutdown called")
    }

    private fun saveCatch(parsed: VoiceParser.ParsedCatch) {
        val catchItem = CatchItem(
            id = 0,
            dateTime = currentTimestamp(),
            longitude = null,
            latitude = null,
            species = parsed.species,
            totalWeightOz = parsed.totalWeightOzs.takeIf { measurementMode == MeasurementMode.LBS_OZ },
            totalWeightHundredthKg = parsed.totalWeightHundredthKg.takeIf { measurementMode == MeasurementMode.KG },
            totalLengthQuarters = parsed.totalLengthQuarters.takeIf { measurementMode == MeasurementMode.INCHES },
            totalLengthTenths = parsed.totalLengthTenths.takeIf { measurementMode == MeasurementMode.CM },
            catchType = typeEntry,
            markerType = null,
            clipColor = null
        )
        dbHelper.insertCatch(catchItem)

        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_CATCH_SAVED))

        uiHelper.speak("Catch is saved. Over and Out.", "TTS_SAVED")
        (context as? VoiceControlService)?.markSessionComplete()
    }

    private fun handleQuestionMode() {
        inQuestionMode = true
        questionRetryCount = 0
        uiHelper.speak("Question mode activated. Ask largest, smallest, total weight or total length. Over and out.", "TTS_QUESTION_INTRO")

        Handler(Looper.getMainLooper()).postDelayed({
            voiceManager.startSession(
                prompt = "Which stat would you like? Over.",
                onResult = { question -> routeQuestion(question) },
                onFailure = {
                    (context as? VoiceControlService)?.markSessionComplete()
                }
            )
        }, 1500)
    }

    private fun routeQuestion(question: String) {
        val overOut = "Over and Out."

        if (question.contains("cancel", ignoreCase = true)) {
            uiHelper.speak("Okay, exiting question mode. $overOut", "TTS_CANCEL")
            inQuestionMode = false
            questionRetryCount = 0
            (context as? VoiceControlService)?.markSessionComplete()
            return
        }

        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allCatches = dbHelper.getCatchesForToday(typeEntry, todaysDate)
        val speciesMentioned = allCatches.map { it.species.lowercase() }
            .distinct()
            .firstOrNull { question.lowercase().contains(it) }

        val filtered = if (speciesMentioned != null) {
            allCatches.filter { it.species.equals(speciesMentioned, ignoreCase = true) }
        } else allCatches

        if (filtered.isEmpty()) {
            uiHelper.speak("No ${speciesMentioned ?: ""} catches today. $overOut", "TTS_ERROR")
            return
        }

        when {
            question.contains("largest", true) -> {
                val fish = filtered.maxByOrNull { it.getComparisonValueByMode(measurementMode) }!!
                val prefix = speciesMentioned?.let { "Your largest ${it.uppercase()} today is" } ?: "Your largest catch today is"
                speakFish(fish, prefix, overOut)
            }

            question.contains("smallest", true) -> {
                val fish = filtered.minByOrNull { it.getComparisonValueByMode(measurementMode) }!!
                val prefix = speciesMentioned?.let { "Your smallest ${it.uppercase()} today is" } ?: "Your smallest catch today is"
                speakFish(fish, prefix, overOut)
            }

            question.contains("total weight", true) -> {
                val total = allCatches.sumOf { it.getComparisonValueByMode(measurementMode).toDouble() }
                uiHelper.speak("Your total weight today is $total. $overOut", "TTS_ANSWER")
            }

            question.contains("total length", true) -> {
                val total = allCatches.sumOf { it.getComparisonValueByMode(measurementMode).toDouble() }
                uiHelper.speak("Your total length today is $total. $overOut", "TTS_ANSWER")
            }

            else -> {
                questionRetryCount++
                if (questionRetryCount > maxQuestionRetries) {
                    uiHelper.speak("Exiting question mode. $overOut", "TTS_FAIL")
                    inQuestionMode = false
                    questionRetryCount = 0
                } else {
                    uiHelper.speak("Sorry, I didn't catch that. Say largest, smallest, total weight or total length. $overOut", "TTS_RETRY_QUESTION")
                    Handler(Looper.getMainLooper()).postDelayed({ handleQuestionMode() }, 1500)
                }
            }
        }
    }

    private fun speakFish(fish: CatchItem, prefix: String, overOut: String) {
        when (measurementMode) {
            MeasurementMode.LBS_OZ -> {
                val oz = fish.totalWeightOz ?: 0
                val lbs = oz / 16
                val remOz = oz % 16
                uiHelper.speak("$prefix ${fish.species} at $lbs pounds and $remOz ounces. $overOut", "TTS_ANSWER")
            }
            MeasurementMode.KG -> {
                val hundredths = fish.totalWeightHundredthKg ?: 0
                val kgs = hundredths / 100
                val grams = hundredths % 100
                uiHelper.speak("$prefix ${fish.species} at $kgs point $grams kilograms. $overOut", "TTS_ANSWER")
            }
            MeasurementMode.INCHES -> {
                val quarters = fish.totalLengthQuarters ?: 0
                val inches = quarters / 4
                val remQuarters = quarters % 4
                uiHelper.speak("$prefix ${fish.species} at $inches inches and $remQuarters quarters. $overOut", "TTS_ANSWER")
            }
            MeasurementMode.CM -> {
                val tenths = fish.totalLengthTenths ?: 0
                val cms = tenths / 10
                val remTenths = tenths % 10
                uiHelper.speak("$prefix ${fish.species} at $cms point $remTenths centimeters. $overOut", "TTS_ANSWER")
            }
        }
    }

    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}