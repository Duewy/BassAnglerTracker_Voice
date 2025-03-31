package com.bramestorm.bassanglertracker.utils

import android.content.Context

object SharedPreferencesManager {
    private const val PREFS_NAME = "user_prefs"
    private const val SELECTED_SPECIES_KEY = "selected_species"
    private const val ORDERED_SPECIES_KEY = "ordered_species"

    fun saveOrderedSpeciesList(context: Context, speciesList: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ORDERED_SPECIES_KEY, speciesList.joinToString(",")).apply()
    }

    fun getOrderedSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(ORDERED_SPECIES_KEY, null)
        return saved?.split(",") ?: listOf(
            "Largemouth", "Smallmouth", "Crappie", "Walleye",
            "Catfish", "Perch", "Pike", "Bluegill"
        )
    }

    fun getSelectedSpecies(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(SELECTED_SPECIES_KEY, null)
        return savedSet?.toList() ?: listOf(
            "Largemouth", "Smallmouth", "Crappie", "Walleye",
            "Catfish", "Perch", "Pike", "Bluegill"
        )
    }

    fun clearSelectedSpecies(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(SELECTED_SPECIES_KEY).remove(ORDERED_SPECIES_KEY).apply()
    }
}
