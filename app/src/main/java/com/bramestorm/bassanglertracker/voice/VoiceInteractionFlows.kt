// === VoiceInteractionFlows.kt ===
package com.bramestorm.bassanglertracker.voice

import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper

/**
 * A single step in a voice-driven interaction flow.
 */
data class VoiceStep(
    val prompt: (helper: VoiceInteractionHelper) -> String,
    val pattern: Regex,
    val onMatch: (match: MatchResult, helper: VoiceInteractionHelper) -> Unit,
    val onNoMatch: (helper: VoiceInteractionHelper) -> Unit = { helper ->
        helper.speak("Sorry, I didn’t catch that. Please repeat.")
    }
)

/**
 * Collection of static, reusable steps or configurations for voice flows.
 */
object VoiceInteractionFlows {
    /**
     * First step in any catch flow: wake phrase.
     */
    val wakeStep = VoiceStep(
        prompt = { _ -> "Catch Caddy, here and waiting for your instructions. Over." },
        pattern = Regex(
            ".*over.*",
            setOf(RegexOption.IGNORE_CASE)
        ),
        onMatch = { _, _ -> /* proceed to next step in the flow */ }
    )
}