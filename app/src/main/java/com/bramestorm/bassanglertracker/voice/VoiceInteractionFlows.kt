package com.bramestorm.bassanglertracker.voice

data class VoiceStep(
    val prompt: String,
    val pattern: Regex,
    val onMatch: (MatchResult) -> Unit,
    val onNoMatch: () -> Unit = { /* reprompt */ }
)

object VoiceInteractionFlows {
    val addCatchFlow = listOf(
        VoiceStep(
            prompt  = "Catch Caddy here. Over to you.",
            pattern = Regex(".*over.*", RegexOption.IGNORE_CASE),
            onMatch = { /* move to speciesStep */ }
        ),
        VoiceStep(
            prompt  = "Please say species.",
            pattern = Regex("(largemouth bass|smallmouth bass|crappie|...)", RegexOption.IGNORE_CASE),
            onMatch = { match -> /* store species = match.groups[1] */ },
            onNoMatch = { /* "Sorry, didn’t catch that—species again." */ }
        ),
        VoiceStep( /* weight step */ ),
        VoiceStep( /* length step, if needed */ ),
        VoiceStep( /* clip color step */ ),
        VoiceStep(
            prompt  = "So you caught a {species} weighing {lbs} pounds {oz} ounces on the {color} clip—correct? Over.",
            pattern = Regex(".*over and out.*", RegexOption.IGNORE_CASE),
            onMatch = { /* saveCatch() */ }
        )
    )
}
