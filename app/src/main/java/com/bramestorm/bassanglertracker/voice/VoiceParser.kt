package com.bramestorm.bassanglertracker.voice


import android.util.Log
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.bramestorm.bassanglertracker.training.VoiceInputMapper.getClipColorFromVoice


object VoiceParser {

    data class ParsedCatch(
        val species: String,
        val weightLbs: Int = 0,
        val weightOz: Int = 0,
        val weightPounds: Int = 0,
        val weightDec: Int = 0,
        val weightKgWhole: Int = 0,
        val weightGrams: Int = 0,
        val lengthCm: Int = 0,
        val lengthTenths: Int = 0,
        val lengthInches: Int = 0,
        val lengthQuarters: Int = 0,
        val clipColor: String = "",

            // math functions for values
        val totalWeightOzs: Int = ((weightLbs * 16) + weightOz),
        val totalWeightHundredthPounds: Int = (( weightPounds * 100) + weightDec),
        val totalWeightHundredthKg: Int = ((weightKgWhole * 100) + weightGrams),
        val totalLengthTenths: Int = ((lengthCm * 10) + lengthTenths),
        val totalLengthQuarters: Int = ((lengthInches * 4) + lengthQuarters),
    )

    private val clipColorWords = listOf( "BLUE","YELLOW", "GREEN",  "ORANGE", "WHITE","RED")


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

    //-------------------------------------------------------------------------------------------
    fun parseLbsOzsCatchWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())
        val detectedClipColor = getClipColorFromVoice(cleanText, clipColors)
        val inputWithoutClip = removeClipColor(cleanText, detectedClipColor)

        val species = VoiceInputMapper.getSpeciesFromVoice(
            inputWithoutClip
                .replace(Regex("""\d+\s*(pounds?|lbs?|ounces?|ozs?)"""), "")
                .replace(Regex("""\b(over|and|clip|color)\b"""), "")
                .trim(),
            speciesList
        )

        val lbs = Regex("""(\d{1,2})\s*(?:pound[s]?|lbs?)""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val oz = Regex("""(\d{1,2})\s*(?:ounce[s]?|ozs?)""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        Log.d("VCC_PARSE", "ParsedCatch → Species=$species, Lbs=$lbs, Oz=$oz, ClipColor=$detectedClipColor")
        Log.d("VCC_STT_RAW", "Raw transcript: $transcript")
        Log.d("VCC_STT_CLEAN", "Cleaned transcript: $cleanText")

        return ParsedCatch(
            species = species,
            weightLbs = lbs,
            weightOz = oz,
            clipColor = detectedClipColor
        )
    }

    //-------------------------------------------------------------------------------------------
    fun parsePoundsCatchWithClips(      //todo check this over there are likely errors...
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())
        val detectedClipColor = getClipColorFromVoice(cleanText, clipColors)
        val inputWithoutClip = removeClipColor(cleanText, detectedClipColor)

        val species = VoiceInputMapper.getSpeciesFromVoice(
            inputWithoutClip
                .replace(Regex("""\d+(?:\.\d{1,2})?\s*(pounds?|lbs?|lb)"""), "")
                .replace(Regex("""\b(over|and|clip|color)\b"""), "")
                .trim(),
            speciesList
        )

        val poundsMatch = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(?:pounds|lbs|lb)""").find(cleanText)
        val lbs = poundsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val dec = poundsMatch?.groupValues?.getOrNull(2)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0

        Log.d("VCC_PARSE", "ParsedCatch Pounds → Species=$species, Lbs= $lbs.$dec, ClipColor=$detectedClipColor")
        Log.d("VCC_STT_RAW", "Raw transcript Pounds: $transcript")

        return ParsedCatch(
            species = species,
            weightPounds = lbs,
            weightDec = dec,
            clipColor = detectedClipColor
        )

    }

    //-------------------------------------------------------------------------------------------
    fun parseKgsCatchWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())
        val detectedClipColor = getClipColorFromVoice(cleanText, clipColors)
        val inputWithoutClip = removeClipColor(cleanText, detectedClipColor)

        val species = VoiceInputMapper.getSpeciesFromVoice(
            inputWithoutClip
                .replace(Regex("""\d+(?:\.\d{1,2})?\s*(kilograms?|kgs?|kg)"""), "")
                .replace(Regex("""\b(over|and|clip|color)\b"""), "")
                .trim(),
            speciesList
        )

        val kgMatch = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(kilograms?|kgs?|kg)""").find(cleanText)
        val kg = kgMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val grams = kgMatch?.groupValues?.getOrNull(2)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            weightKgWhole = kg,
            weightGrams = grams,
            clipColor = detectedClipColor
        )

    }

    //-------------------------------------------------------------------------------------------
    fun parseImperialLengthWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())
        val detectedClipColor = getClipColorFromVoice(cleanText, clipColors)
        val inputWithoutClip = removeClipColor(cleanText, detectedClipColor)

        val species = VoiceInputMapper.getSpeciesFromVoice(
            inputWithoutClip
                .replace(Regex("""\d+\s*(inches?|in|")"""), "")
                .replace(Regex("""\b(over|and|clip|color|quarter|fourth)\b"""), "")
                .trim(),
            speciesList
        )

        val complexMatch = Regex("""(\d+)\s*(?:and)?\s*(one|two|three)\s*(quarters?|fourths?)""").find(cleanText)
        val whole = complexMatch?.groupValues?.get(1)?.toIntOrNull()
        val fractionWord = complexMatch?.groupValues?.get(2)
        val fraction = when (fractionWord) {
            "one" -> 1
            "two" -> 2
            "three" -> 3
            else -> 0
        }

        val inchesOnly = Regex("""(\d+)\s*(inches?|in|")""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val lengthInches = whole ?: inchesOnly
        val lengthQuarters = if (complexMatch != null) fraction else 0

        return ParsedCatch(
            species = species,
            lengthInches = lengthInches,
            lengthQuarters = lengthQuarters,
            clipColor = detectedClipColor
        )
    }

//-------------------------------------------------------------------------------------------
    fun parseMetricLengthWithClips(
        transcript: String,
        speciesList: List<String>,
        clipColors: List<String>
    ): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())
        val detectedClipColor = getClipColorFromVoice(cleanText, clipColors)
        val inputWithoutClip = removeClipColor(cleanText, detectedClipColor)

        val species = VoiceInputMapper.getSpeciesFromVoice(
            inputWithoutClip
                .replace(Regex("""\d+(?:\.\d)?\s*(cm|centimeters?)"""), "")
                .replace(Regex("""\b(over|and|clip|color)\b"""), "")
                .trim(),
            speciesList
        )

        val cmMatch = Regex("""(\d+)(?:\.(\d))?\s*(cm|centimeters?)""").find(cleanText)
        val cm = cmMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tenths = cmMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            lengthCm = cm,
            lengthTenths = tenths,
            clipColor = detectedClipColor
        )
    }


    // ======== 🎣 FUN DAY MODE PARSERS ==========

    fun parseImperialCatchSimple(transcript: String): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())

        val species = VoiceInputMapper.getSpeciesFromVoice(
            cleanText
                .replace(Regex("""\d+\s*(pounds?|lbs?|ounces?|ozs?)"""), "")
                .replace(Regex("""\b(over|and)\b"""), "")
                .trim(),
            emptyList()
        )

        val lbs = Regex("""(\d{1,2})\s*(pounds?|lbs?)""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val oz = Regex("""(\d{1,2})\s*(ounces?|ozs?)""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            weightLbs = lbs,
            weightOz = oz
        )
    }

//------------------------------------------------------------------------------------------

    fun parsePoundsCatchSimple(transcript: String): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())

        val species = VoiceInputMapper.getSpeciesFromVoice(
            cleanText
                .replace(Regex("""\d+(?:\.\d{1,2})?\s*(pounds?|lbs?|lb)"""), "")
                .replace(Regex("""\b(over|and)\b"""), "")
                .trim(),
            emptyList()
        )

        val poundsMatch = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(pounds?|lbs?|lb)""").find(cleanText)
        val lbs = poundsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val dec = poundsMatch?.groupValues?.getOrNull(2)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            weightPounds = lbs,
            weightDec = dec
        )
    }

    //--------------------------------------------------------------------------------------
    fun parseMetricCatchSimple(transcript: String): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())

        val species = VoiceInputMapper.getSpeciesFromVoice(
            cleanText
                .replace(Regex("""\d+(?:\.\d{1,2})?\s*(kilograms?|kgs?|kg)"""), "")
                .replace(Regex("""\b(over|and)\b"""), "")
                .trim(),
            emptyList()
        )

        val kgMatch = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(kilograms?|kgs?|kg)""").find(cleanText)
        val kg = kgMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val grams = kgMatch?.groupValues?.getOrNull(2)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0

        return ParsedCatch(
            species = species,
            weightKgWhole = kg,
            weightGrams = grams
        )
    }

    //-----------------------------------------------------------------------------------------

    fun parseImperialLengthSimple(transcript: String): ParsedCatch {
        val cleanText = correctMisheardWords(transcript.lowercase())

        val species = VoiceInputMapper.getSpeciesFromVoice(
            cleanText
                .replace(Regex("""\d+\s*(inches?|in|")"""), "")
                .replace(Regex("""\b(over|and|quarter|fourth)\b"""), "")
                .trim(),
            emptyList()
        )

        val complexMatch = Regex("""(\d+)\s*(?:and)?\s*(one|two|three)\s*(quarters?|fourths?)""").find(cleanText)
        val whole = complexMatch?.groupValues?.get(1)?.toIntOrNull()
        val fractionWord = complexMatch?.groupValues?.get(2)
        val fraction = when (fractionWord) {
            "one" -> 1
            "two" -> 2
            "three" -> 3
            else -> 0
        }

        val inchesOnly = Regex("""(\d+)\s*(inches?|in|")""").find(cleanText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val lengthInches = whole ?: inchesOnly
        val lengthQuarters = if (complexMatch != null) fraction else 0

        return ParsedCatch(
            species = species,
            lengthInches = lengthInches,
            lengthQuarters = lengthQuarters
        )
    }


//-------------------------------------------------------------------------------------------------------------
fun parseMetricLengthSimple(transcript: String): ParsedCatch {
    val cleanText = correctMisheardWords(transcript.lowercase())

    val species = VoiceInputMapper.getSpeciesFromVoice(
        cleanText
            .replace(Regex("""\d+(?:\.\d)?\s*(cm|centimeters?)"""), "")
            .replace(Regex("""\b(over|and)\b"""), "")
            .trim(),
        emptyList()
    )

    val cmMatch = Regex("""(\d+)(?:\.(\d))?\s*(cm|centimeters?)""").find(cleanText)
    val cm = cmMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val tenths = cmMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0

    return ParsedCatch(
        species = species,
        lengthCm = cm,
        lengthTenths = tenths
    )
}


//-------------------------------------------------------------------------------------------
    private fun removeClipColor(input: String, color: String?): String {
        if (color.isNullOrBlank()) return input

        // Only remove exact match of color as a word
        return input.replace("\\b${Regex.escape(color)}\\b".toRegex(RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ") // clean double spaces
            .trim()
    }


}
