package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchEntryTournament.ClipColor
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.getMeasurementMode
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.utils.FishSpecies
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


    private var isSessionActive = false
    private var sessionRef: ((VoiceInteractionManager) -> Unit)? = null
    private val rawSpecies = SharedPreferencesManager.getTournamentSpecies(context) ?: ""
    private val tournamentSpecies = VoiceInputMapper.getSpeciesFromVoice(rawSpecies, FishSpecies.allSpeciesList)
    private val tournamentCatchLimit = SharedPreferencesManager.getNumberOfCatches(context)


    // Track clip colors already used in official top catches
    private var availableClipColors: MutableList<String> =
        ClipColor.entries.map { it.name }.toMutableList()

    // Manages the TTS → listen → parse → confirm flow
    private val voiceManager = VoiceInteractionManager(
        context = context,
        uiHelper = uiHelper,
        // placeholder parser, replaced immediately in startVoiceSession()
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

    data class ParsedCatch(
        val species: String,
        val weightLbs: Int = 0,
        val weightOz: Int = 0,
        val weightKgWhole: Int = 0,
        val weightGrams: Int = 0,
        val lengthCm: Int = 0,
        val lengthTenths: Int = 0,
        val lengthInches: Int = 0,
        val lengthQuarters: Int = 0,
        val clipColor: String = "",

        val totalWeightOzs: Int = weightLbs * 16 + weightOz,
        val totalWeightHundredthKg: Int = weightKgWhole * 100 + weightGrams,
        val totalLengthTenths: Int = lengthCm * 10 + lengthTenths,
        val totalLengthQuarters: Int = lengthInches * 4 + lengthQuarters,
    )

    /** Called by VoiceControlService on wake */
    fun onWake() {

        startVoiceSession()
    }

    /** Starts a new voice session for lbs/oz catches */
    private fun startVoiceSession() {
        // ensure no double looping
        if (isSessionActive) {
            Log.w("VCC_PROTECT", "Session already active — aborting duplicate start.")
            return
        }
        // SET UP the Available Clip Colors to double check for User Error
        val usedColors = dbHelper.getTopTournamentCatches(tournamentCatchLimit)
            .mapNotNull { it.clipColor?.uppercase() }
            .toSet()

        availableClipColors = ClipColor.entries.map { it.name }
            .filterNot { usedColors.contains(it.uppercase()) }
            .toMutableList()

        // Inject a parser that wraps our static parse logic into the VoiceCommandParser interface
        val mode = SharedPreferencesManager.getTournamentUnit(context)

        // ✅ Let the service track the session for call shutdown
        sessionRef?.invoke(voiceManager)

        isSessionActive = true// Tell the if that yes the Voice Session is Active

        voiceManager.parser = object : VoiceCommandParser {     // SENDS everything to VoiceParser to get the values
            override fun parse(input: String): VoiceCommandParser.ParseResult {

                Log.d("VCC_DEBUG", "🐟 SpeciesList sent to parser: ${listOf(tournamentSpecies)}")

                val parsed = when (mode) {
                    MeasurementMode.LBS_OZ -> VoiceParser.parseImperialCatchWithClips(input, listOf(tournamentSpecies), availableClipColors)
                    MeasurementMode.KG     -> VoiceParser.parseMetricCatchWithClips(input, listOf(tournamentSpecies), availableClipColors)
                    MeasurementMode.INCHES -> VoiceParser.parseImperialLengthWithClips(input, listOf(tournamentSpecies), availableClipColors)
                    MeasurementMode.CM     -> VoiceParser.parseMetricLengthWithClips(input, listOf(tournamentSpecies), availableClipColors)
                }
                if (parsed.totalWeightOzs == 0 || parsed.species.isBlank() || parsed.clipColor.isBlank()) {
                    Log.e("VCC_PARSE", "❌ Incomplete catch — skipping save. Weight=${parsed.totalWeightOzs}, Species='${parsed.species}', Clip='${parsed.clipColor}'")
                    uiHelper.speak("Sorry, I couldn't understand your catch. Please try again. Over.")
                    return VoiceCommandParser.ParseResult.Failure("Missing required info")
                }

                val clip = parsed.clipColor

                Log.d("VCC", "🧠 Parsed Voice Input → " +
                        "Species=${parsed.species}, " +
                        "WeightLbs=${parsed.weightLbs}, " +
                        "WeightOz=${parsed.weightOz}, " +
                        "TotalOz=${parsed.totalWeightOzs}, " +
                        "ClipColor=${clip}")

                val confirmationPrompt = when (mode) {
                    MeasurementMode.LBS_OZ -> "To confirm, the ${parsed.species} is ${parsed.weightLbs} pounds and ${parsed.weightOz} ounces and is on the $clip clip. Is that correct? over"
                    MeasurementMode.KG     -> "To confirm, your ${parsed.species} is ${parsed.weightKgWhole} point ${parsed.weightGrams} kilograms on the $clip clip. Is that correct? over"
                    MeasurementMode.INCHES -> "To confirm, your ${parsed.species} is ${parsed.lengthInches} and ${parsed.lengthQuarters}quarter inches long on the $clip clip. Is that correct? over"
                    MeasurementMode.CM     -> "To confirm, your ${parsed.species} is ${parsed.lengthCm} point ${parsed.lengthTenths} centimeters, on the $clip clip. Is that correct? over"
                }
                Log.d("VCC_CONFIRM_PROMPT", "🧾 $confirmationPrompt")
                    // TOAST just to be able to see what the VCC heard while learning the app...
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (powerManager.isInteractive) {
                   val toast= Toast.makeText(context, "Heard: $confirmationPrompt", Toast.LENGTH_LONG)
                    toast.setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, -100)
                    toast.show()
                }

                val catchData = ConfirmedCatch(
                    weightOz = if (mode == MeasurementMode.LBS_OZ) parsed.totalWeightOzs else null,
                    weightKgs = if (mode == MeasurementMode.KG) parsed.totalWeightHundredthKg else null,
                    lengthQuarters = if (mode == MeasurementMode.INCHES) parsed.totalLengthQuarters else null,
                    lengthTenths = if (mode == MeasurementMode.CM) parsed.totalLengthTenths else null,
                    species = parsed.species,
                    clipColor = clip
                )

                return VoiceCommandParser.ParseResult.Confirm(catchData, confirmationPrompt)
            }

            override fun awaitConfirmation(
                lastCatch: ConfirmedCatch,
                onConfirmed: (ConfirmedCatch) -> Unit
            ) {
                Log.d("VCC_CONFIRM_PROMPT", "Awaiting confirmation for: ${lastCatch.species}, ${lastCatch.weightOz}oz, ${lastCatch.clipColor}")

                // Inject confirmation parser FIRST
                voiceManager.parser = object : VoiceCommandParser {
                    override fun parse(input: String): VoiceCommandParser.ParseResult {
                        val cleaned = input.trim().lowercase()

                        val normalized = cleaned
                            .replace(Regex("[^a-z\\s]"), "")
                            .replace("clip", "")
                            .trim()

                        Log.d("VCC_CONFIRM_RAW", "Raw input: $cleaned")
                        Log.d("VCC_CONFIRM_CLEAN", "Normalized: $normalized")

                        return when {
                            normalized.contains("yes over") -> {
                                Log.d("VCC_CONFIRM", "✅ User confirmed catch.")
                                onConfirmed(lastCatch)
                                uiHelper.speak("Catch saved. Over and Out.")
                                VoiceCommandParser.ParseResult.Confirm(lastCatch, "confirmed")
                            }
                            normalized.contains("no over") -> {
                                Log.d("VCC_CONFIRM", "↩️ User rejected catch.")
                                startVoiceSession()
                                VoiceCommandParser.ParseResult.Confirm(lastCatch, "restarting")
                            }
                            normalized.contains("cancel that") -> {
                                Log.d("VCC_CONFIRM", "❌ User canceled catch.")
                                uiHelper.speak("All canceled. Over and Out.")
                                endVoiceSession()
                                VoiceCommandParser.ParseResult.Confirm(lastCatch, "cancelled")
                            }
                            else -> {
                                Log.d("VCC_CONFIRM", "🤷 Unrecognized: $normalized")
                                uiHelper.speak("Please say yes over, no over, or cancel that. Over.")
                                awaitConfirmation(lastCatch, onConfirmed)
                                VoiceCommandParser.ParseResult.Confirm(lastCatch, "retrying")
                            }
                        }
                    }

                    override fun awaitConfirmation(
                        lastCatch: ConfirmedCatch,
                        onConfirmed: (ConfirmedCatch) -> Unit
                    ) { /* no-op */ }
                }

            }// === END override Await Confirmation ======

        } //=== END Voice Command Parser =================

        val startPrompt = when (mode) {
            MeasurementMode.LBS_OZ -> "Please tell me the pounds and ounces, the species and the clip color of your catch, over"
            MeasurementMode.KG     -> "Please say the kilograms, grams, species and clip color of your catch, over"
            MeasurementMode.INCHES -> "Please say the inches, quarters, species and clip color of your catch, over"
            MeasurementMode.CM     -> "Please say the centimeters, millimeters, species and clip color of your catch, over"
        }

        voiceManager.startSession(
            prompt = startPrompt,
            onCatchConfirmed = ::onCatchConfirmed)

    }   // ====== END start Voice Session ====================

    private fun endVoiceSession() {
        isSessionActive = false
        voiceManager.shutdown()
    }


    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    /** Called when user confirms a parsed catch via voice */
    private fun onCatchConfirmed(catch: ConfirmedCatch) {

        Log.d("VCC", "📥 Confirmed Catch: " +        // what are the actual values sent to database
                "Species=${catch.species}, " +
                "TotalOz=${catch.weightOz}, " +
                "ClipColor=${catch.clipColor}")

        // set the proper catchType, and markerType variables to send to the DataBase
        val mode = catch.getMeasurementMode() ?: MeasurementMode.LBS_OZ
        val typeEntry = when (mode) {
            MeasurementMode.LBS_OZ -> "lbsOzs"
            MeasurementMode.KG     -> "kgs"
            MeasurementMode.INCHES -> "inches"
            MeasurementMode.CM     -> "metric"
        }

        val speciesInitial = when (catch.species) {
                "Largemouth"   -> "L"
                "Smallmouth"   -> "S"
                "Spotted"      -> "P"
                else           -> ""
        }

        // 1) Save new catch to database
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
            catchType = typeEntry,
            markerType = speciesInitial,
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

        Log.d("VCC", "📊 Top Tournament Catches Retrieved: ${topCatches.size}")
        culled.forEach {
            Log.d("VCC", "🗑️ Culled Catch: ${it.species}, ${it.totalWeightOz}oz, Clip=${it.clipColor}")
        }


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
        Log.d("VCC", "📣 Spoken Feedback: $spokenSummary")


        // 5) Ensure Voice Session is over
        endVoiceSession()

    }//=== END on Catch Confirm ================

    private fun ConfirmedCatch.getMeasurementMode(): MeasurementMode? {
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
        ref(voiceManager) // ✅ immediately pass reference back for tracking
    }



}// =============== END ===========================================
