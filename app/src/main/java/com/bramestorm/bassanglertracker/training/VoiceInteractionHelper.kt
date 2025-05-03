package com.bramestorm.bassanglertracker.training

import android.speech.RecognitionListener
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.training.VoiceInputMapper.saveUserVoiceMap
import com.bramestorm.bassanglertracker.voice.VoiceInteractionFlows
import com.bramestorm.bassanglertracker.voice.VoiceStep

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

class VoiceInteractionHelper(
    private val activity: AppCompatActivity,
    private val currentMode: CatchMode,
    private val recognitionListener: RecognitionListener
) {
    private val responseManager = VoiceResponseManager(activity)
    private val commandManager = VoiceCommandManager(
        activity,
        onCommandReceived = { spokenText -> handleTranscript(spokenText) },
        onError = { speak("Sorry, I didn't catch that.") },
        onAlreadyListening = {
            Toast.makeText(activity, "Voice already listening. Please wait...", Toast.LENGTH_SHORT).show()
        }
    )

    private lateinit var currentFlow: List<VoiceStep>
    private var currentStepIndex = 0

    var onCommandRecognized: ((String) -> Unit)? = null
    lateinit var userVoiceMap: MutableMap<String, String>

    private var lastUnknownPhrase: String? = null
    private var unknownPhraseFailCount: Int = 0
    private val correctionThreshold = 4

    fun startAddCatchSequence() = startCatchSequence()

    fun startCatchSequence() {
        val catchStep = buildCatchStep(currentMode)
        runFlow(listOf(VoiceInteractionFlows.wakeStep, catchStep))
    }

    private fun runFlow(flow: List<VoiceStep>) {
        currentFlow = flow
        currentStepIndex = 0
        speak(currentFlow[0].prompt(this))
        startListening { _ -> }
    }

    fun handleTranscript(transcript: String) {
        val cleanedInput = transcript.trim().lowercase()
        val mapped = userVoiceMap[cleanedInput] ?: cleanedInput

        if (VoiceCommandList.isKnownTournamentCommand(mapped)) {
            onCommandRecognized?.invoke(mapped)
            lastUnknownPhrase = null
            unknownPhraseFailCount = 0
        } else {
            if (mapped.isBlank()) {
                speak("I didn’t catch anything. Please try again.")
                return
            }

            if (mapped == lastUnknownPhrase) {
                unknownPhraseFailCount++
            } else {
                lastUnknownPhrase = mapped
                unknownPhraseFailCount = 1
            }

            if (unknownPhraseFailCount >= correctionThreshold) {
                promptToSaveUnknownPhrase(mapped)
            } else {
                speak("Sorry, I didn’t understand. Try again.")
            }
        }
    }

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

    fun startListening(onResult: (String) -> Unit) = commandManager.startListening()
    fun stopListening() = commandManager.stopListening()

    fun speak(message: String, onDone: (() -> Unit)? = null) = responseManager.speak(message, onDone)

    fun listenForConfirmation(onResult: (String) -> Unit) {
        startListening { confirmationTranscript ->
            val response = confirmationTranscript.trim().lowercase()
            onResult(response)
        }
    }

    fun shutdown() {
        Log.d("Voice", "🔻 VoiceInteractionHelper shutting down...")
        commandManager.stopListening()
        commandManager.shutdown()
        responseManager.shutdown()
    }

    private fun promptToSaveUnknownPhrase(unknownPhrase: String) {
        speak("I didn’t recognize that. Should I remember this phrase for next time?")
        listenForConfirmation { userResponse ->
            if (userResponse == "yes") {
                speak("Okay. What did you mean to say?")
                listenForConfirmation { intendedPhrase ->
                    userVoiceMap[unknownPhrase] = intendedPhrase.lowercase()
                    saveUserVoiceMap(activity, userVoiceMap)
                    speak("Got it. I’ll remember that.")
                }
            } else {
                speak("Okay, not saving it.")
            }
        }
    }

    fun isTournamentMode(): Boolean = when (currentMode) {
        CatchMode.TOURNAMENT_LBS_OZS,
        CatchMode.TOURNAMENT_INCHES_QUARTERS,
        CatchMode.TOURNAMENT_METRIC,
        CatchMode.TOURNAMENT_KGS -> true
        else -> false
    }

    private fun getNextClipColor(): String = TODO("Provide next available clip color")
    private fun caughtCount(): Int = TODO("Return number of fish caught")
    private fun targetCount(): Int = TODO("Return tournament catch limit")
    private fun totalWeightLbsOz(): String = TODO("Format total weight")
    private fun timeToAlarm(): String = TODO("Return time until alarm")
    private fun saveCatch(species: String, lbs: Int, oz: Int): Nothing = TODO("Persist catch to DB")
    private fun finishFlow() = stopListening()

    private fun buildCatchStep(mode: CatchMode): VoiceStep {
        val speciesList = listOf("largemouth bass", "smallmouth bass", "crappie", "pike", "perch", "walleye", "catfish", "panfish")
        val speciesPattern = speciesList.joinToString("|") { Regex.escape(it) }
        val clipColorsArray = if (isTournamentMode()) activity.resources.getStringArray(R.array.clip_colors) else emptyArray()
        val clipPattern = clipColorsArray.joinToString("|") { Regex.escape(it) }

        val promptText = "I’m listening for your catch. Say, \"I have caught a <species> and it weighs <X> pounds and <Y> ounces${if (isTournamentMode()) " and I put it on the Red clip" else ""}, Over.\""

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
                val spokenColor = match.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }
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
