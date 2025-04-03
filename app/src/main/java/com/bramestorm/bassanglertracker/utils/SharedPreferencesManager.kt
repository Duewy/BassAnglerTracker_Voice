package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.util.Log
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object SharedPreferencesManager {

    private const val PREFS_NAME = "SpeciesPrefs"
    private const val KEY_SELECTED_SPECIES = "SELECTED_SPECIES_LIST"
    private const val KEY_ALL_SPECIES = "ALL_SPECIES_LIST"
    private const val TAG = "SharedPreferencesManager"


    fun initializeDefaultSpeciesIfNeeded(context: Context) {
        val prefs = getPrefs(context)
        val gson = Gson()

        if (!prefs.contains(KEY_ALL_SPECIES)) {
            val defaultSpecies = FishSpecies.allSpeciesList
            val jsonAll = gson.toJson(defaultSpecies)
            prefs.edit().putString(KEY_ALL_SPECIES, jsonAll).apply()

            val jsonSelected = gson.toJson(defaultSpecies.take(8))
            prefs.edit().putString(KEY_SELECTED_SPECIES, jsonSelected).apply()

            Log.d("SharedPrefsInit", "Initialized master list and selected species.")
        }
    }


    fun getSafeSpeciesList(context: Context): List<String> {
        val selected = SharedPreferencesManager.getSelectedSpeciesList(context)
        return if (selected.isNotEmpty()) selected else SharedPreferencesManager.getAllSpecies(context)
    }

    fun getMasterSpeciesList(context: Context): List<String> {
        return getAllSavedSpecies(context)
    }


    fun setMasterSpeciesList(context: Context, speciesList: List<String>) {
        saveAllSpecies(context, speciesList)
    }


    fun getSelectedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SELECTED_SPECIES, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveSelectedSpeciesList(context: Context, selected: List<String>) {
        val normalized = selected.map { normalizeSpeciesName(it) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val json = Gson().toJson(normalized)
        prefs.putString(KEY_SELECTED_SPECIES, json)
        prefs.apply()
        Log.d(TAG, "Saved selected species: $normalized")
    }



    fun getAllSavedSpecies(context: Context): List<String> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_ALL_SPECIES, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveAllSpecies(context: Context, allSpecies: List<String>) {
        val prefs = getPrefs(context).edit()
        val json = Gson().toJson(allSpecies)
        prefs.putString(KEY_ALL_SPECIES, json)
        prefs.apply()
        Log.d(TAG, "Saved all species list: $allSpecies")
    }



    fun getUserAddedSpeciesList(context: Context): List<String> {
        val saved = getAllSavedSpecies(context).map { normalizeSpeciesName(it) }
        val defaultSpecies = listOf("Largemouth", "Smallmouth", "Crappie", "Walleye", "Catfish", "Perch", "Pike", "Bluegill")
            .map { normalizeSpeciesName(it) }

        return saved.filterNot { it in defaultSpecies }
    }


    fun getAllSpecies(context: Context): List<String> {
        val defaultSpecies = FishSpecies.allSpeciesList.map { normalizeSpeciesName(it) }
        val userAdded = getUserAddedSpeciesList(context)
        return (defaultSpecies + userAdded).distinct()
    }



    fun clearSelectedSpecies(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SELECTED_SPECIES).remove(KEY_ALL_SPECIES).apply()
    }

    fun clearSpeciesPreferences(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SELECTED_SPECIES).remove(KEY_ALL_SPECIES).apply()
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrderedSelectedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SELECTED_SPECIES, null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } else {
            // Fallback to all or default
            getAllSavedSpecies(context).take(8)
        }
    }

    fun isSpeciesInitialized(context: Context): Boolean {
        return getPrefs(context).getBoolean("SPECIES_INITIALIZED", false)
    }

    fun setSpeciesInitialized(context: Context, initialized: Boolean) {
        getPrefs(context).edit().putBoolean("SPECIES_INITIALIZED", initialized).apply()
    }


}
