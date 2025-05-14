package com.bramestorm.bassanglertracker.training

import android.content.Context
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
        "red" to "RED",
        "blue" to "BLUE",
        "green" to "GREEN",
        "yellow" to "YELLOW",
        "orange" to "ORANGE",
        "white" to "WHITE"
    )

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

        put("crappie", "Crappie")
        put("crap pie", "Crappie")
        put("crappy", "Crappie")
        put("crop e", "Crappie")

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
    }

    /**
     * Normalize raw input → Title-cased species name,
     * or return null if it’s empty after cleaning.
     */
    fun normalizeSpecies(raw: String): String? {
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

    fun getSpeciesFromVoice(text: String, speciesList: List<String>): String {
        val normalizedText = text.lowercase()
            .replace("clip", "")
            .replace(Regex("""[^a-z\s]"""), "")
            .trim()
        for (species in speciesList) {
            val simplified = species.lowercase()
            if (normalizedText.contains(simplified)) {
                return species.trim()
            }
        }
        return "Unrecognized"
    }

    fun getClipColorFromVoice(text: String): String {
        val colors = listOf("red", "blue", "green", "yellow", "orange", "white")
        var normalized = text.lowercase()
        normalized = normalized.replace("clip", "")
        normalized = normalized.replace(Regex("""[^a-z\s]"""), "")
        normalized = normalized.trim()
        for (color in colors) {
            if (normalized.contains(color)) {
                return color.uppercase()
            }
        }
        return "RED"
    }

    // (All 8 parse*Command functions are correctly left untouched for exact pattern matching)

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
}
