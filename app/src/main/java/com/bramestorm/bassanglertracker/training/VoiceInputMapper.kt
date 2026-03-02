package com.bramestorm.bassanglertracker.training

//TODO
    /*Light refactor later (not urgent)
    Split into:
    VoiceSpeciesResolver
    VoiceColorResolver
    VoiceGrammarCleaner  */

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

    private val baseColorMap = mapOf(
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
    //  ── FIX: Output names NOW match normalizeSpeciesName() + SpeciesImageHelper ──
    //  All output values are the EXACT lowercase form that the rest of the app expects.
    //  normalizeSpeciesName() does .trim().lowercase(), so these must match that output.
    val baseSpeciesVoiceMap = mutableMapOf<String, String>().apply {
        put("clear list", "Clear List")
        put("clearlist", "Clear List")
        put("save the catch", "Save Catch")
        put("new fish", "New Fish")
        put("caught", "Caught")
        put("log entry", "Log Entry")

        // ── "large mouth" — SpeciesImageHelper + initials map both use "large mouth" ──
        put("largemouth", "large mouth")
        put("large mouth", "large mouth")
        put("lard mouth", "large mouth")
        put("large moth", "large mouth")

        // ── "small mouth" — already correct ──
        put("smallmouth", "small mouth")
        put("small mouth", "small mouth")
        put("smile mouth", "small mouth")

        // ── "spotted bass" — already correct ──
        put("spotted bass", "spotted bass")
        put("spot", "spotted bass")
        put("spottedbass", "spotted bass")
        put("spot bass", "spotted bass")

        // ── "crappie" ──
        put("crappie", "crappie")
        put("crap pie", "crappie")
        put("crappy", "crappie")
        put("crop e", "crappie")
        put("crop i", "crappie")

        // ── "sunfish" ──
        put("sunfish", "sunfish")
        put("sun fish", "sunfish")
        put("some fish", "sunfish")

        // ── "white bass" ──
        put("white bass", "white bass")
        put("why bass", "white bass")
        put("wide bass", "white bass")

        // ── "rock bass" ──
        put("rock bass", "rock bass")
        put("rack bass", "rock bass")
        put("rug bass", "rock bass")

        // ── "bowfin" ──
        put("bowfin", "bowfin")
        put("bow fin", "bowfin")
        put("bovine", "bowfin")

        // ── "muskie" ──
        put("muskie", "muskie")
        put("musky", "muskie")
        put("musky fish", "muskie")
        put("muskellunge", "muskie")

        // ── "walleye" ──
        put("walleye", "walleye")
        put("wall eye", "walleye")
        put("wallie", "walleye")
        put("while I", "walleye")

        // ── "pike" ──
        put("pike", "pike")
        put("northern pike", "pike")

        // ── "perch" ──
        put("perch", "perch")
        put("purse", "perch")

        // ── "catfish" — SpeciesImageHelper expects "catfish" (no space!) ──
        put("catfish", "catfish")
        put("cat fish", "catfish")

        // ── "gar" — SpeciesImageHelper uses "gar", not "gar pike" ──
        put("gar pike", "gar")
        put("gor pike", "gar")
        put("guard pike", "gar")
        put("gar", "gar")

        // ── "bull head" — SpeciesImageHelper uses "bull head" ──
        put("bullhead", "bull head")
        put("bull head", "bull head")
        put("bald head", "bull head")

        // ── "red drum" ──
        put("red drum", "red drum")
        put("redrum", "red drum")
        put("red fish", "red drum")

        // ── "carp" ──
        put("carp", "carp")
        put("cart", "carp")

        // ── "panfish" ──
        put("panfish", "panfish")
        put("pan fish", "panfish")

    }// ========== END of base Species Voice Map =========================

    /**
     * Normalize raw input → Title-Cased species name,
     * or return null if it’s empty after cleaning.
     */
    private fun normalizeSpecies(raw: String): String? {
        val result = raw.trim().lowercase()
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
        return baseSpeciesVoiceMap[raw.trim().lowercase()] ?: raw.trim().lowercase()
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
