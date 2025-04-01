package com.bramestorm.bassanglertracker.utils

import android.content.Context

object SharedPreferencesManager {
    private const val PREFS_NAME = "user_prefs"
    private const val SELECTED_SPECIES_KEY = "selected_species"
    private const val ORDERED_SPECIES_KEY = "ordered_species"
    private const val FULL_SPECIES_LIST_KEY = "full_species_list"

    // ✅ Save the 8 selected species
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
        return getOrderedSpeciesList(context)
    }


    // ✅ Save full species list
    fun saveFullSpeciesList(context: Context, allSpecies: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(FULL_SPECIES_LIST_KEY, allSpecies.joinToString(",")).apply()
    }

    fun getFullSpeciesList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(FULL_SPECIES_LIST_KEY, null)
        return saved?.split(",") ?: listOf(
            "Largemouth", "Smallmouth", "Crappie", "Walleye",
            "Catfish", "Perch", "Pike", "Bluegill",
            "Carp", "Musky", "Trout", "Salmon", "Other"
        )
    }

    fun clearSelectedSpecies(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(SELECTED_SPECIES_KEY).remove(ORDERED_SPECIES_KEY).apply()
    }
}
