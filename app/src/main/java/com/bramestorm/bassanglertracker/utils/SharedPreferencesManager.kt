package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.util.Log
import com.bramestorm.bassanglertracker.MeasurementMode
import org.json.JSONArray
import org.json.JSONObject

object SharedPreferencesManager {

    // Preferences files
    private const val KEY_VCC_DOZE_AGREEMENT = "USER_AGREED_TO_VCC_DOZE"
    private const val SPECIES_PREFS = "SpeciesPrefs"
    private const val APP_PREFS = "BassAnglerTrackerPrefs"
    private const val PREFS_NAME = "bass_angler_prefs"

    // Species keys
    private const val KEY_SPECIES_LIST = "species_list_v2"

    private const val KEY_SPECIES_IMAGE_URIS = "species_image_uris"
    private const val KEY_SPECIES_INITIALS_MAP = "species_initials_map"


    // Catch entry type
    private const val KEY_CATCH_TYPE = "catchEntryType"

    // Voice & tournament keys
    private const val KEY_VOICE_CONTROL_ENABLED = "VOICE_CONTROL_ENABLED"
    private const val KEY_NUMBER_OF_CATCHES = "NUMBER_OF_CATCHES"
    private const val KEY_TOURNAMENT_SPECIES = "TOURNAMENT_SPECIES"
    private const val KEY_CULLING_ENABLED = "CULLING_ENABLED"

    private const val TAG = "SharedPreferencesManager"

    // === VOICE CONTROL ===
    fun setVccEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VOICE_CONTROL_ENABLED, enabled).apply()
    }

    fun isVccEnabled(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VOICE_CONTROL_ENABLED, false)
    }

    fun setUserAgreedToDeepDoze(context: Context, agreed: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VCC_DOZE_AGREEMENT, agreed).apply()
    }

    fun hasUserAgreedToDeepDoze(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VCC_DOZE_AGREEMENT, false)
    }

    // === CatchEntry Type ===
    fun saveCatchEntryType(context: Context, type: Int) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CATCH_TYPE, type).apply()
    }

    fun getCatchEntryType(context: Context): Int {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CATCH_TYPE, -1)
    }

  //=============== MEASUREMENT MODE #'s ===========================
    fun getFunDayUnit(context: Context): MeasurementMode {
        val type = getCatchEntryType(context)
        return when (type) {
            1 -> MeasurementMode.LBS_OZ
            2 -> MeasurementMode.POUNDS
            3 -> MeasurementMode.KG
            4 -> MeasurementMode.INCHES
            5 -> MeasurementMode.CM

            else -> MeasurementMode.LBS_OZ
        }
    }

    fun getTournamentUnit(context: Context): MeasurementMode {
        val type = getCatchEntryType(context)
        return when (type) {
            6   -> MeasurementMode.LBS_OZ
            7   -> MeasurementMode.POUNDS
            8   -> MeasurementMode.KG
            9   -> MeasurementMode.INCHES
            10  -> MeasurementMode.CM
            else -> MeasurementMode.LBS_OZ
        }
    }


    // === Tournament Settings 🏆 ===
    fun setNumberOfCatches(context: Context, number: Int) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_NUMBER_OF_CATCHES, number).apply()
    }

    fun getNumberOfCatches(context: Context): Int {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_NUMBER_OF_CATCHES, 5)
    }

    fun setTournamentSpecies(context: Context, species: String) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOURNAMENT_SPECIES, species).apply()
    }

    fun getTournamentSpecies(context: Context): String? {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOURNAMENT_SPECIES, null)
    }

    fun setCullingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CULLING_ENABLED, enabled).apply()
    }

    fun isCullingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CULLING_ENABLED, false)
    }


    // === Species Handling SYSTEM ===
    // -------- There is a set list on the FishSpecies that has Icons from SpeciesImageHelper -------

// =============================================================
// SPECIES – SINGLE SOURCE OF TRUTH
// =============================================================

    fun loadSpeciesList(context: Context): MutableList<String> {
        val prefs = prefs(context)

        val json = prefs.getString(KEY_SPECIES_LIST, null)

        if (!json.isNullOrBlank()) {
            return try {
                val array = JSONArray(json)
                MutableList(array.length()) { i ->
                    normalizeSpeciesName(array.getString(i))
                }
            } catch (e: Exception) {
                mutableListOf()
            }
        }

        // ---- FIRST RUN / MIGRATION ----
        val defaults = FishSpecies.allSpeciesList
            .map { normalizeSpeciesName(it) }
            .toMutableList()

        saveSpeciesList(context, defaults)
        return defaults
    }

    fun saveSpeciesList(context: Context, list: List<String>) {
        val cleaned = list
            .map { normalizeSpeciesName(it) }
            .filter { it.isNotBlank() }
            .distinct()

        val json = JSONArray(cleaned).toString()

        prefs(context).edit()
            .putString(KEY_SPECIES_LIST, json)
            .apply()
    }

    fun addSpecies(context: Context, species: String) {
        val list = loadSpeciesList(context)
        val normalized = normalizeSpeciesName(species)

        if (!list.contains(normalized)) {
            list.add(normalized)
            saveSpeciesList(context, list)
        }
    }

    fun removeSpecies(context: Context, species: String) {
        val list = loadSpeciesList(context)
        list.remove(normalizeSpeciesName(species))
        saveSpeciesList(context, list)
    }



    private fun getSpeciesPrefs(context: Context) =
        context.getSharedPreferences(SPECIES_PREFS, Context.MODE_PRIVATE)

    fun isSpeciesInitialized(context: Context): Boolean {
        return getSpeciesPrefs(context).getBoolean("SPECIES_INITIALIZED", false)
    }

    fun setSpeciesInitialized(context: Context, initialized: Boolean) {
        getSpeciesPrefs(context).edit().putBoolean("SPECIES_INITIALIZED", initialized).apply()
    }
    // =============================================================
    // HELPERS
    // =============================================================
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun normalizeSpeciesName(name: String): String =
        name.trim().lowercase()

    /**
     * Canonical form for duplicate detection.
     * Used ONLY for comparison, not storage.
     *
     * Examples:
     *  - "large-mouth"  -> "large mouth"
     *  - "largemouth"   -> "largemouth"
     *  - "large   mouth"-> "large mouth"
     */
    fun canonicalizeSpeciesName(name: String): String =
        name
            .trim()
            .lowercase()
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")

    // =============================================================
    // ALL SPECIES (CATALOGUE)
    // =============================================================



    fun saveSpeciesImageUri(context: Context, speciesName: String, uri: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SPECIES_IMAGE_URIS, "{}")
        val obj = JSONObject(json ?: "{}")

        if (uri == null) {
            obj.remove(speciesName)
        } else {
            obj.put(speciesName, uri)
        }

        prefs.edit()
            .putString(KEY_SPECIES_IMAGE_URIS, obj.toString())
            .apply()
    }

    fun getSpeciesImageUri(context: Context, speciesName: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SPECIES_IMAGE_URIS, null) ?: return null

        val value = JSONObject(json).optString(speciesName, "")
        return value.ifBlank { null }
    }


    fun loadSpeciesInitialsMap(context: Context): MutableMap<String, String> {
        val prefs = prefs(context)
        val json = prefs.getString(KEY_SPECIES_INITIALS_MAP, null) ?: return mutableMapOf()
        val obj = JSONObject(json)
        return obj.keys().asSequence().associateWith { obj.getString(it) }.toMutableMap()
    }

    fun saveSpeciesInitialsMap(context: Context, map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs(context).edit().putString(KEY_SPECIES_INITIALS_MAP, obj.toString()).apply()
    }

    private fun generateBaseInitial(name: String): String {
        val parts = name.split(" ").filter { it.isNotBlank() }
        return if (parts.size >= 2) {
            "${parts[0][0]}${parts[1][0]}".uppercase()
        } else {
            name.take(2).uppercase()
        }
    }

    fun assignUniqueSpeciesInitial(
        context: Context,
        speciesName: String
    ): String {
        val normalized = normalizeSpeciesName(speciesName)
        val map = loadSpeciesInitialsMap(context)
        val used = map.values.toSet()

        val base = generateBaseInitial(normalized)
        if (base !in used) return base

        // Try letter variations from the name
        val letters = normalized.replace(" ", "").uppercase()
        for (i in 1 until letters.length) {
            val candidate = "${letters[0]}${letters[i]}"
            if (candidate !in used) return candidate
        }

        // Absolute fallback (rare)
        var suffix = 'A'
        while ("${base[0]}$suffix" in used) suffix++
        return "${base[0]}$suffix"
    }

    fun ensureDefaultSpeciesInitials(context: Context) {
        val map = loadSpeciesInitialsMap(context)
        if (map.isNotEmpty()) return

        map.putAll(FishSpecies.defaultSpeciesInitials)
        saveSpeciesInitialsMap(context, map)
    }


// ----- Species Initials -------

    fun getSpeciesInitial(context: Context, species: String): String {
        ensureDefaultSpeciesInitials(context)

        val normalized = normalizeSpeciesName(species)
        val map = loadSpeciesInitialsMap(context)

        return map[normalized] ?: "--"
    }

                           /* fun getSpeciesInitials(normalizedSpecies: String): String {
                            val s = normalizedSpecies.trim().lowercase(Locale.US)

                            // ✅ Explicit tournament overrides for Spotted Bass
                            if (s == "spotted bass" || s == "spotted") {
                                return "SP"
                            }

                            val words = s.split(" ").filter { it.isNotBlank() }

                            return when {
                                // Two-word (or more) species → first letters
                                words.size >= 2 -> {
                                    "${words[0][0]}${words[1][0]}".uppercase(Locale.US)
                                }

                                // Single-word species → first two consonants
                                words.size == 1 -> {
                                    val consonants = words[0]
                                        .uppercase(Locale.US)
                                        .filter { it !in "AEIOU" }

                                    when {
                                        consonants.length >= 2 -> consonants.substring(0, 2)
                                        consonants.length == 1 -> "${consonants[0]}${words[0][0].uppercaseChar()}"
                                        else -> words[0].take(2).uppercase(Locale.US)
                                    }
                                }

                                else -> "--"
                            }
                        }  */



    //==== ADVERTISEMENT SECTION saveSpeciesCatalogue 📰 ======================

    fun logAdCloseTime(context: Context, adSource: String, durationMs: Long) {
        val prefs = context.getSharedPreferences("AdStatsPrefs", Context.MODE_PRIVATE)
        val key = "duration_${adSource}_${System.currentTimeMillis()}"
        prefs.edit().putLong(key, durationMs).apply()
        Log.d("AdTracker", "⏱️ $adSource ad closed after ${durationMs / 1000.0} sec")
    }

    fun logAdImpression(context: Context, adSource: String) {
        val prefs = context.getSharedPreferences("AdStatsPrefs", Context.MODE_PRIVATE)
        val key = "impressions_$adSource"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        Log.d("AdTracker", "📊 Impression logged for $adSource → Total: ${count + 1}")
    }

    fun getAdImpressionCount(context: Context, adSource: String): Int {
        val prefs = context.getSharedPreferences("AdStatsPrefs", Context.MODE_PRIVATE)
        return prefs.getInt("impressions_$adSource", 0)
    }


}//===== END =======
