package com.bramestorm.bassanglertracker.training
// Adjusting for Language and Accent Issues
// A Listing of Acceptable Words and or Phrases from the User


object VoiceInputMapper {

    private val speciesVoiceAliases = mapOf(
        "Large Mouth" to listOf("large mouth", "largemouth", "lard mouth", "large moth"),
        "Small Mouth" to listOf("small mouth", "smallmouth", "smile mouth"),
        "Crappie" to listOf("crappie", "crap pie", "crappy", "crop e"),
        "Sunfish" to listOf("sunfish", "sun fish", "some fish"),
        "White Bass" to listOf("white bass", "why bass", "wide bass"),
        "Rock Bass" to listOf("rock bass", "rack bass", "rug bass"),
        "Bowfin" to listOf("bowfin", "bow fin", "bovine"),
        "Muskie" to listOf("muskie", "musky", "musky fish"),
        "Gar" to listOf("gar", "car", "guard"),
        "Bullhead" to listOf("bullhead", "bull head", "bald head"),
        "Red Drum" to listOf("red drum", "redrum", "red fish"),
        "Carp" to listOf("carp", "cart", "cap")
        // Add more mappings as needed
    )

    private val voiceToSpecies = speciesVoiceAliases
        .flatMap { (canonical, aliases) -> aliases.map { it.lowercase() to canonical } }
        .toMap()

    fun getSpeciesFromVoice(input: String): String? {
        return voiceToSpecies[input.trim().lowercase()]
    }
}
