package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bramestorm.bassanglertracker.training.VoiceInputMapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object SharedPreferencesManager {

    private const val PREFS_NAME = "SpeciesPrefs"
    private const val KEY_SELECTED_SPECIES_LIST = "SELECTED_SPECIES_LIST"
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
            prefs.edit().putString(KEY_SELECTED_SPECIES_LIST, jsonSelected).apply()

            Log.d("SharedPrefsInit", "Initialized master list and selected species.")
        }
    }

    fun resetToDefaultSpecies(context: Context) {
        val defaultSpecies = FishSpecies.allSpeciesList
        val jsonAll = Gson().toJson(defaultSpecies)
        val jsonSelected = Gson().toJson(defaultSpecies.take(8))

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putString(KEY_ALL_SPECIES, jsonAll)
        prefs.putString(KEY_SELECTED_SPECIES_LIST, jsonSelected)
        prefs.apply()

        Log.d(TAG, "Reset species to default.")
    }

    fun removeUserSpecies(context: Context, speciesName: String) {
        val normalized = normalizeSpeciesName(speciesName)

        val currentAll = getAllSavedSpecies(context).toMutableList()
        val updatedAll = currentAll.filterNot { normalizeSpeciesName(it) == normalized }
        saveAllSpecies(context, updatedAll)

        // Also remove from selected species if it was selected
        val currentSelected = getSelectedSpeciesList(context).toMutableList()
        val updatedSelected = currentSelected.filterNot { normalizeSpeciesName(it) == normalized }
        saveSelectedSpeciesList(context, updatedSelected)

        Log.d(TAG, "Removed species: $speciesName")
    }


   private fun getAllSavedSpecies(context: Context): List<String> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_ALL_SPECIES, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }


    fun updateUserSpeciesName(context: Context, oldName: String, newName: String) {
        val normalizedOld = normalizeSpeciesName(oldName)
        val normalizedNew = normalizeSpeciesName(newName)

        if (normalizedNew.isEmpty()) {
            Log.w(TAG, "New name is empty. Update aborted.")
            Toast.makeText(context, "Species name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val allSpecies = getAllSavedSpecies(context)
        val nameExists = allSpecies.any {
            normalizeSpeciesName(it) == normalizedNew && normalizeSpeciesName(it) != normalizedOld
        }

        if (nameExists) {
            Log.w(TAG, "Species name '$newName' already exists. Update aborted.")
            Toast.makeText(context, "A species with this name already exists.", Toast.LENGTH_SHORT).show()
            return
        }

        // Update in ALL species
        val updatedAll = allSpecies.toMutableList()
        val indexInAll = updatedAll.indexOfFirst { normalizeSpeciesName(it) == normalizedOld }
        if (indexInAll != -1) {
            updatedAll[indexInAll] = newName
            saveAllSpecies(context, updatedAll)
        }

        // Update in SELECTED species
        val selectedSpecies = getSelectedSpeciesList(context).toMutableList()
        val indexInSelected = selectedSpecies.indexOfFirst { normalizeSpeciesName(it) == normalizedOld }
        if (indexInSelected != -1) {
            selectedSpecies[indexInSelected] = newName
            saveSelectedSpeciesList(context, selectedSpecies)
        }

        Log.d(TAG, "Updated species from '$oldName' to '$newName'")
    }



    fun getMasterSpeciesList(context: Context): List<String> {
        return getAllSavedSpecies(context)
    }

    fun getSelectedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SELECTED_SPECIES_LIST, null)

        Log.d("SpeciesLoad", "Loaded selected species list: $json")

        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveSelectedSpeciesList(context: Context, speciesList: List<String>) {
        val limited = speciesList.take(8).map { normalizeSpeciesName(it) }
        val json = Gson().toJson(limited)

        Log.d("SpeciesSave", "Saving normalized species list: $limited")

        getPrefs(context).edit().putString(KEY_SELECTED_SPECIES_LIST, json).apply()
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


    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    fun isSpeciesInitialized(context: Context): Boolean {
        return getPrefs(context).getBoolean("SPECIES_INITIALIZED", false)
    }

    fun setSpeciesInitialized(context: Context, initialized: Boolean) {
        getPrefs(context).edit().putBoolean("SPECIES_INITIALIZED", initialized).apply()
    }

    fun normalizeSpeciesName(name: String): String {
        return name.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    fun loadSelectedSpecies(context: Context): List<String> {
        val prefs = context.getSharedPreferences("SpeciesPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("selectedSpeciesList", null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun loadAllSpecies(context: Context): List<String> {
        val prefs = context.getSharedPreferences("SpeciesPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("allSpeciesList", null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }


    fun isVoiceControlEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("VOICE_CONTROL_ENABLED", false)
    }

    // For the Vcc Pop ups to validate the available clip colors the user can call
    fun validateClipColorFromVoice(cleaned: String, availableColors: Array<String>): String? {
        val spoken = VoiceInputMapper.getClipColorFromVoice(cleaned).uppercase()
        return if (availableColors.contains(spoken)) spoken else null
    }



}//------------- END -------------------------------------------
