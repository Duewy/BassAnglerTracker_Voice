package com.bramestorm.bassanglertracker.questionnaire

import android.content.Context
import com.google.gson.Gson

private val gson = Gson()

object FirstTimeQuestionnaireGate {
    private const val PREFS_NAME = "FirstTimeQuestionnaire"
    private const val KEY_COMPLETED = "FirstTimeQuestionnaireCompleted.v1"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_COMPLETED).apply()
    }
}

object FirstTimeQuestionnaireStore {
    private const val PREFS_NAME = "FirstTimeQuestionnaire"
    private const val KEY_ANSWERS = "FirstTimeQuestionnaireAnswers.v1"

    fun save(context: Context, answers: FirstTimeQuestionnaireAnswers) {
        val dto = AnswersDto.from(answers)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ANSWERS, gson.toJson(dto)).apply()
    }

    fun load(context: Context): FirstTimeQuestionnaireAnswers? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ANSWERS, null) ?: return null
        return try {
            gson.fromJson(json, AnswersDto::class.java).toAnswers()
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ANSWERS).apply()
    }

    private data class AnswersDto(
        val countryCode: String = "",
        val regionCode: String = "",
        val waterType: String? = null,
        val purpose: String? = null,
        val frequency: String? = null,
        val platforms: List<String> = emptyList(),
        val techniques: List<String> = emptyList(),
        val speciesGroups: List<String> = emptyList(),
        val gearInterests: List<String> = emptyList()
    ) {
        companion object {
            fun from(a: FirstTimeQuestionnaireAnswers) = AnswersDto(
                countryCode = a.countryCode,
                regionCode = a.regionCode,
                waterType = a.waterType?.name,
                purpose = a.purpose?.name,
                frequency = a.frequency?.name,
                platforms = a.platforms.map { it.name },
                techniques = a.techniques.map { it.name },
                speciesGroups = a.speciesGroups.map { it.name },
                gearInterests = a.gearInterests.map { it.name }
            )
        }

        fun toAnswers() = FirstTimeQuestionnaireAnswers(
            countryCode = countryCode,
            regionCode = regionCode,
            waterType = waterType?.let { runCatching { WaterType.valueOf(it) }.getOrNull() },
            purpose = purpose?.let { runCatching { Purpose.valueOf(it) }.getOrNull() },
            frequency = frequency?.let { runCatching { Frequency.valueOf(it) }.getOrNull() },
            platforms = platforms.mapNotNull { runCatching { FishingPlatform.valueOf(it) }.getOrNull() }.toSet(),
            techniques = techniques.mapNotNull { runCatching { FishingTechnique.valueOf(it) }.getOrNull() }.toSet(),
            speciesGroups = speciesGroups.mapNotNull { runCatching { SpeciesGroup.valueOf(it) }.getOrNull() }.toSet(),
            gearInterests = gearInterests.mapNotNull { runCatching { GearInterest.valueOf(it) }.getOrNull() }.toSet()
        )
    }
}

data class AdvertisingFocusProfile(
    var freshwater: Boolean = true,
    var saltwater: Boolean = true,
    var platforms: Set<String> = emptySet(),
    var techniques: Set<String> = emptySet(),
    var speciesGroups: Set<String> = emptySet(),
    var gearInterests: Set<String> = emptySet(),
    var tournamentFocused: Boolean = false,
    var frequentAngler: Boolean = false
)

object AdvertisingSelectionStore {
    private const val PREFS_NAME = "AdvertisingFocusPrefs"
    private const val KEY_PROFILE = "AdvertisingFocusProfile.v1"
    private const val KEY_SEEDED = "AdvertisingFocusProfile.seeded.v1"

    fun load(context: Context): AdvertisingFocusProfile {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE, null) ?: return AdvertisingFocusProfile()
        return try {
            gson.fromJson(json, AdvertisingFocusProfile::class.java)
        } catch (e: Exception) {
            AdvertisingFocusProfile()
        }
    }

    fun save(context: Context, profile: AdvertisingFocusProfile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE, gson.toJson(profile))
            .putBoolean(KEY_SEEDED, true)
            .apply()
    }

    fun hasSeeded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEDED, false)

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PROFILE)
            .remove(KEY_SEEDED)
            .apply()
    }

    fun seedIfNeeded(context: Context, answers: FirstTimeQuestionnaireAnswers?) {
        if (hasSeeded(context) || answers == null) return
        val profile = AdvertisingFocusProfile(
            freshwater = answers.waterType != WaterType.SALTWATER,
            saltwater = answers.waterType != WaterType.FRESHWATER,
            platforms = answers.platforms.map { it.name }.toSet(),
            techniques = answers.techniques.map { it.name }.toSet(),
            speciesGroups = answers.speciesGroups.map { it.name }.toSet(),
            gearInterests = answers.gearInterests.map { it.name }.toSet(),
            tournamentFocused = answers.purpose == Purpose.COMPETITION || answers.purpose == Purpose.BOTH,
            frequentAngler = answers.frequency == Frequency.WEEKLY || answers.frequency == Frequency.VERY_FREQUENT
        )
        save(context, profile)
    }
}
