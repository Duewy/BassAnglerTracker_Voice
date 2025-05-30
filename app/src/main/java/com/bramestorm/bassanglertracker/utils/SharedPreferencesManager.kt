package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.bramestorm.bassanglertracker.MeasurementMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

object SharedPreferencesManager {

    // Preferences files
    private const val SPECIES_PREFS = "SpeciesPrefs"
    private const val APP_PREFS = "BassAnglerTrackerPrefs"

    // Species keys
    private const val KEY_SELECTED_SPECIES_LIST = "SELECTED_SPECIES_LIST"
    private const val KEY_ALL_SPECIES_LIST = "ALL_SPECIES_LIST"

    // Catch entry type
    private const val KEY_CATCH_TYPE = "catchEntryType"

    // Voice & tournament keys
    private const val KEY_VOICE_CONTROL_ENABLED = "VOICE_CONTROL_ENABLED"
    private const val KEY_NUMBER_OF_CATCHES = "NUMBER_OF_CATCHES"
    private const val KEY_TOURNAMENT_SPECIES = "TOURNAMENT_SPECIES"
    private const val KEY_CULLING_ENABLED = "CULLING_ENABLED"

    private const val TAG = "SharedPreferencesManager"

    /**
     * Return the singleton instance (nothing to construct).
     */
    fun getInstance(context: Intent): SharedPreferencesManager = this

    // --- CatchEntryType ---
    fun saveCatchEntryType(context: Context, type: Int) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CATCH_TYPE, type).apply()
    }

    fun getCatchEntryType(context: Context): Int {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CATCH_TYPE, -1) // Value is from StartUp btnStartFishing selection 1-4 Fun Day, 5-8 Tournament default 0
    }

    // --- Voice Control ---
    fun setVccEnabled(context: Context, enabled: Boolean) {
           context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
           .edit().putBoolean(KEY_VOICE_CONTROL_ENABLED, enabled).apply()
    }

    // --- Tournament Settings ---
    fun setNumberOfCatches(context: Context, number: Int) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_NUMBER_OF_CATCHES, number).apply()
    }

    fun getNumberOfCatches(context: Context): Int {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_NUMBER_OF_CATCHES, 5)
    }

    fun getTournamentUnit(context: Context): MeasurementMode {
        val unit = context.getSharedPreferences("catch_and_call_prefs", Context.MODE_PRIVATE)
            .getString("unitType", "lbs") ?: "lbs"
        return when (unit.lowercase(Locale.ROOT)) {
            "lbs", "pounds", "weight" -> MeasurementMode.LBS_OZ
            "kg", "kgs" -> MeasurementMode.KG
            "inches" -> MeasurementMode.INCHES
            "cm", "centimeters" -> MeasurementMode.CM
            else -> MeasurementMode.LBS_OZ
        }
    }

    fun getFunDayUnit(context: Context): MeasurementMode {
        val type = getCatchEntryType(context)
        return when (type) {
            1 -> MeasurementMode.LBS_OZ
            2 -> MeasurementMode.KG
            3 -> MeasurementMode.INCHES
            4 -> MeasurementMode.CM
            else -> MeasurementMode.LBS_OZ  // default fallback
        }
    }


    fun setTournamentSpecies(context: Context, species: String) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOURNAMENT_SPECIES, species).apply()
    }

    fun getTournamentSpecies(context: Context): String? {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOURNAMENT_SPECIES, null)
    }

    fun loadAllSpecies(context: Context): List<String> =
        getAllSpecies(context)

    fun setCullingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CULLING_ENABLED, enabled).apply()
    }

    fun isCullingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CULLING_ENABLED, false)
    }

    // --- Species Management ---
    fun initializeDefaultSpeciesIfNeeded(context: Context) {
        val prefs = getSpeciesPrefs(context)
        val gson = Gson()

        if (!prefs.contains(KEY_ALL_SPECIES_LIST)) {
            val defaultSpecies = FishSpecies.allSpeciesList
            prefs.edit().putString(KEY_ALL_SPECIES_LIST, gson.toJson(defaultSpecies)).apply()
            prefs.edit().putString(KEY_SELECTED_SPECIES_LIST, gson.toJson(defaultSpecies.take(8)))
                .apply()
            Log.d("SharedPrefsInit", "Initialized species lists.")
        }
    }

    fun resetToDefaultSpecies(context: Context) {
        val defaultSpecies = FishSpecies.allSpeciesList
        val gson = Gson()
        val prefsEditor = getSpeciesPrefs(context).edit()
        prefsEditor.putString(KEY_ALL_SPECIES_LIST, gson.toJson(defaultSpecies))
        prefsEditor.putString(KEY_SELECTED_SPECIES_LIST, gson.toJson(defaultSpecies.take(8)))
        prefsEditor.apply()
        Log.d(TAG, "Reset species to default.")
    }

    fun removeUserSpecies(context: Context, speciesName: String) {
        val normalized = normalizeSpeciesName(speciesName)
        val all = getAllSavedSpecies(context).toMutableList()
        all.removeAll { normalizeSpeciesName(it) == normalized }
        saveAllSpecies(context, all)

        val selected = getSelectedSpeciesList(context).toMutableList()
        selected.removeAll { normalizeSpeciesName(it) == normalized }
        saveSelectedSpeciesList(context, selected)
        Log.d(TAG, "Removed species: $speciesName")
    }

    private fun getAllSavedSpecies(context: Context): List<String> {
        val json = getSpeciesPrefs(context).getString(KEY_ALL_SPECIES_LIST, null)
        return if (json != null) Gson().fromJson(
            json,
            object : TypeToken<List<String>>() {}.type
        ) else emptyList()
    }

    fun updateUserSpeciesName(context: Context, oldName: String, newName: String) {
        val normalizedOld = normalizeSpeciesName(oldName)
        val normalizedNew = normalizeSpeciesName(newName)
        if (normalizedNew.isBlank()) return

        val all = getAllSavedSpecies(context).toMutableList()
        val idxAll = all.indexOfFirst { normalizeSpeciesName(it) == normalizedOld }
        if (idxAll >= 0) all[idxAll] = newName
        saveAllSpecies(context, all)

        val sel = getSelectedSpeciesList(context).toMutableList()
        val idxSel = sel.indexOfFirst { normalizeSpeciesName(it) == normalizedOld }
        if (idxSel >= 0) sel[idxSel] = newName
        saveSelectedSpeciesList(context, sel)

        Log.d(TAG, "Updated species from '$oldName' to '$newName'.")
    }

    fun getMasterSpeciesList(context: Context): List<String> = getAllSavedSpecies(context)

    fun getSelectedSpeciesList(context: Context): List<String> {
        val json = getSpeciesPrefs(context).getString(KEY_SELECTED_SPECIES_LIST, null)
        return if (json != null) Gson().fromJson(
            json,
            object : TypeToken<List<String>>() {}.type
        ) else emptyList()
    }

    fun saveSelectedSpeciesList(context: Context, speciesList: List<String>) {
        val limited = speciesList.take(8).map { normalizeSpeciesName(it) }
        getSpeciesPrefs(context).edit().putString(KEY_SELECTED_SPECIES_LIST, Gson().toJson(limited))
            .apply()
        Log.d(TAG, "Saved selected species: $limited")
    }

    fun saveAllSpecies(context: Context, speciesList: List<String>) {
        getSpeciesPrefs(context).edit().putString(KEY_ALL_SPECIES_LIST, Gson().toJson(speciesList))
            .apply()
        Log.d(TAG, "Saved all species: $speciesList")
    }

    fun getUserAddedSpeciesList(context: Context): List<String> {
        val saved = getAllSavedSpecies(context).map { normalizeSpeciesName(it) }
        val default = FishSpecies.allSpeciesList.map { normalizeSpeciesName(it) }
        return saved.filterNot { it in default }
    }

    fun getAllSpecies(context: Context): List<String> {
        val default = FishSpecies.allSpeciesList.map { normalizeSpeciesName(it) }
        return (default + getUserAddedSpeciesList(context)).distinct()
    }

    private fun getSpeciesPrefs(context: Context) =
        context.getSharedPreferences(SPECIES_PREFS, Context.MODE_PRIVATE)

    fun isSpeciesInitialized(context: Context): Boolean {
        return getSpeciesPrefs(context).getBoolean("SPECIES_INITIALIZED", false)
    }

    fun setSpeciesInitialized(context: Context, initialized: Boolean) {
        getSpeciesPrefs(context).edit().putBoolean("SPECIES_INITIALIZED", initialized).apply()
    }

    fun normalizeSpeciesName(name: String): String =
        name.trim().lowercase().replace(Regex("\\s+"), " ")


}//------------- END -------------------------------------------
