package com.bramestorm.bassanglertracker.questionnaire

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class AdvertisingFocusProfile(
    var freshwater: Boolean = true,
    var saltwater: Boolean = true,
    var platforms: Set<FishingPlatform> = emptySet(),
    var techniques: Set<FishingTechnique> = emptySet(),
    var speciesGroups: Set<SpeciesGroup> = emptySet(),
    var gearInterests: Set<GearInterest> = emptySet(),
    var tournamentFocused: Boolean = false,
    var frequentAngler: Boolean = false
)

object AdvertisingSelectionStore {
    private const val PREFS_NAME = "advertising_profile"
    private const val KEY_PROFILE = "AdvertisingFocusProfile.v1"
    private const val KEY_SEEDED = "AdvertisingFocusProfile.seeded.v1"

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

    fun seedIfNeeded(context: Context, answers: FirstTimeQuestionnaireAnswers?) {
        if (hasSeeded(context) || answers == null) return

        val wt = answers.waterType
        val profile = AdvertisingFocusProfile(
            freshwater = wt != WaterType.SALTWATER,
            saltwater = wt != WaterType.FRESHWATER,
            platforms = answers.platforms,
            techniques = answers.techniques,
            speciesGroups = answers.speciesGroups,
            gearInterests = answers.gearInterests,
            tournamentFocused = answers.purpose == Purpose.COMPETITION || answers.purpose == Purpose.BOTH,
            frequentAngler = answers.frequency == Frequency.WEEKLY || answers.frequency == Frequency.VERY_FREQUENT
        )

        save(context, profile)
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PROFILE)
            .remove(KEY_SEEDED)
            .apply()
    }
}