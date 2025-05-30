package com.bramestorm.bassanglertracker.voice


import android.util.Log
import com.bramestorm.bassanglertracker.training.VoiceInputMapper


object VoiceParser {

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
            // math functions for values
        val totalWeightOzs: Int = ((weightLbs * 16) + weightOz),
        val totalWeightHundredthKg: Int = ((weightKgWhole * 100) + weightGrams),
        val totalLengthTenths: Int = ((lengthCm * 10) + lengthTenths),
        val totalLengthQuarters: Int = ((lengthInches * 4) + lengthQuarters),
    )


            /**  GOTO VoiceInputMapper to
             * Apply misheard-word Corrections ✅ using alias map 📃
             *  and return a Clean Version of STT 🔊
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
        val cleanText = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.getSpeciesFromVoice(
            cleanText
                .replace(Regex("""\d+\s*(pounds?|lbs?|ounces?|ozs?)"""), "")
                .replace(Regex("""\b(over|and|clip|color)\b"""), "")
                .trim(),
            speciesList
        )

        val lbs = Regex("""(\d{1,2})\s*(?:pound[s]?|lbs?)""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val oz = Regex("""(\d{1,2})\s*(?:ounce[s]?|ozs?)""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val color = VoiceInputMapper.getClipColorFromVoice(cleanText, clipColors)

        Log.d("VCC_PARSE", "ParsedCatch → Species=$species, Lbs=$lbs, Oz=$oz, ClipColor=$color") // to see what info is gathered...
        Log.d("VCC_STT_RAW", "Raw transcript: $transcript")
        Log.d("VCC_STT_CLEAN", "Cleaned transcript: $cleanText")

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
        val species = VoiceInputMapper.getSpeciesFromVoice(text, speciesList)
        val kgMatch = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(?:kilograms|kgs|kg)""").find(text)
        val kg = kgMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val grams = kgMatch?.groupValues?.getOrNull(2)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0
        val color = VoiceInputMapper.getClipColorFromVoice(text, clipColors)


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

        val species = VoiceInputMapper.getSpeciesFromVoice(text, speciesList)
        val color = VoiceInputMapper.getClipColorFromVoice(text, clipColors)

        // Pattern: "four and three quarters inches"
        val complexMatch = Regex("""(\d+)\s*(?:and)?\s*(one|two|three)\s*(?:quarters?|fourths?)""").find(text)
        val whole = complexMatch?.groupValues?.get(1)?.toIntOrNull()
        val fractionWord = complexMatch?.groupValues?.get(2)
        val fraction = when (fractionWord) {
            "one" -> 1
            "two" -> 2
            "three" -> 3
            else -> 0
        }

        // Fallback if not matched
        val inchesOnly = Regex("""(\d+)\s*(?:inches|in|")""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val lengthInches = whole ?: inchesOnly
        val lengthQuarters = if (complexMatch != null) fraction else 0

        return ParsedCatch(
            species = species,
            lengthInches = lengthInches,
            lengthQuarters = lengthQuarters,
            clipColor = color
        )
    }


    fun parseMetricLengthWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.getSpeciesFromVoice(text, speciesList)
        val cmMatch = Regex("""(\d+)(?:\.(\d))?\s*(?:cm|centimeters?)""").find(text)
        val cm = cmMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tenths = cmMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
        val color = VoiceInputMapper.getClipColorFromVoice(text, clipColors)

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
        val species = VoiceInputMapper.getSpeciesFromVoice(text, emptyList())
        val lbs = Regex("""(\d{1,2})\s*(?:pounds|lbs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val oz = Regex("""(\d{1,2})\s*(?:ounces|ozs?)""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            weightLbs = lbs,
            weightOz = oz
        )
    }

    fun parseMetricCatchSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.getSpeciesFromVoice(text, emptyList())
        val kgMatch = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(?:kilograms|kgs|kg)""").find(text)
        val kg = kgMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val grams = kgMatch?.groupValues?.getOrNull(2)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            weightKgWhole = kg,
            weightGrams = grams
        )
    }

    fun parseImperialLengthSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.getSpeciesFromVoice(text, emptyList())

        // Pattern: "four and three quarters inches" or "five and one fourth inches"
        val complexMatch = Regex("""(\d+)\s*(?:and)?\s*(one|two|three)\s*(?:quarters?|fourths?)""").find(text)
        val whole = complexMatch?.groupValues?.get(1)?.toIntOrNull()
        val fractionWord = complexMatch?.groupValues?.get(2)
            val fraction = when (fractionWord) {
                "one" -> 1
                "two" -> 2
                "three" -> 3
                else -> 0
            }
        // Fallback: whole number only like "six inches"
        val inchesOnly = Regex("""(\d+)\s*(?:inches|in|")""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val lengthInches = whole ?: inchesOnly
        val lengthQuarters = if (complexMatch != null) fraction else 0

        return ParsedCatch(
            species = species,
            lengthInches = lengthInches,
            lengthQuarters = lengthQuarters
        )
    }


    fun parseMetricLengthSimple(transcript: String): ParsedCatch {
        val text = correctMisheardWords(transcript.lowercase())
        val species = VoiceInputMapper.getSpeciesFromVoice(text, emptyList())
        val cmMatch = Regex("""(\d+)(?:\.(\d))?\s*(?:cm|centimeters?)""").find(text)
        val cm = cmMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tenths = cmMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0


        return ParsedCatch(
            species = species,
            lengthCm = cm,
            lengthTenths = tenths
        )
    }
}
