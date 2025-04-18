package com.bramestorm.bassanglertracker.training

data class ParsedCatch(
    val species: String,
    val weightLbs: Int,
    val weightOz: Int,
    val clipColor: String
)

class VoiceCatchParse {

    fun parseVoiceCommand(command: String): ParsedCatch? {
        val lower = command.lowercase()

        // ✅ Extract weight
        val weightRegex = Regex("""(\d+)\s*(pounds|lbs|lb)\s*(\d+)?\s*(ounces|oz)?""")
        val weightMatch = weightRegex.find(lower)

        val weightLbs = weightMatch?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        val weightOz = weightMatch?.groups?.get(3)?.value?.toIntOrNull() ?: 0

        // ✅ Handle short versions like "smallmouth", "largemouth"
        val knownSpecies = listOf(
            "largemouth bass", "smallmouth bass", "largemouth", "smallmouth",
            "walleye", "perch", "crappie", "pike", "catfish", "panfish"
        )

        val speciesFound = knownSpecies.firstOrNull { lower.contains(it) }

        val normalizedSpecies = when (speciesFound) {
            "largemouth" -> "Large Mouth"
            "largemouth bass" -> "Large Mouth"
            "smallmouth" -> "Small Mouth"
            "smallmouth bass" -> "Small Mouth"
            else -> speciesFound?.replaceFirstChar { it.uppercase() } ?: ""
        }

        // ✅ Extract clip color
        val clipColorRegex = Regex("""on (the )?(\w+)\s*(clip)?""")
        val colorMatch = clipColorRegex.find(lower)
        val rawColor = colorMatch?.groups?.get(2)?.value ?: ""
        val clipColor = rawColor.replaceFirstChar { it.uppercase() }

        return if (normalizedSpecies.isNotBlank() && (weightLbs > 0 || weightOz > 0)) {
            ParsedCatch(
                species = normalizedSpecies,
                weightLbs = weightLbs,
                weightOz = weightOz,
                clipColor = clipColor
            )
        } else {
            null
        }
    }
}
