// === VoiceInteractionHelper.kt ===
package com.bramestorm.bassanglertracker.training

import android.speech.RecognitionListener
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.voice.VoiceInteractionFlows
import com.bramestorm.bassanglertracker.voice.VoiceStep

/**
 * Modes for different catch entry screens.
 */
enum class CatchMode {
    FUN_LBS_OZS,
    TOURNAMENT_LBS_OZS,
    FUN_INCHES_QUARTERS,
    TOURNAMENT_INCHES_QUARTERS,
    FUN_METRIC,
    TOURNAMENT_METRIC,
    FUN_KGS,
    TOURNAMENT_KGS
}

/**
 * Centralized helper for all voice-driven interactions (e.g., Add Catch flow).
 */
class VoiceInteractionHelper(
    private val activity: AppCompatActivity,
    private val currentMode: RecognitionListener,
    private val onCommandAction: CatchMode
){
    private val responseManager = VoiceResponseManager(activity)
    private val commandManager = VoiceCommandManager(
        activity,
        onCommandReceived = { spokenText -> handleRecognitionResult(spokenText) },
        onError = { _ -> speak("Sorry, I didn't catch that.") },
        onAlreadyListening = { Toast.makeText(activity, "Voice already listening. Please wait...", Toast.LENGTH_SHORT).show() }
    )

    // Flow state
    private lateinit var currentFlow: List<VoiceStep>
    private var currentStepIndex = 0

    /**
     * Legacy alias for startCatchSequence
     */
    fun startAddCatchSequence() = startCatchSequence()

    /**
     * Entry point: builds and runs the two-step catch flow.
     */
    fun startCatchSequence() {
        val catchStep = buildCatchStep(currentMode)
        runFlow(listOf(VoiceInteractionFlows.wakeStep, catchStep))
    }

    /**
     * Drives a list of VoiceSteps from start to finish.
     */
    private fun runFlow(flow: List<VoiceStep>) {
        currentFlow = flow
        currentStepIndex = 0
        speak(currentFlow[0].prompt(this))
        startListening()
    }

    /**
     * Called by RecognitionListener when speech is recognized.
     */
    private fun handleRecognitionResult(spokenText: String) {
        val step = currentFlow[currentStepIndex]
        val match = step.pattern.find(spokenText)
        if (match != null) {
            step.onMatch(match, this)
            currentStepIndex++
            if (currentStepIndex < currentFlow.size) {
                speak(currentFlow[currentStepIndex].prompt(this))
            } else {
                stopListening()
            }
        } else {
            step.onNoMatch(this)
        }
    }

    /**
     * Start listening via the SpeechRecognizer.
     */
    fun startListening() = commandManager.startListening()

    /**
     * Stop the SpeechRecognizer.
     */
    fun stopListening() = commandManager.stopListening()

    /**
     * Speak text via TTS and optional callback on done.
     */
    fun speak(message: String, onDone: (() -> Unit)? = null) = responseManager.speak(message, onDone)

    /**
     * Shutdown and clean up voice resources.
     */
    fun shutdown() {
        Log.d("Voice", "🔻 VoiceInteractionHelper shutting down...")
        commandManager.stopListening()
        commandManager.shutdown()
        responseManager.shutdown()
    }

    // --- Application-specific hooks to implement ---

    fun isTournamentMode(): Boolean = when (currentMode) {
        CatchMode.TOURNAMENT_LBS_OZS,
        CatchMode.TOURNAMENT_INCHES_QUARTERS,
        CatchMode.TOURNAMENT_KGS -> true
        else -> false
    }

    fun getNextClipColor(): String = TODO("Provide next available clip color from your logic")
    fun caughtCount(): Int = TODO("Return number of fish caught so far")
    fun targetCount(): Int = TODO("Return tournament limit or 1 for Fun Day")
    fun totalWeightLbsOz(): String = TODO("Compute and format cumulative weight in lbs/oz")
    fun timeToAlarm(): String = TODO("Compute time remaining until alarm or return current time")
    fun saveCatch(species: String, lbs: Int, oz: Int): Nothing = TODO("Persist the catch to your DB")
    fun finishFlow() = stopListening()

    /**
     * Dynamically builds the catch-utterance step based on mode, species list, and clip colors.
     */
    private fun buildCatchStep(mode: CatchMode): VoiceStep {
        val speciesList = /* load from SharedPreferences */ listOf("largemouth bass", "smallmouth bass", "crappie", "pike", "perch", "walleye", "catfish", "panfish")
        val speciesPattern = speciesList.joinToString("|") { Regex.escape(it) }
        val clipColorsArray = if (isTournamentMode()) activity.resources.getStringArray(R.array.clip_colors) else emptyArray()
        val clipPattern = clipColorsArray.joinToString("|") { Regex.escape(it) }

        val promptText = "I’m listening for your catch. Say, “I have caught a <species> and it weighs <X> pounds and <Y> ounces${if (isTournamentMode()) " and I put it on the Red clip" else ""}, Over.”"
        val regexPattern =
            """
            I have caught a\s+($speciesPattern)\s+and it weighs\s+(\d{1,2})\s*(?:pounds|lbs?)\s*(?:and\s*)?(\d{1,2})\s*(?:ounces|ozs?)(?:\s+and I put it on the\s+($clipPattern)\s+clip)?\s*,\s*Over
            """.trimIndent()

        return VoiceStep(
            prompt = { promptText },
            pattern = Regex(regexPattern, setOf(RegexOption.IGNORE_CASE, RegexOption.COMMENTS)),
            onMatch = { match, helper ->
                val species = match.groupValues[1]
                val lbs = match.groupValues[2].toInt()
                val oz = match.groupValues[3].toInt()
                val spokenColor = match.groupValues.getOrNull(4).takeIf { it!!.isNotEmpty() }
                val color = spokenColor ?: if (helper.isTournamentMode()) helper.getNextClipColor() else ""

                helper.saveCatch(species, lbs, oz)

                val base = "So you have caught a $species weighing $lbs pounds and $oz ounces"
                val clipSummary = if (color.isNotBlank()) "—put on the $color clip." else ""
                val tally = if (helper.isTournamentMode())
                    " You now have ${helper.caughtCount()}/${helper.targetCount()} fish, total ${helper.totalWeightLbsOz()}."
                else
                    " You’ve now caught ${helper.totalWeightLbsOz()}."
                val alarmSummary = " ${helper.timeToAlarm()} until alarm."

                helper.speak("$base$clipSummary$tally$alarmSummary Over and out.")
                helper.finishFlow()
            }
        )
    }
}
