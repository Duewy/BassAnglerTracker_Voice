package com.bramestorm.bassanglertracker.questionnaire

// ─────────────────────────────────────────────────────────────────────────────
// FirstTimeQuestionnaireAnswers.kt
//
// Data model for the first-time questionnaire.
// Serialised to JSON via Gson and stored in SharedPreferences (local only).
//
// Mirrors iOS: FirstTimeQuestionnaireAnswers in firstTimeQuestionnaire.swift
// ─────────────────────────────────────────────────────────────────────────────

data class FirstTimeQuestionnaireAnswers(
    var countryCode:  String = "",
    var regionCode:   String = "",
    var waterType:    WaterType? = null,
    var purpose:      Purpose?   = null,
    var frequency:    Frequency? = null,
    var platforms:    MutableSet<FishingPlatform>  = mutableSetOf(),
    var techniques:   MutableSet<FishingTechnique> = mutableSetOf(),
    var speciesGroups: MutableSet<SpeciesGroup>    = mutableSetOf(),
    var gearInterests: MutableSet<GearInterest>    = mutableSetOf()
) {
    enum class WaterType { FRESHWATER, SALTWATER, BOTH }
    enum class Purpose   { FUN, COMPETITION, BOTH }
    enum class Frequency { FEW_PER_YEAR, MONTHLY, WEEKLY, VERY_FREQUENT }
}
