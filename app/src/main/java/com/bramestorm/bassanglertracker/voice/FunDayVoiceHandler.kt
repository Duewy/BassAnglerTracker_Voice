package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.voice.VoiceCommandParser.ConfirmedCatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FunDayVoiceHandler(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,
    private val dbHelper: CatchDatabaseHelper = CatchDatabaseHelper(context)
) {
    private var sessionRef: ((VoiceInteractionManager) -> Unit)? = null
    private val measurementMode = SharedPreferencesManager.getFunDayUnit(context)

    private val voiceManager = VoiceInteractionManager(
        context = context,
        uiHelper = uiHelper,
        parser = object : VoiceCommandParser {
            override fun parse(input: String): VoiceCommandParser.ParseResult {
                val parsed = when (measurementMode) {
                    MeasurementMode.LBS_OZ -> VoiceParser.parseImperialCatchSimple(input)
                    MeasurementMode.KG     -> VoiceParser.parseMetricCatchSimple(input)
                    MeasurementMode.INCHES -> VoiceParser.parseImperialLengthSimple(input)
                    MeasurementMode.CM     -> VoiceParser.parseMetricLengthSimple(input)
                }

                val confirmationPrompt = when (measurementMode) {
                    MeasurementMode.LBS_OZ -> "To confirm, you caught a ${parsed.weightLbs} pound ${parsed.weightOz} ounce ${parsed.species}. Is that correct? over"
                    MeasurementMode.KG     -> "To confirm, you caught a ${parsed.weightKgWhole} kilogram ${parsed.weightGrams} gram ${parsed.species}. Is that correct? over"
                    MeasurementMode.INCHES -> "To confirm, your catch was ${parsed.lengthInches} and ${parsed.lengthQuarters} inches long, ${parsed.species}. Is that correct? over"
                    MeasurementMode.CM     -> "To confirm, your catch was ${parsed.lengthCm} centimeters, ${parsed.species}. Is that correct? over"
                }

                val catchData = ConfirmedCatch(
                    weightOz = if (measurementMode == MeasurementMode.LBS_OZ) (((parsed.weightLbs ?: 0) * 16) + (parsed.weightOz ?: 0)) else null,
                    weightKgs = if (measurementMode == MeasurementMode.KG) ((parsed.weightKgWhole ?: 0) + (parsed.weightGrams ?: 0) / 100.0) else null,
                    lengthQuarters = if (measurementMode == MeasurementMode.INCHES) (((parsed.lengthInches ?: 0) * 4) + (parsed.lengthQuarters ?: 0)) else null,
                    lengthTenths = if (measurementMode == MeasurementMode.CM) ((parsed.lengthCm?.times(10))?.toInt() ?: 0) else null,
                    species = parsed.species ?: "Unknown",
                    clipColor = null.toString() // Not used in Fun Day mode
                )

                return VoiceCommandParser.ParseResult.Confirm(catchData, confirmationPrompt)
            }

            override fun awaitConfirmation(lastCatch: ConfirmedCatch, onConfirmed: (ConfirmedCatch) -> Unit) {
                onConfirmed(lastCatch)
            }
        }
    )// === END == Voice Interaction Manager ========

    companion object {
        fun getInstance(
            context: Context,
            uiHelper: VoiceUiHelper
        ): FunDayVoiceHandler {
            val dbHelper = CatchDatabaseHelper(context)
            return FunDayVoiceHandler(context, uiHelper, dbHelper)
        }
    }

    fun onWake() {
        val startPrompt = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "Please say the pounds, ounces, and species of your catch. Over"
            MeasurementMode.KG     -> "Please say the kilograms, grams, and species of your catch. Over"
            MeasurementMode.INCHES -> "Please say the inches, quarters, and species of your catch. Over"
            MeasurementMode.CM     -> "Please say the centimeters, and species of your catch. Over"
        }

        sessionRef?.invoke(voiceManager)

        voiceManager.startSession(
            prompt = startPrompt,
            onCatchConfirmed = ::onCatchConfirmed
        )
    }

    private fun onCatchConfirmed(catch: ConfirmedCatch) {
        val mode = catch.getMeasurementMode() ?: MeasurementMode.LBS_OZ

        val dbItem = CatchItem(
            id = 0,
            dateTime = currentTimestamp(),
            longitude = null,
            latitude = null,
            species = catch.species,
            totalWeightOz = catch.weightOz,
            totalWeightHundredthKg = catch.weightKgs,
            totalLengthQuarters = catch.lengthQuarters,
            totalLengthTenths = catch.lengthTenths,
            catchType = mode.name.lowercase(Locale.ROOT),
            markerType = null,
            clipColor = null
        )

        dbHelper.insertCatch(dbItem)

        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent("com.bramestorm.VOICE_CATCH_SAVED")
        )

        val successMessage = when (mode) {
            MeasurementMode.LBS_OZ -> "${catch.species} saved at ${catch.weightOz!! / 16} lbs ${catch.weightOz % 16} oz"
            MeasurementMode.KG     -> "${catch.species} saved at ${catch.weightKgs} kg"
            MeasurementMode.INCHES -> "${catch.species} saved at ${catch.lengthQuarters!! / 4} in ${catch.lengthQuarters % 4} quarters"
            MeasurementMode.CM     -> "${catch.species} saved at ${catch.lengthTenths?.toDouble()?.div(10)} cm"
        }

        uiHelper.speak(successMessage)
    }// == END == on Catch Confirmed ====================

    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun ConfirmedCatch.getMeasurementMode(): MeasurementMode? {
        return when {
            this.weightOz != null -> MeasurementMode.LBS_OZ
            this.weightKgs != null -> MeasurementMode.KG
            this.lengthQuarters != null -> MeasurementMode.INCHES
            this.lengthTenths != null -> MeasurementMode.CM
            else -> null
        }
    }

    fun setSessionRef(ref: (VoiceInteractionManager) -> Unit) {
        this.sessionRef = ref
    }


}//=== END == Fun Day Voice Handler ============
