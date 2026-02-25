package com.bramestorm.bassanglertracker.questionnaire

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────────────────────────────────────
// AdvertisingFocusProfile.kt
//
// Local-only advertising preference profile derived from the questionnaire.
// Stored in SharedPreferences. No network calls.
//
// Mirrors iOS:
//   AdvertisingFocusProfile struct  →  AdvertisingFocusProfile.swift
//   AdvertisingSelectionStore enum  →  AdvertisingFocusProfile.swift
// ─────────────────────────────────────────────────────────────────────────────

data class AdvertisingFocusProfile(
    var freshwater:        Boolean               = true,
    var saltwater:         Boolean               = true,
    var platforms:         Set<FishingPlatform>  = emptySet(),
    var techniques:        Set<FishingTechnique> = emptySet(),
    var speciesGroups:     Set<SpeciesGroup>      = emptySet(),
    var gearInterests:     Set<GearInterest>      = emptySet(),
    var tournamentFocused: Boolean               = false,
    var frequentAngler:    Boolean               = false
)

object AdvertisingSelectionStore {

    private const val PREFS_NAME  = "advertising_profile"
    private const val KEY_PROFILE = "AdvertisingFocusProfile.v1"
    private const val KEY_SEEDED  = "AdvertisingFocusProfile.seeded.v1"

    private val gson = Gson()
    private val type = object : TypeToken<AdvertisingFocusProfile>() {}.type

    fun load(context: Context): AdvertisingFocusProfile {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE, null) ?: return AdvertisingFocusProfile()
        return runCatching { gson.fromJson<AdvertisingFocusProfile>(json, type) }
            .getOrDefault(AdvertisingFocusProfile())
    }

    private fun hasSeeded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEDED, false)

    private fun save(context: Context, profile: AdvertisingFocusProfile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE, gson.toJson(profile))
            .putBoolean(KEY_SEEDED, true)
            .apply()
    }

    /**
     * Seeds the advertising profile from questionnaire answers.
     * Runs exactly once; subsequent calls are no-ops.
     * Mirrors iOS: AdvertisingSelectionStore.seedIfNeeded(from:)
     */
    fun seedIfNeeded(context: Context, answers: FirstTimeQuestionnaireAnswers?) {
        if (hasSeeded(context) || answers == null) return

        val profile = AdvertisingFocusProfile().apply {
            when (answers.waterType) {
                FirstTimeQuestionnaireAnswers.WaterType.FRESHWATER -> {
                    freshwater = true;  saltwater = false
                }
                FirstTimeQuestionnaireAnswers.WaterType.SALTWATER  -> {
                    freshwater = false; saltwater = true
                }
                FirstTimeQuestionnaireAnswers.WaterType.BOTH       -> {
                    freshwater = true;  saltwater = true
                }
                null -> Unit
            }
            platforms     = answers.platforms.toSet()
            techniques    = answers.techniques.toSet()
            speciesGroups = answers.speciesGroups.toSet()
            gearInterests = answers.gearInterests.toSet()
            tournamentFocused = answers.purpose == FirstTimeQuestionnaireAnswers.Purpose.COMPETITION ||
                                answers.purpose == FirstTimeQuestionnaireAnswers.Purpose.BOTH
            frequentAngler    = answers.frequency == FirstTimeQuestionnaireAnswers.Frequency.WEEKLY ||
                                answers.frequency == FirstTimeQuestionnaireAnswers.Frequency.VERY_FREQUENT
        }
        save(context, profile)
    }

    /** Clears the profile and the seeded flag. For developer / debug use only. */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PROFILE)
            .remove(KEY_SEEDED)
            .apply()
    }
}
