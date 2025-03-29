package com.bramestorm.bassanglertracker.utils

import android.content.Context

object SharedPreferencesManager {
    private const val PREFS_NAME = "user_prefs"
    private const val SELECTED_SPECIES_KEY = "selected_species"

    fun saveSelectedSpecies(context: Context, speciesList: MutableList<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(SELECTED_SPECIES_KEY, speciesList.toSet()).apply()
    }


    fun getSelectedSpecies(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(SELECTED_SPECIES_KEY, emptySet()) ?: emptySet()
    }

    fun clearSelectedSpecies(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(SELECTED_SPECIES_KEY).apply()
    }
}
