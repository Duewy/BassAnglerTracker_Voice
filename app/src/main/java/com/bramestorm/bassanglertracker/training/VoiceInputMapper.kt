package com.bramestorm.bassanglertracker.training

/**
 * VoiceInputMapper handles known speech misfires and provides canonical names for commands and species.
 * This includes dynamic registration of new user-added species.
 */
object VoiceInputMapper {

    // Stores all voice-to-species mappings (static + dynamic)
    private val baseSpeciesVoiceMap = mutableMapOf<String, String>().apply {
        // PHRASES / COMMANDS
        put("clear list", "Clear List")
        put("clearlist", "Clear List")
        put("save the catch", "Save Catch")
        put("new fish", "New Fish")
        put("caught", "Caught")
        put("log entry", "Log Entry")

        // SPECIES: Largemouth
        put("largemouth", "Largemouth")
        put("large mouth", "Largemouth")
        put("lard mouth", "Largemouth")
        put("large moth", "Largemouth")

        // Smallmouth
        put("smallmouth", "Small Mouth")
        put("small mouth", "Small Mouth")
        put("smile mouth", "Small Mouth")

        // Crappie
        put("crappie", "Crappie")
        put("crap pie", "Crappie")
        put("crappy", "Crappie")
        put("crop e", "Crappie")

        // Sunfish
        put("sunfish", "Sunfish")
        put("sun fish", "Sunfish")
        put("some fish", "Sunfish")

        // White Bass
        put("white bass", "White Bass")
        put("why bass", "White Bass")
        put("wide bass", "White Bass")

        // Rock Bass
        put("rock bass", "Rock Bass")
        put("rack bass", "Rock Bass")
        put("rug bass", "Rock Bass")

        // Bowfin
        put("bowfin", "Bowfin")
        put("bow fin", "Bowfin")
        put("bovine", "Bowfin")

        // Muskie
        put("muskie", "Muskie")
        put("musky", "Muskie")
        put("musky fish", "Muskie")

        // Walleye
        put("walleye", "Walleye")
        put("wall eye", "Walleye")
        put("wallie", "Walleye")
        put("while I", "Walleye")

        // Pike
        put("pike", "Pike")
        put("northern pike", "Pike")

        // Perch
        put("perch", "Perch")
        put("purse", "Perch")

        // Catfish
        put("catfish", "Cat Fish")
        put("cat fish", "Cat Fish")

        // Gar Pike
        put("gar pike", "Gar Pike")
        put("gor pike", "Gar Pike")
        put("guard pike", "Gar Pike")

        // Bullhead
        put("bullhead", "Bullhead")
        put("bull head", "Bullhead")
        put("bald head", "Bullhead")

        // Red Drum
        put("red drum", "Red Drum")
        put("redrum", "Red Drum")
        put("red fish", "Red Drum")

        // Carp
        put("carp", "Carp")
        put("cart", "Carp")
    }

    /**
     * Register a new user-added species so it’s recognized by the voice system.
     */
    fun registerUserSpecies(name: String) {
        val cleaned = name.trim().lowercase()
        baseSpeciesVoiceMap[cleaned] = name
    }

    /**
     * Given some voice input (full or partial), return the matched canonical species or command.
     */
    fun getSpeciesFromVoice(input: String): String {
        val cleaned = input.trim().lowercase()
        return baseSpeciesVoiceMap.keys.firstOrNull { cleaned.contains(it) }?.let {
            baseSpeciesVoiceMap[it]!!
        } ?: "Unrecognized"
    }
}
