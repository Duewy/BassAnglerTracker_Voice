package com.bramestorm.bassanglertracker.voice

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.getComparisonValueByMode
import com.bramestorm.bassanglertracker.utils.FishSpecies
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles voice-driven tournament catch entries and queries.
 * Logs extensively for end-to-end tracing.
 */

class TournamentVoiceHandler(
    private val context: Context,
    private val uiHelper: VoiceUiHelper,
    private val alarmHour: Int = -1,
    private val alarmMinute: Int = -1,
    private val dbHelper: CatchDatabaseHelper = CatchDatabaseHelper(context)
) : VoiceSessionHandler {

    private val maxParseRetries    = 3
    private val maxQuestionRetries = 3

    private var parseRetryCount    = 0
    private var questionRetryCount = 0

    private var lastCatchItem: CatchItem? = null // keep track of the last catch we inserted
    private val tournamentCatchLimit = SharedPreferencesManager.getNumberOfCatches(context)
    private val measurementMode = SharedPreferencesManager.getTournamentUnit(context)
    private val speciesList = SharedPreferencesManager.getTournamentSpecies(context)?.split(",")?.map { it.trim() } ?: FishSpecies.allSpeciesList
    private val clipColors = listOf( "BLUE","YELLOW", "GREEN",  "ORANGE", "WHITE", "RED")
    private var inQuestionMode = false


    // Initial prompt based on mode
    private fun getStartPrompt(): String = when (measurementMode) {
        MeasurementMode.LBS_OZ -> "Please say the pounds, ounces, species, and clip color of your catch. Over."
        MeasurementMode.POUNDS -> "Please say the point pounds, species, and clip color of your catch. Over."
        MeasurementMode.KG     -> "Please say the kilograms, grams, species, and clip color of your catch. Over." //todo should we have "point kilograms" to tell the user to say 4.15 kilograms ???
        MeasurementMode.INCHES -> "Please say the inches, quarters, species, and clip color of your catch. Over."
        MeasurementMode.CM     -> "Please say the centimeters, species, and clip color of your catch. Over."
    }

    /** Entry point for service wake or media-button tap. */
    override fun onWake() {
        Log.d(TAG, "onWake() called")
        // Clear any stuck session flags that block restart
        endSession("Forced reset via Bluetooth button")
        startVoiceSession()
    }

    /** Begins a catch or question session. */
    private fun startVoiceSession() {
        if (inQuestionMode) return            // don’t restart the “catch” flow mid-question

        (context as? VoiceControlService)
            ?.startVoiceSession(getStartPrompt(), uiHelper) { transcript ->
                val clean = transcript.trim().lowercase()
                when {
                    clean.contains("question") && clean.contains("over") -> handleQuestionMode()
                    else                            -> parseAndConfirm(transcript)
                }
            } ?: run  {                   // fallback: fire the same Intent the media button uses
                   val intent = Intent(context, VoiceControlService::class.java)
                       .setAction(VoiceControlService.ACTION_START_VOICE)
                   ContextCompat.startForegroundService(context, intent)
               }
    }

    /** Parses numeric + text components, then confirms with the user. */
    private fun parseAndConfirm(transcript: String) {
        val parsed = when (measurementMode) {
            MeasurementMode.LBS_OZ -> VoiceParser.parseLbsOzsCatchWithClips(transcript, speciesList, clipColors)
            MeasurementMode.POUNDS -> VoiceParser.parsePoundsCatchWithClips(transcript, speciesList, clipColors)
            MeasurementMode.KG     -> VoiceParser.parseKgsCatchWithClips(transcript, speciesList, clipColors)
            MeasurementMode.INCHES -> VoiceParser.parseImperialLengthWithClips(transcript, speciesList, clipColors)
            MeasurementMode.CM     -> VoiceParser.parseMetricLengthWithClips(transcript, speciesList, clipColors)
        }

        // Simple sanity check for measurement unit overflow
        val oz     = parsed.weightOz
        val dec     = parsed.weightDec
        val grams  = parsed.weightGrams
        val quarters = parsed.lengthQuarters
        val tenths = parsed.lengthTenths

        if ((measurementMode == MeasurementMode.LBS_OZ && oz > 15) ||
            (measurementMode == MeasurementMode.POUNDS && dec > 99) ||
            (measurementMode == MeasurementMode.KG     && grams > 99) ||
            (measurementMode == MeasurementMode.INCHES && quarters > 3) ||
            (measurementMode == MeasurementMode.CM     && tenths > 9)) {

            Log.w(TAG, "❌ Invalid unit detected → oz=$oz, grams=$grams, quarters=$quarters, tenths=$tenths")
            uiHelper.speak("That value was out of range. Say it again or say cancel that. Over.", "TTS_INVALID_UNIT")
            (context as? VoiceControlService)?.startVoiceSession(getStartPrompt(), uiHelper) { response ->
                if (response.contains("cancel", true)) {
                    uiHelper.speak("Cancelled. Over and Out.", "TTS_CANCEL")
                    endSession("cancel from confirm prompt")
                } else {
                    parseAndConfirm(response)
                }
            }
        }


        val missingInfo = parsed.species.isBlank() || parsed.clipColor.isBlank() || when (measurementMode) {
            MeasurementMode.LBS_OZ -> parsed.totalWeightOzs == 0
            MeasurementMode.POUNDS -> parsed.totalWeightHundredthPounds == 0
            MeasurementMode.KG     -> parsed.totalWeightHundredthKg == 0
            MeasurementMode.INCHES -> parsed.totalLengthQuarters == 0
            MeasurementMode.CM     -> parsed.totalLengthTenths == 0

        }

        Log.d(TAG, "Parsed result: \$parsed")

        if (missingInfo) {  parseRetryCount++
            if (parseRetryCount > maxParseRetries) {
                uiHelper.speak("Okay, let’s try again later. Over and Out.", "TTS_FAIL")
                endSession("too many parse retries")
                return
            }
            uiHelper.speak("Sorry, I missed some info—let’s try again. Over.", "TTS_RETRY")
            Handler(Looper.getMainLooper()).postDelayed({
                startVoiceSession()
            }, 1500)
            return
        }


        parseRetryCount = 0     // successful parse → reset counter

        // Build confirmation prompt

        val confirmPrompt = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "To confirm, your ${parsed.species} is ${parsed.weightLbs} pounds and ${parsed.weightOz} ounces on the ${parsed.clipColor} clip. Is that correct?"
            MeasurementMode.POUNDS -> "To confirm, your ${parsed.species} is ${parsed.weightPounds} point ${parsed.weightDec} pounds on the ${parsed.clipColor} clip. Is that correct?"
            MeasurementMode.KG     -> "To confirm, your ${parsed.species} is ${parsed.weightKgWhole} point ${parsed.weightGrams} kilograms on the ${parsed.clipColor} clip. Is that correct?"
            MeasurementMode.INCHES -> "To confirm, your ${parsed.species} is ${parsed.lengthInches} inches and ${parsed.lengthQuarters} quarters on the ${parsed.clipColor} clip. Is that correct?"
            MeasurementMode.CM     -> "To confirm, your ${parsed.species} is ${parsed.lengthCm} point ${parsed.lengthTenths} centimeters on the ${parsed.clipColor} clip. Is that correct?"
        }

        Log.d(TAG, "Confirm prompt: \$confirmPrompt")
        Log.d(TAG, "Confirm Species🐟: \${parsed.species}")
        uiHelper.speak(confirmPrompt, "TTS_CONFIRM")


        // Listen for yes/no/cancel
        Handler(Looper.getMainLooper()).postDelayed({
            (context as? VoiceControlService)?.let { svc ->
                svc.startVoiceSession(
                    "Please say yes, no, or cancel that, Over.",
                    uiHelper
                ) { response ->
                    when {
                        response.contains("yes ", true)    -> saveCatch(parsed)
                        response.contains("no ",  true)    -> startVoiceSession()
                        response.contains("cancel", true) -> {
                            uiHelper.speak("Catch cancelled. Over and Out.", "TTS_CANCEL")
                            endSession("cancel from confirm prompt") // ✅ RESET SESSION HERE
                        }
                        else -> {
                            uiHelper.speak("Sorry, please say yes over no over or cancel that. Over.","TTS_RETRY")
                            Handler(Looper.getMainLooper()).postDelayed({ parseAndConfirm(transcript) }, 3500)
                        }
                    }
                }
            } ?: run {
                // fallback to Intent
                endSession("If fallback voice session fails")
                val intent = Intent(context, VoiceControlService::class.java)
                    .setAction(VoiceControlService.ACTION_START_VOICE)
                ContextCompat.startForegroundService(context, intent)
            }
        }, 3500)
    }

    /** Persists the parsed catch and provides feedback. */
    private fun saveCatch(parsed: VoiceParser.ParsedCatch) {
        val typeEntry = when (measurementMode) {
            MeasurementMode.LBS_OZ -> "lbsOzs"
            MeasurementMode.POUNDS -> "pounds"
            MeasurementMode.KG -> "kgs"
            MeasurementMode.INCHES -> "inches"
            MeasurementMode.CM -> "metric"
        }

        val markerType = when (parsed.species.lowercase()) {
            "largemouth" -> "L"
            "smallmouth" -> "S"
            "spotted" -> "P"
            else -> ""
        }

        val dbItem = CatchItem(
            id = 0,
            dateTime = currentTimestamp(),
            longitude = null,
            latitude = null,
            species = parsed.species,
            totalWeightOz = parsed.totalWeightOzs.takeIf { measurementMode == MeasurementMode.LBS_OZ },
            totalWeightHundredthPounds = parsed.totalWeightHundredthPounds.takeIf { measurementMode == MeasurementMode.POUNDS },
            totalWeightHundredthKg = parsed.totalWeightHundredthKg.takeIf { measurementMode == MeasurementMode.KG },
            totalLengthQuarters = parsed.totalLengthQuarters.takeIf { measurementMode == MeasurementMode.INCHES },
            totalLengthTenths = parsed.totalLengthTenths.takeIf { measurementMode == MeasurementMode.CM },
            catchType = typeEntry,
            markerType = markerType,
            clipColor = parsed.clipColor
        )
        Log.d(TAG, "Checking the actual Species is: \$species")

        dbHelper.insertCatch(dbItem)
        lastCatchItem = dbItem// remember this one for question mode

        Log.d(TAG, "DB insert succeeded: \$dbItem")

        // Notify UI
        Log.d(TAG, "Broadcasting catch saved event")

        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent("com.bramestorm.VOICE_CATCH_SAVED")
        )

        // Feedback summary if on last or penultimate catch
        val stats = TournamentVoiceFeedback.analyzeTournamentStats(
            dbHelper,
            tournamentCatchLimit,
            alarmHour,
            alarmMinute,
            dbItem,
            measurementMode
        )
            // only speak the full summary when this is your last or penultimate call
        val pos = stats.thisCatchPosition
        val limit = tournamentCatchLimit
        if (pos == limit || pos == limit - 1) {
            val summary = TournamentVoiceFeedback.getCatchSummaryResponse(stats)
            uiHelper.speak(summary, "TTS_FEEDBACK")
        }

        uiHelper.speak("Catch is saved. Over and Out.", "TTS_SAVED")
        endSession("catch successfully saved")
    }

    override fun shutdown() {
        // Add cleanup logic if needed in the future
        Log.d("TournamentVoiceHandler", "🔻 shutdown called")
    }

 //??????????????????? 🤔 QUESTION MODE ❓🤔?????????????????????????????????

    /** Switch into question mode for stats queries. */
    private fun handleQuestionMode() {
        questionRetryCount = 0
        inQuestionMode = true
        Log.d(TAG, "Question mode activated")
        uiHelper.speak(
            "Question mode activated, Over.",
            "TTS_QUESTION_INTRO"
        )
        (context as? VoiceControlService)?.startVoiceSession(
            "Which stat would you like? Over.",
            uiHelper
        ) { followUp ->
            Log.d(TAG, "Question received: '\$followUp'")
            routeQuestion(followUp)
        }
    }

    /** Routes a user question to the appropriate response. */
    private fun routeQuestion(question: String) {

        val overOut = "Over and Out."   // just a cute way to add the "Over and out." to speakFish questions
        Log.d(TAG, "routeQuestion('\$question')")
        // Prepare list and stats

        //  Check for “cancel” command ❌ to get out of the VCC Question section
        if (question.contains("cancel", ignoreCase = true)) {
            uiHelper.speak("Okay, exiting question mode. Over and out.", "TTS_CANCEL")
            endSession("cancel from confirm prompt")
            return
        }

        // 1️⃣ rebuild your top-N list here
        val fullList = dbHelper.getTopTournamentCatches(tournamentCatchLimit + 6)
        val sortedDesc = fullList.sortedByDescending { it.getComparisonValueByMode(measurementMode) }
        val cullList = sortedDesc.take(tournamentCatchLimit)     // “cull list” = your tournamentCatchLimit biggest fish
        val catch = lastCatchItem ?: run {
            Log.w(TAG, "No last catch—cannot answer questions yet Over and out.")
            uiHelper.speak("I don't have a catch to ask about, Over and Out.","TTS_ERROR" )
            return
        }

        // re-compute stats once
               val stats = TournamentVoiceFeedback.analyzeTournamentStats(
                   dbHelper,
                   tournamentCatchLimit,
                   alarmHour,
                   alarmMinute,
                   catch,
                   measurementMode
               )

        when {

            question.contains("smallest", true) -> {
                Log.d(TAG, "Answering smallest fish")
                if (cullList.isEmpty()) {
                    uiHelper.speak("I don’t have enough catches yet to tell you the smallest. $overOut","TTS_ANSWER")
                } else {
                    val fish = cullList.last()
                    speakFish(fish, "Your smallest fish on the list is", overOut)
                }
            }

            question.contains("largest", true) -> {
                Log.d(TAG, "Answering largest fish")
                cullList.firstOrNull()?.let { fish ->
                    speakFish(fish, "Your largest fish on the list is", overOut)
                } ?: uiHelper.speak("You do not have any catches logged yet. $overOut","TTS_ANSWER")
            }

            question.contains("total weight", true) -> {
                Log.d(TAG, "Answering total weight")
                val msg = when (measurementMode) {
                    MeasurementMode.LBS_OZ ->
                        "Your total weight is ${stats.totalWeightLbs} pounds ${stats.totalWeightRemainingOz} ounces. $overOut"
                    MeasurementMode.POUNDS ->
                        "Your total weight is ${stats.totalWeightPounds}.${stats.totalWeightDec} Pounds. $overOut"
                    MeasurementMode.KG ->
                        "Your total weight is ${stats.totalWeightKgs}.${stats.totalWeightGrams} kilograms. $overOut"
                    MeasurementMode.INCHES ->
                        // if they ask “total weight” in a length-mode, you can still fall back
                        "Weight stats aren’t available in inches mode. $overOut"
                    MeasurementMode.CM ->
                        "Weight stats aren’t available in centimeter mode. $overOut"
                }
                uiHelper.speak(msg, "TTS_ANSWER")
            }

            question.contains("total length", true) -> {
                Log.d(TAG, "Answering total length")
                val msg = when (measurementMode) {
                    MeasurementMode.LBS_OZ ->
                        // if they ask length in a weight-mode, fall back or say “length not available”
                        "Length stats aren’t available in pounds/ounces mode. $overOut"
                    MeasurementMode.POUNDS ->
                        "Length stats aren’t available in Pounds mode. $overOut"
                    MeasurementMode.KG ->
                        "Length stats aren’t available in kilograms mode. $overOut"
                    MeasurementMode.INCHES ->
                        "Your total length is ${stats.totalLengthInches} and ${stats.totalLengthFourths} inches. $overOut"
                    MeasurementMode.CM ->
                        "Your total length is ${stats.totalLengthCms}.${stats.totalLengthDec} centimeters. $overOut"
                }
                uiHelper.speak(msg, "TTS_ANSWER")
            }

            question.contains("position", true) ->{
                Log.d(TAG, "Answering position of catch")
                uiHelper.speak(
                    "This catch is number ${stats.thisCatchPosition} on the culling list. $overOut",
                    "TTS_ANSWER"
                )}

            question.contains("time since", true) ->{
                Log.d(TAG, "Answering time since last catch")
                uiHelper.speak(
                    "It’s been ${stats.timeSinceLastCatchMin} minutes since your last catch. $overOut",
                    "TTS_ANSWER"
                )}

            question.contains("time remaining", true) ->{
                Log.d(TAG, "Answering time remaining")
                uiHelper.speak(
                    "${stats.timeUntilAlarmMin} minutes remain in the tournament. $overOut",
                    "TTS_ANSWER"
                )}

            else -> {
                questionRetryCount++
                if (questionRetryCount > maxQuestionRetries) {
                    uiHelper.speak("Okay, exiting question mode. $overOut", "TTS_FAIL")
                    endSession("too many question retries")
                } else {
                    uiHelper.speak(
                        "Sorry, I did not catch that. Say smallest, largest, total weight, position or time left.$overOut",
                        "TTS_RETRY_QUESTION"
                    )
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleQuestionMode()
                    }, 1500)
                }
            }
        }

    }

    // helper to pick the corresponding units:
    private fun speakFish(fish: CatchItem, prefix: String, overOut: String) {
        when (measurementMode) {
            MeasurementMode.LBS_OZ -> {
                val oz = fish.totalWeightOz ?: 0
                val lbs = oz / 16
                val remOz = oz % 16
                uiHelper.speak(
                    "$prefix ${fish.species} at $lbs pounds and $remOz ounces.$overOut",
                    "TTS_ANSWER"
                )
            }
            MeasurementMode.POUNDS -> {
                val hundredthsPounds = fish.totalWeightHundredthPounds ?: 0
                val pounds = hundredthsPounds / 100
                val dec = hundredthsPounds % 100
                uiHelper.speak(
                    "$prefix ${fish.species} at $pounds point $dec pounds. $overOut",
                    "TTS_ANSWER"
                )
            }
            MeasurementMode.KG -> {
                val hundredths = fish.totalWeightHundredthKg ?: 0
                val kgs = hundredths / 100
                val grams = hundredths % 100
                uiHelper.speak(
                    "$prefix ${fish.species} at $kgs point $grams kilograms. $overOut",
                    "TTS_ANSWER"
                )
            }
            MeasurementMode.INCHES -> {
                val quarters = fish.totalLengthQuarters ?: 0
                val inches = quarters / 4
                val remQuarters = quarters % 4
                uiHelper.speak(
                    "$prefix ${fish.species} at $inches inches and $remQuarters quarters. $overOut",
                    "TTS_ANSWER"
                )
            }
            MeasurementMode.CM -> {
                val tenths = fish.totalLengthTenths ?: 0
                val cms = tenths / 10
                val remTenths = tenths % 10
                uiHelper.speak(
                    "$prefix ${fish.species} at $cms point $remTenths centimeters. $overOut",
                    "TTS_ANSWER"
                )
            }
        }
    }

    private fun endSession(reason: String = "User cancel") {
        Log.d(TAG, "Session ended: $reason")
        (context as? VoiceControlService)?.markSessionComplete()
        inQuestionMode = false
        parseRetryCount = 0
        questionRetryCount = 0
    }

    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}
