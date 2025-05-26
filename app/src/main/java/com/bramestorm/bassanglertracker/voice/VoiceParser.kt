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
    val lengthCm: Double? = null,
    val lengthTenths: Int? = null,
    val clipColor: String? = null
)

object VoiceParser {
    /**
     * 1) Pre-process transcript for known mis-heard 🙉 species aliases
     */
    fun correctMisheardWords(
        input: String,
        speciesList: List<String>
    ): String {
        var corrected = input
        VoiceInputMapper.baseSpeciesVoiceMap.forEach { (alias, species) ->
            if (corrected.contains(alias, ignoreCase = true)) {
                corrected = corrected.replace(alias, species, ignoreCase = true)
            }
        }
        return corrected
    }

    /** 🦈 PARSE WEIGHT IN LBS + OZ 🎣 **/
    fun parseImperialCatch(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase(), speciesList)
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val lbs = Regex("""(\d{1,2})\s*(?:pounds|lbs?)""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val oz = Regex("""(\d{1,2})\s*(?:ounces|ozs?)""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val color = clipColors.firstOrNull { text.contains(it.lowercase()) }
        return ParsedCatch(
            species = species,
            weightLbs = lbs,
            weightOz = oz,
            weightKgWhole = null,
            weightGrams = null,
            lengthInches = null,
            lengthQuarters = null,
            lengthCm = null,
            lengthTenths = null,
            clipColor = color
        )
    }

    /** PARSE WEIGHT IN KG + GRAMS **/
    fun parseMetricCatch(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase(), speciesList)
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val kgWhole = Regex("""(\d+)\s*(?:kilograms|kgs?|kg)""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val grams = Regex("""(\d{1,3})\s*(?:grams|g)""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val color = clipColors.firstOrNull { text.contains(it.lowercase()) }
        return ParsedCatch(
            species = species,
            weightLbs = null,
            weightOz = null,
            weightKgWhole = kgWhole,
            weightGrams = grams,
            lengthInches = null,
            lengthQuarters = null,
            lengthCm = null,
            lengthTenths = null,
            clipColor = color
        )
    }

    /** PARSE LENGTH IN INCHES + QUARTERS **/
    fun parseImperialLength(
        transcript: String,
        speciesList: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase(), speciesList)
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val inches = Regex("""(\d{1,2})\s*(?:inches|ins?|\")""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val quarters = Regex("""(\d)\s*(?:quarters?)""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        return ParsedCatch(
            species = species,
            weightLbs = null,
            weightOz = null,
            weightKgWhole = null,
            weightGrams = null,
            lengthInches = inches,
            lengthQuarters = quarters,
            lengthCm = null,
            lengthTenths = null,
            clipColor = null
        )
    }

    /** PARSE LENGTH IN CM + TENTHS **/
    fun parseMetricLength(
        transcript: String,
        speciesList: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase(), speciesList)
        val species = speciesList.firstOrNull { text.contains(it.lowercase()) }
        val cm = Regex("""(\d+(?:\.\d+)?)\s*(?:cm|centimeters?)""")
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val tenths = Regex("""\.(\d)\s*(?:tenths?)""")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        return ParsedCatch(
            species = species,
            weightLbs = null,
            weightOz = null,
            weightKgWhole = null,
            weightGrams = null,
            lengthInches = null,
            lengthQuarters = null,
            lengthCm = cm,
            lengthTenths = tenths,
            clipColor = null
        )
    }

    // Parser objects for easy import
    object LbsOzParser {
        fun parse(
            transcript: String,
            speciesList: List<String>,
            clipColors: List<String>
        ): ParsedCatch = parseImperialCatch(transcript, speciesList, clipColors)
    }

    object KgParser {
        fun parse(
            transcript: String,
            speciesList: List<String>,
            clipColors: List<String>
        ): ParsedCatch = parseMetricCatch(transcript, speciesList, clipColors)
    }

    object InchesParser {
        fun parse(
            transcript: String,
            speciesList: List<String>
        ): ParsedCatch = parseImperialLength(transcript, speciesList)
    }

    object CmParser {
        fun parse(
            transcript: String,
            speciesList: List<String>
        ): ParsedCatch = parseMetricLength(transcript, speciesList)
    }

}
