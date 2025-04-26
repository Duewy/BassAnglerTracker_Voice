package com.bramestorm.bassanglertracker.training

/**
 * ParsedCatch carries voice-extracted data for a catch:
 *  - weight in lbs+oz
 *  - weight in kg+grams (hundredths of kg)
 *  - length in cm+tenth
 *  - length in inches+eighth
 *  - species
 *  - clip color
 */

data class ParsedCatch(
    val species: String,
    val weightLbs: Int = 0,
    val weightOz: Int = 0,
    val lengthTenths: Int = 0,
    val lengthA8ths: Int = 0,
    val totalWeightOzs: Int = 0,
    val totalWeightHundredthKg: Int = 0,   // kilograms ×100 + grams
    val totalLengthTenths: Int = 0,         // centimeters ×10 + tenths
    val totalLengthA8th: Int = 0,           // inches ×8 + eighths
    val clipColor: String = ""
)

class VoiceCatchParse {

    fun parseVoiceCommand(command: String): ParsedCatch? {
        val lower = command.lowercase()

        // ✅ Extract kg & grams: e.g. "2.35 kg" or "2 kilograms and 35 grams"
        val kgRegex = Regex("""(\d+)(?:\.(\d{1,2}))?\s*(?:kilograms|kgs|kg)""")
        val kgMatch = kgRegex.find(lower)
        val totalWeightHundredthKg = kgMatch?.let {
            val kg = it.groupValues[1].toIntOrNull() ?: 0
            val dec = it.groupValues.getOrNull(2)?.let { d ->
                // pad or trim to 2 digits
                val s = d.padEnd(2, '0').take(2)
                s.toIntOrNull() ?: 0
            } ?: 0
            kg * 100 + dec
        } ?: 0

        // ✅ Extract weight (lbs & oz)
        val weightRegex = Regex("""(\d+)\s*(?:pounds|lbs|lb)\s*(\d+)?\s*(?:ounces|oz)?""")
        val weightMatch = weightRegex.find(lower)
        val weightLbs = weightMatch?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        val weightOz = weightMatch?.groups?.get(2)?.value?.toIntOrNull() ?: 0

        // ✅ Extract metric length (cm + tenths)
        val metricRegex = Regex("""(\d+)(?:\.(\d))?\s*(?:cm|centimeters)""")
        val metricMatch = metricRegex.find(lower)
        val totalLengthTenths = metricMatch?.let {
            val cm = it.groupValues[1].toIntOrNull() ?: 0
            val tenths = it.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            cm * 10 + tenths
        } ?: 0

        // ✅ Extract imperial length (inches + eighths)
        val inchRegex = Regex("""(\d+)\s*(?:and)?\s*(\d+)/8\s*(?:inches|in)""")
        val inchMatch = inchRegex.find(lower)
        val totalLengthA8th = inchMatch?.let {
            val inches = it.groupValues[1].toIntOrNull() ?: 0
            val eighths = it.groupValues[2].toIntOrNull() ?: 0
            inches * 8 + eighths
        } ?: 0

        // ✅ Handle known species
        val knownSpecies = listOf(
            "largemouth bass", "smallmouth bass", "largemouth", "smallmouth",
            "walleye", "perch", "crappie", "pike", "catfish", "panfish"
        )
        val speciesFound = knownSpecies.firstOrNull { lower.contains(it) }
        val normalizedSpecies = when (speciesFound) {
            "largemouth", "largemouth bass" -> "Large Mouth"
            "smallmouth", "smallmouth bass" -> "Small Mouth"
            else -> speciesFound?.replaceFirstChar { it.uppercase() } ?: ""
        }

        // ✅ Extract clip color
        val clipColorRegex = Regex("""on (?:the )?(\w+)\s*(?:clip)?""")
        val colorMatch = clipColorRegex.find(lower)
        val rawColor = colorMatch?.groups?.get(1)?.value ?: ""
        val clipColor = rawColor.replaceFirstChar { it.uppercase() }

        val hasWeight = (weightLbs > 0 || weightOz > 0 || totalWeightHundredthKg > 0)
        val hasMetricLen = totalLengthTenths > 0
        val hasImperialLen = totalLengthA8th > 0

        return if (normalizedSpecies.isNotBlank() && (hasWeight || hasMetricLen || hasImperialLen)) {
            ParsedCatch(
                species = normalizedSpecies,
                weightLbs = weightLbs,
                weightOz = weightOz,
                totalWeightHundredthKg = totalWeightHundredthKg,
                totalLengthTenths = totalLengthTenths,
                totalLengthA8th = totalLengthA8th,
                clipColor = clipColor
            )
        } else {
            null
        }
    }
}