package com.bramestorm.bassanglertracker.voice

import com.bramestorm.bassanglertracker.training.VoiceInputMapper

data class ParsedCatch(
    val species: String? = null,
    val weightLbs: Int? = null,
    val weightOz: Int? = null,
    val weightKgWhole: Int? = null,
    val weightGrams: Int? = null,
    val lengthInches: Int? = null,
    val lengthQuarters: Int? = null,
    val lengthCm: Int? = null,
    val lengthTenths: Int? = null,
    val clipColor: String? = null
)

object VoiceParser {

    /**
     * Apply misheard-word corrections using alias map
     */
    private fun correctMisheardWords(input: String): String {
        var result = input
        VoiceInputMapper.baseSpeciesVoiceMap.forEach { (alias, correct) ->
            if (result.contains(alias, ignoreCase = true)) {
                result = result.replace(alias, correct, ignoreCase = true)
            }
        }
        return result
    }

    // ======== 🏆 TOURNAMENT MODE PARSERS ==========

    fun parseImperialCatchWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val lbs = Regex("""(\d{1,2})\s*(?:pounds|lbs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val oz = Regex("""(\d{1,2})\s*(?:ounces|ozs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val color = clipColors.firstOrNull { text.contains(it.lowercase()) }

        return ParsedCatch(
            species = species,
            weightLbs = lbs,
            weightOz = oz,
            clipColor = color
        )
    }

    fun parseMetricCatchWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val kg = Regex("""(\d+)\s*(?:kilograms|kg|kgs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val grams = Regex("""(\d{1,3})\s*(?:grams|g)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val color = clipColors.firstOrNull { text.contains(it.lowercase()) }

        return ParsedCatch(
            species = species,
            weightKgWhole = kg,
            weightGrams = grams,
            clipColor = color
        )
    }

    fun parseImperialLengthWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val inches = Regex("""(\d{1,2})\s*(?:inches|ins?|\")""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val quarters = Regex("""(\d)\s*(?:quarters?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val color = clipColors.firstOrNull { text.contains(it.lowercase()) }

        return ParsedCatch(
            species = species,
            lengthInches = inches,
            lengthQuarters = quarters,
            clipColor = color
        )
    }

    fun parseMetricLengthWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val cm = Regex("""(\d+(?:\.\d+)?)\s*(?:cm|centimeters?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val color = clipColors.firstOrNull { text.contains(it.lowercase()) }
        val tenths = if (cm != null) (cm * 10).toInt() else null

        return ParsedCatch(
            species = species,
            lengthCm = cm,
            lengthTenths = tenths,
            clipColor = color
        )
    }

    // ======== 🎣 FUN DAY MODE PARSERS ==========

    fun parseImperialCatchSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.baseSpeciesVoiceMap.values.firstOrNull { text.contains(it.lowercase()) }
        val lbs = Regex("""(\d{1,2})\s*(?:pounds|lbs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val oz = Regex("""(\d{1,2})\s*(?:ounces|ozs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()

        return ParsedCatch(
            species = species,
            weightLbs = lbs,
            weightOz = oz
        )
    }

    fun parseMetricCatchSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.baseSpeciesVoiceMap.values.firstOrNull { text.contains(it.lowercase()) }
        val kg = Regex("""(\d+)\s*(?:kilograms|kg|kgs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val grams = Regex("""(\d{1,3})\s*(?:grams|g)""").find(text)?.groupValues?.get(1)?.toIntOrNull()

        return ParsedCatch(
            species = species,
            weightKgWhole = kg,
            weightGrams = grams
        )
    }

    fun parseImperialLengthSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.baseSpeciesVoiceMap.values.firstOrNull { text.contains(it.lowercase()) }
        val inches = Regex("""(\d{1,2})\s*(?:inches|ins?|\")""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val quarters = Regex("""(\d)\s*(?:quarters?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()

        return ParsedCatch(
            species = species,
            lengthInches = inches,
            lengthQuarters = quarters
        )
    }

    fun parseMetricLengthSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.baseSpeciesVoiceMap.values.firstOrNull { text.contains(it.lowercase()) }
        val cm = Regex("""(\d+(?:\.\d+)?)\s*(?:cm|centimeters?)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val tenths = if (cm != null) (cm * 10).toInt() else null

        return ParsedCatch(
            species = species,
            lengthCm = cm,
            lengthTenths = tenths
        )
    }
}
