package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object SharedPreferencesManager {
    private const val PREFS_NAME = "user_prefs"
    private const val SELECTED_SPECIES_KEY = "selected_species"
    private const val ORDERED_SPECIES_KEY = "ordered_species"
    private const val FULL_SPECIES_LIST_KEY = "full_species_list"


    fun getOrderedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(ORDERED_SPECIES_KEY, null)
        return saved?.split(",") ?: listOf(
            "Largemouth", "Smallmouth", "Crappie", "Walleye",
            "Catfish", "Perch", "Pike", "Panfish"
        )
    }

    fun getSelectedSpecies(context: Context): List<String> {
        return getOrderedSpeciesList(context)
    }

    fun initializeDefaultSpeciesIfNeeded(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = "DEFAULT_SPECIES_LOADED"
        val alreadyLoaded = prefs.getBoolean(key, false)

        if (!alreadyLoaded) {
            val defaultSpecies = listOf(
                "Largemouth", "Smallmouth", "Crappie", "Perch",
                "Walleye", "Catfish", "Pike", "Panfish"
            )

            saveAllSpecies(context, defaultSpecies)
            saveSelectedSpeciesList(context, defaultSpecies.take(4)) // just for first launch

            prefs.edit().putBoolean(key, true).apply()
        }
    }



    fun clearSelectedSpecies(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(SELECTED_SPECIES_KEY).remove(ORDERED_SPECIES_KEY).apply()
    }

    fun saveAllSpecies(context: Context, allSpecies: List<String>) {
        val prefs = context.getSharedPreferences("SpeciesPrefs", Context.MODE_PRIVATE).edit()
        val json = Gson().toJson(allSpecies)
        prefs.putString("ALL_SPECIES_LIST", json)
        prefs.apply()
    }

    fun getAllSavedSpecies(context: Context): List<String> {
        val prefs = context.getSharedPreferences("SpeciesPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("ALL_SPECIES_LIST", null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        else listOf()
    }

    fun getSelectedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences("SpeciesPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("SELECTED_SPECIES_LIST", null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else listOf()
    }

    fun saveSelectedSpeciesList(context: Context, selected: List<String>) {
        val prefs = context.getSharedPreferences("SpeciesPrefs", Context.MODE_PRIVATE).edit()
        val json = Gson().toJson(selected)
        prefs.putString("SELECTED_SPECIES_LIST", json)
        prefs.apply()
    }




}
