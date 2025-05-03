package com.bramestorm.bassanglertracker.voice

import com.bramestorm.bassanglertracker.training.VoiceInputMapper

data class ParsedCatch(val species: String?, val lbs: Int?, val oz: Int?, val clipColor: String?)

object VoiceParser {

    // Step 1: Pre-process the transcript for known misheard words
    private fun correctMisheardWords(input: String): String {
        var corrected = input
        VoiceInputMapper.baseSpeciesVoiceMap.forEach { (_, aliasList) ->
            aliasList.forEach { alias ->
                if (corrected.contains(alias, ignoreCase = true)) {
                    corrected = corrected.replace(alias, VoiceInputMapper.getSpeciesFromVoice(alias.toString()), ignoreCase = true)
                }
            }
        }
        return corrected
    }

    // Step 2: Parse the corrected input
    fun parseImperialCatch(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val corrected = correctMisheardWords(transcript.lowercase())

        val species = speciesList.firstOrNull { corrected.contains(it.lowercase()) }

        val lbsRegex = Regex("""(\d{1,2})\s*(pounds|lbs?)""")
        val lbs = lbsRegex.find(corrected)?.groupValues?.get(1)?.toIntOrNull()

        val ozRegex = Regex("""(\d{1,2})\s*(ounces|ozs?)""")
        val oz = ozRegex.find(corrected)?.groupValues?.get(1)?.toIntOrNull()

        val clipColor = clipColors.firstOrNull { corrected.contains(it.lowercase()) }

        return ParsedCatch(species, lbs, oz, clipColor)
    }

    // Later: Add parseKgs(), parseMetric(), parseInches(), etc.



    private fun String.replace(alias: Char, speciesFromVoice: String, ignoreCase: Boolean): String {
        return ".."
    }//todo fix this up for tomorrow

}

