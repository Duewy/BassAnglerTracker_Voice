package com.bramestorm.bassanglertracker.training

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object VoiceInputMapper {

    data class CatchData(
        val species: String = "",
        val pounds: Int = -1,
        val ounces: Int = -1,
        val kilograms: Double = -1.0,
        val inches: Int = -1,
        val eighths: Int = -1,
        val centimeters: Double = -1.0,
        val clipColor: String = ""
    )

    val userVoiceMap = mutableMapOf<String, String>()

    val baseColorMap = mapOf(
        "blu" to "BLUE",
        "blue" to "BLUE",
        "yellow" to "YELLOW",
        "yelo" to "YELLOW",
        "green" to "GREEN",
        "gren" to "GREEN",
        "orange" to "ORANGE",
        "white" to "WHITE",
        "wite" to "WHITE",
        "red" to "RED",
        "read" to "RED"
    )

    //  Ensure we set the ✔️🔊 correct wording for misspoken User Input or various accents
    val baseSpeciesVoiceMap = mutableMapOf<String, String>().apply {
        put("clear list", "Clear List")
        put("clearlist", "Clear List")
        put("save the catch", "Save Catch")
        put("new fish", "New Fish")
        put("caught", "Caught")
        put("log entry", "Log Entry")

        put("largemouth", "Largemouth")
        put("large mouth", "Largemouth")
        put("lard mouth", "Largemouth")
        put("large moth", "Largemouth")

        put("smallmouth", "Small Mouth")
        put("small mouth", "Small Mouth")
        put("smile mouth", "Small Mouth")

        put("spotted bass", "Spotted Bass")
        put("spot", "Spotted Bass")
        put("spottedbass", "Spotted Bass")
        put("spot bass", "Spotted Bass")

        put("crappie", "Crappie")
        put("crap pie", "Crappie")
        put("crappy", "Crappie")
        put("crop e", "Crappie")
        put("crop i", "Crappie")

        put("sunfish", "Sunfish")
        put("sun fish", "Sunfish")
        put("some fish", "Sunfish")

        put("white bass", "White Bass")
        put("why bass", "White Bass")
        put("wide bass", "White Bass")

        put("rock bass", "Rock Bass")
        put("rack bass", "Rock Bass")
        put("rug bass", "Rock Bass")

        put("bowfin", "Bowfin")
        put("bow fin", "Bowfin")
        put("bovine", "Bowfin")

        put("muskie", "Muskie")
        put("musky", "Muskie")
        put("musky fish", "Muskie")
        put("muskellunge", "Muskie")

        put("walleye", "Walleye")
        put("wall eye", "Walleye")
        put("wallie", "Walleye")
        put("while I", "Walleye")

        put("pike", "Pike")
        put("northern pike", "Pike")

        put("perch", "Perch")
        put("purse", "Perch")

        put("catfish", "Cat Fish")
        put("cat fish", "Cat Fish")

        put("gar pike", "Gar Pike")
        put("gor pike", "Gar Pike")
        put("guard pike", "Gar Pike")

        put("bullhead", "Bullhead")
        put("bull head", "Bullhead")
        put("bald head", "Bullhead")

        put("red drum", "Red Drum")
        put("redrum", "Red Drum")
        put("red fish", "Red Drum")

        put("carp", "Carp")
        put("cart", "Carp")

    }// ========== END of base Species Voice Map =========================

    /**
     * Normalize raw input → Title-Cased species name,
     * or return null if it’s empty after cleaning.
     */
    private fun normalizeSpecies(raw: String): String? {
        val words = raw
            .trim()
            .split(Regex("\\s+"))
            .map { token ->
                token
                    .lowercase()
                    .replaceFirstChar { it.uppercaseChar() }
            }

        val result = words.joinToString(" ")
        return result.ifBlank { null }
    }

    fun registerUserSpecies(name: String) {
        val cleaned = name.trim().lowercase()
        baseSpeciesVoiceMap[cleaned] = name
    }

        // From STT set up in proper Casing and adjust to proper Grammar 📃🖋️
    fun getSpeciesFromVoice(text: String, speciesList: List<String>): String {
        val normalizedText = text.lowercase()
            .replace("clip", "")
            .replace(Regex("""[^a-z\s]"""), "")
            .trim()

        for (species in speciesList) {
            val simplified = species.lowercase()
            if (normalizedText.contains(simplified)) {
                // 🔁 Clean and normalize before returning
                return unifySpeciesName(species.trim())
            }
            Log.d("VCC_getSpeciesFromVoice", "🐟 ParsedCatch from Voice: $simplified")

        }

        // 🧠 Try to resolve via baseSpeciesVoiceMap
        val resolved = baseSpeciesVoiceMap[normalizedText]
        return resolved ?: normalizeSpecies(normalizedText) ?: "Unknown"
    }

    private fun unifySpeciesName(raw: String): String {
        return baseSpeciesVoiceMap[raw.lowercase()] ?: normalizeSpecies(raw) ?: "Unknown"
    }

    fun getClipColorFromVoice(text: String, clipColors: List<String>): String {
        val cleaned = text.lowercase()
            .replace("clip", "")
            .replace(Regex("""[^a-z\s]"""), "")
            .trim()
        for ((alias, canonical) in baseColorMap) {
            if (cleaned.contains(alias) && clipColors.any { it.equals(canonical, ignoreCase = true) }) {
                return canonical
            }
        }
        return "RED" // default fallback
    }



    // (All 8 parse*Command functions are correctly left untouched for exact pattern matching)
    //todo work on the Voice Mapping which will enable the clean correct input from the User's Voice Commands

    fun saveUserVoiceMap(context: Context, voiceMap: Map<String, String>) {
        val prefs = context.getSharedPreferences("user_voice_map", Context.MODE_PRIVATE)
        val json = Gson().toJson(voiceMap)
        prefs.edit().putString("voice_map_json", json).apply()
    }

    fun loadUserVoiceMap(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences("user_voice_map", Context.MODE_PRIVATE)
        val json = prefs.getString("voice_map_json", "{}")
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(json, type)
    }

}//=================== END of Voice Input Mapper =======================
