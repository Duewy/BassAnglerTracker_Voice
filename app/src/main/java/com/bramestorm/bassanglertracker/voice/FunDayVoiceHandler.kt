package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FunDayVoiceHandler(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,
    private val dbHelper: CatchDatabaseHelper = CatchDatabaseHelper(context)
) {

    private val measurementMode = SharedPreferencesManager.getFunDayUnit(context)

    private val voiceManager = VoiceInteractionManager(
        context = context,
        uiHelper = uiHelper,
        parser = VoiceParser
    )

    fun onWake() {
        val startPrompt = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "Please say the pounds, ounces, and species of your catch. Over."
            MeasurementMode.KG     -> "Please say the kilograms, grams, and species of your catch. Over."
            MeasurementMode.INCHES -> "Please say the inches, quarters, and species of your catch. Over."
            MeasurementMode.CM     -> "Please say the centimeters and species of your catch. Over."
        }

        voiceManager.startSession(
            prompt = startPrompt,
            onResult = ::onCatchConfirmed
        )
    }

    private fun onCatchConfirmed(transcript: String) {
        val parsed = when (measurementMode) {
            MeasurementMode.LBS_OZ -> VoiceParser.parseImperialCatchSimple(transcript)
            MeasurementMode.KG     -> VoiceParser.parseMetricCatchSimple(transcript)
            MeasurementMode.INCHES -> VoiceParser.parseImperialLengthSimple(transcript)
            MeasurementMode.CM     -> VoiceParser.parseMetricLengthSimple(transcript)
        }

        val confirmPrompt = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "To confirm, your ${parsed.species} is ${parsed.weightLbs} pounds and ${parsed.weightOz} ounces. Is that correct? Over."
            MeasurementMode.KG     -> "To confirm, your ${parsed.species} is ${parsed.weightKgWhole} point ${parsed.weightGrams} kilograms. Is that correct? Over."
            MeasurementMode.INCHES -> "To confirm, your ${parsed.species} is ${parsed.lengthInches} inches and ${parsed.lengthQuarters} quarters. Is that correct? Over."
            MeasurementMode.CM     -> "To confirm, your ${parsed.species} is ${parsed.lengthCm} point ${parsed.lengthTenths} centimeters. Is that correct? Over."
        }

        uiHelper.speak(confirmPrompt, "TTS_CONFIRM")

        Handler(Looper.getMainLooper()).postDelayed({
            voiceManager.startSession(
                prompt = "Please say yes over, no over, or cancel that. Over.",
                onResult = { followUp ->
                    when {
                        followUp.contains("yes over", ignoreCase = true) -> saveCatch(parsed)
                        followUp.contains("no over", ignoreCase = true) -> onWake()
                        followUp.contains("cancel", ignoreCase = true) -> uiHelper.speak("Okay, canceling. Over and Out.")
                        else -> onWake()
                    }
                }
            )
        }, 3500)
    }

    private fun saveCatch(parsed: VoiceParser.ParsedCatch) {
        val catchItem = CatchItem(
            id = 0,
            dateTime = currentTimestamp(),
            longitude = null,
            latitude = null,
            species = parsed.species ?: "Unknown",      // todo we need to separate the Parse from the Save Catch function... Not like this set up.
            totalWeightOz = parsed.weightLbs?.times(16)?.plus(parsed.weightOz ?: 0),
            totalWeightHundredthKg = parsed.weightKgWhole?.times(100)?.plus(parsed.weightGrams ?: 0),
            totalLengthQuarters = parsed.lengthInches?.times(4)?.plus(parsed.lengthQuarters ?: 0),
            totalLengthTenths = parsed.lengthCm?.times(10)?.plus(parsed.lengthTenths ?: 0),
            catchType = measurementMode.name.lowercase(Locale.ROOT),
            markerType = null,
            clipColor = null
        )

        dbHelper.insertCatch(catchItem)

        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent("com.bramestorm.VOICE_CATCH_SAVED")
        )

        val spoken = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "${parsed.species} saved at ${parsed.weightLbs} lbs and ${parsed.weightOz} oz."
            MeasurementMode.KG     -> "${parsed.species} saved at ${parsed.weightKgWhole} point ${parsed.weightGrams} kilograms."
            MeasurementMode.INCHES -> "${parsed.species} saved at ${parsed.lengthInches} inches and ${parsed.lengthQuarters} quarters."
            MeasurementMode.CM     -> "${parsed.species} saved at ${parsed.lengthCm} point ${parsed.lengthTenths} centimeters."
        }

        uiHelper.speak(spoken, "TTS_SAVED")
    }

    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}
