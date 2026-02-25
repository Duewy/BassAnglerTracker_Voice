package com.bramestorm.bassanglertracker.questionnaire

// ─────────────────────────────────────────────────────────────────────────────
// AdTargetingModels.kt
//
// Enums describing the angler's fishing profile for local ad-preference seeding.
// All values are stored on-device only (SharedPreferences). No network calls.
//
// Mirrors iOS: CatchAndCall/Database/Models/AdTargetingModels.swift
// ─────────────────────────────────────────────────────────────────────────────

/** Fishing platform / vessel types. */
enum class FishingPlatform(val label: String) {
    SHORE("Shore / Bank"),
    KAYAK_CANOE("Kayak / Canoe"),
    BOAT("Boat"),
    MULTIPLE("Multiple platforms")
}

/** Fishing technique styles. */
enum class FishingTechnique(val label: String) {
    CASTING_SPINNING("Casting / Spinning"),
    TROLLING("Trolling"),
    FLY_FISHING("Fly Fishing"),
    SPEAR_BOW("Spear / Bow Fishing"),
    OTHER("Other / Mixed")
}

/** Broad species groups for ad targeting. */
enum class SpeciesGroup(val label: String) {
    BASS("Bass (Largemouth / Smallmouth)"),
    TROUT_SALMON("Trout / Salmon"),
    WALLEYE_ZANDER("Walleye / Zander"),
    PIKE_MUSKIE("Pike / Muskie"),
    PANFISH("Panfish"),
    INSHORE_SALT("Inshore Saltwater Species"),
    OFFSHORE_SALT("Offshore Saltwater Species"),
    MULTI_SPECIES("Multi-Species / Everything")
}

/** Gear / product categories for ad targeting. */
enum class GearInterest(val label: String) {
    RODS_REELS("Rods & Reels"),
    ELECTRONICS("Electronics / Sonar / GPS"),
    BOATS_MOTORS("Boats / Motors"),
    APPAREL_ACCESSORIES("Apparel & Accessories"),
    GENERAL_GEAR("General Fishing Gear")
}
