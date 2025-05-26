package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchEntryTournament.ClipColor
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.getMeasurementMode
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.voice.VoiceCommandParser.ConfirmedCatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles hands-free tournament-mode catch entries via voice.
 */
class TournamentVoiceHandler(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,
    private val alarmHour: Int = -1,
    private val alarmMinute: Int = -1,
    private val dbHelper: CatchDatabaseHelper = CatchDatabaseHelper(context),) {

    private val tournamentSpecies = SharedPreferencesManager.getTournamentSpecies(context) ?: ""
    private val tournamentCatchLimit = SharedPreferencesManager.getNumberOfCatches(context)
    private val cullingEnabled = SharedPreferencesManager.isCullingEnabled(context)  // not really needed as all Tournament are culling Enabled

    // Track clip colors already used in official top catches
    private var availableClipColors: MutableList<String> =
        ClipColor.entries.map { it.name }.toMutableList()

    // Manages the TTS → listen → parse → confirm flow
    private val voiceManager = VoiceInteractionManager(
        context = context,
        uiHelper = uiHelper,
                                //todo LOOK INTO the placeholder parser, which will be replaced in startVoiceSession()
        parser = object : VoiceCommandParser {
            override fun parse(input: String) =
                VoiceCommandParser.ParseResult.Failure("Parser not initialized")

            override fun awaitConfirmation(
                lastCatch: ConfirmedCatch,
                onConfirmed: (ConfirmedCatch) -> Unit
            ) {
            }
        }
    )

    companion object {
        fun getInstance(
            context: Context,
            uiHelper: VoiceUiHelper,
            alarmHour: Int,
            alarmMinute: Int
        ): TournamentVoiceHandler {
            val dbHelper = CatchDatabaseHelper(context)
            return TournamentVoiceHandler(context, uiHelper, alarmHour, alarmMinute, dbHelper)
        }
    }


    /** Called by VoiceControlService on wake */
    fun onWake() {
        startVoiceSession()         // todo find the sequence of Vcc communication to ensure proper flow and grammar with variables
    }

    /** Starts a new voice session for lbs/oz catches */
    private fun startVoiceSession() {
        // Inject a parser that wraps our static parse logic into the VoiceCommandParser interface
        val mode = SharedPreferencesManager.getTournamentUnit(context)

        voiceManager.parser = object : VoiceCommandParser {
            override fun parse(input: String): VoiceCommandParser.ParseResult {
                val parsed = when (mode) {
                    MeasurementMode.LBS_OZ -> VoiceParser.parseImperialCatch(input, listOf(tournamentSpecies), availableClipColors)
                    MeasurementMode.KG     -> VoiceParser.parseMetricCatch(input, listOf(tournamentSpecies), availableClipColors)
                    MeasurementMode.INCHES -> VoiceParser.parseImperialLength(input, listOf(tournamentSpecies))
                    MeasurementMode.CM     -> VoiceParser.parseMetricLength(input, listOf(tournamentSpecies))
                }

                val clip = parsed.clipColor ?: availableClipColors.firstOrNull().orEmpty()

                val confirmationPrompt = when (mode) {
                    MeasurementMode.LBS_OZ -> "To confirm, you caught a ${parsed.weightLbs} pound ${parsed.weightOz} ounce ${parsed.species} on the $clip clip. Is that correct? over"
                    MeasurementMode.KG     -> "To confirm, you caught a ${parsed.weightKgWhole} kilogram ${parsed.weightGrams} gram ${parsed.species} on the $clip clip. Is that correct? over"
                    MeasurementMode.INCHES -> "To confirm, your catch was ${parsed.lengthInches} and ${parsed.lengthQuarters} inches long  ${parsed.species} on the $clip clip. Is that correct? over"
                    MeasurementMode.CM     -> "To confirm, your catch was ${parsed.lengthCm} centimeters ${parsed.species} on the $clip clip. Is that correct? over" //todo add the millimeters here Look into 0.0 or 0.5 as standards??
                }

                val catchData = ConfirmedCatch(
                    weightOz = if (mode == MeasurementMode.LBS_OZ) ((parsed.weightLbs ?: 0) * 16 + (parsed.weightOz ?: 0)) else null,
                    weightKgs = if (mode == MeasurementMode.KG) ((parsed.weightKgWhole ?: 0) + (parsed.weightGrams ?: 0) / 100.0) else null,
                    lengthQuarters = if (mode == MeasurementMode.INCHES) ((parsed.lengthInches ?: 0) * 4 + (parsed.lengthQuarters ?: 0)) else null,
                    lengthTenths = if (mode == MeasurementMode.CM) ((parsed.lengthCm?.times(10))?.toInt() ?: 0) else null,
                    species = parsed.species ?: "Unknown",
                    clipColor = clip
                )

                return VoiceCommandParser.ParseResult.Confirm(catchData, confirmationPrompt)
            }

            override fun awaitConfirmation(lastCatch: ConfirmedCatch, onConfirmed: (ConfirmedCatch) -> Unit) {
                onConfirmed(lastCatch)
            }
        }

        val startPrompt = when (mode) {
            MeasurementMode.LBS_OZ -> "Please say the pounds, ounces, species and clip color of your catch, over"
            MeasurementMode.KG     -> "Please say the kilograms, grams, species and clip color of your catch, over"
            MeasurementMode.INCHES -> "Please say the inches, quarters, species and clip color of your catch, over"
            MeasurementMode.CM     -> "Please say the centimeters, millimeters, species and clip color of your catch, over"
        }


        voiceManager.startSession(
            prompt = startPrompt,
            onCatchConfirmed = ::onCatchConfirmed)

    }   // ====== END start Voice Session ====================


    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    /** Called when user confirms a parsed catch via voice */
    private fun onCatchConfirmed(catch: ConfirmedCatch) {
        // 1) Save new catch to database
        val mode = catch.getMeasurementMode() ?: MeasurementMode.LBS_OZ

        val dbItem = CatchItem(
            id = 0,
            dateTime = currentTimestamp(),
            longitude = null,
            latitude = null,
            species = catch.species,
            totalWeightOz = catch.weightOz,
            totalWeightHundredthKg = if (mode == MeasurementMode.KG) ((catch.weightKgs ?: 0.0) * 100).toInt() else null,
            totalLengthQuarters = catch.lengthQuarters,
            totalLengthTenths = catch.lengthTenths,
            catchType = mode.name.lowercase(Locale.ROOT),
            markerType = catch.clipColor,
            clipColor = catch.clipColor
        )

        dbHelper.insertCatch(dbItem)
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent("com.bramestorm.VOICE_CATCH_SAVED")
        )

        // 2) Fetch Tournament Information to use for Feed Back and Queries
        val allTopCatches = dbHelper.getTopTournamentCatches(tournamentCatchLimit + 1)
        val topCatches = allTopCatches.take(tournamentCatchLimit)
        val culled = allTopCatches.drop(tournamentCatchLimit)


        val stats = TournamentVoiceFeedback.analyzeTournamentStats(
            dbHelper = dbHelper,
            tournamentCatchLimit = tournamentCatchLimit,
            alarmHour = alarmHour,
            alarmMinute = alarmMinute,
            currentCatch = dbItem,
            mode = dbItem.getMeasurementMode() ?: MeasurementMode.LBS_OZ
        )

        // 3) Refresh available clip colors to avoid repeats
        val usedColors = topCatches.mapNotNull { it.clipColor?.uppercase() }.toSet()
        availableClipColors = ClipColor.values()
            .map { it.name }
            .filterNot { usedColors.contains(it) }
            .toMutableList()

         // 4) Speak feedback
        val spokenSummary = TournamentVoiceFeedback.getCatchSummaryResponse(stats)
        uiHelper.speak(spokenSummary)

    }//=== END on Catch Confirm ================

    fun ConfirmedCatch.getMeasurementMode(): MeasurementMode? {
        return when {
            this.weightOz != null -> MeasurementMode.LBS_OZ
            this.weightKgs != null -> MeasurementMode.KG
            this.lengthQuarters != null -> MeasurementMode.INCHES
            this.lengthTenths != null -> MeasurementMode.CM
            else -> null
        }
    }


}// =============== END ===========================================
