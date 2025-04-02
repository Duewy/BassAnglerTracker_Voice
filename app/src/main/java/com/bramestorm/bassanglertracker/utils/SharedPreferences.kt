package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPreferencesManager {
    private const val PREFS_NAME = "SpeciesPrefs"
    private const val KEY_SELECTED_SPECIES = "SELECTED_SPECIES_LIST"
    private const val KEY_ALL_SPECIES = "ALL_SPECIES_LIST"




    // Default species list
    private val defaultSpecies = listOf(
        "Largemouth", "Smallmouth", "Crappie", "Walleye",
        "Catfish", "Perch", "Pike", "Bluegill"
    )

    fun initializeDefaultSpeciesIfNeeded(context: Context) {
        val prefs = getPrefs(context)
        val gson = Gson()

        if (!prefs.contains(KEY_ALL_SPECIES)) {
            val defaultSpecies = listOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
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


    fun getSelectedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SELECTED_SPECIES, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveSelectedSpeciesList(context: Context, selected: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val json = Gson().toJson(selected)
        prefs.putString(KEY_SELECTED_SPECIES, json)
        prefs.apply()
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
    }



    fun getUserAddedSpeciesList(context: Context): List<String> {
        val saved = getAllSavedSpecies(context)
        val defaultSpecies = listOf("Large Mouth", "Small Mouth", "Crappie", "Walleye", "Catfish", "Perch", "Pike", "Bluegill")
        return saved.filterNot { it in defaultSpecies }
    }

    fun getAllSpecies(context: Context): List<String> {
        val defaultSpecies = listOf("Large Mouth", "Small Mouth", "Crappie", "Walleye", "Catfish", "Perch", "Pike", "Bluegill")
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

}
