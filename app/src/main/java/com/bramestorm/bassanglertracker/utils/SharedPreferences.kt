package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPreferencesManager {
    private const val PREFS_NAME = "SpeciesPrefs"
    private const val SELECTED_SPECIES_KEY = "SELECTED_SPECIES_LIST"
    private const val ALL_SPECIES_KEY = "ALL_SPECIES_LIST"
    private const val INIT_FLAG_KEY = "DEFAULT_SPECIES_LOADED"

    // Default species list
    private val defaultSpecies = listOf(
        "Largemouth", "Smallmouth", "Crappie", "Walleye",
        "Catfish", "Perch", "Pike", "Bluegill"
    )

    fun initializeDefaultSpeciesIfNeeded(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val alreadyLoaded = prefs.getBoolean(INIT_FLAG_KEY, false)

        if (!alreadyLoaded) {
            saveAllSpecies(context, defaultSpecies)
            saveSelectedSpeciesList(context, defaultSpecies.take(5)) // Load 5 by default
            prefs.edit().putBoolean(INIT_FLAG_KEY, true).apply()
        }
    }

    fun getSelectedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(SELECTED_SPECIES_KEY, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveSelectedSpeciesList(context: Context, selected: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val json = Gson().toJson(selected)
        prefs.putString(SELECTED_SPECIES_KEY, json)
        prefs.apply()
    }

    fun getAllSavedSpecies(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ALL_SPECIES_KEY, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveAllSpecies(context: Context, allSpecies: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val json = Gson().toJson(allSpecies)
        prefs.putString(ALL_SPECIES_KEY, json)
        prefs.apply()
    }

    fun getUserAddedSpeciesList(context: Context): List<String> {
        val saved = getAllSavedSpecies(context)
        return saved.filterNot { it in defaultSpecies }
    }

    fun getAllSpecies(context: Context): List<String> {
        return (defaultSpecies + getUserAddedSpeciesList(context)).distinct()
    }

    fun clearSelectedSpecies(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(SELECTED_SPECIES_KEY).remove(ALL_SPECIES_KEY).apply()
    }
}
