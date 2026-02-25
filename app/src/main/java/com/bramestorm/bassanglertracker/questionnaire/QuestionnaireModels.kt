package com.bramestorm.bassanglertracker.questionnaire

enum class FishingPlatform(val label: String) {
    SHORE("Shore / Bank"),
    KAYAK_CANOE("Kayak / Canoe"),
    BOAT("Boat"),
    MULTIPLE("Multiple Platforms")
}

enum class FishingTechnique(val label: String) {
    CASTING_SPINNING("Casting / Spinning"),
    TROLLING("Trolling"),
    FLY_FISHING("Fly Fishing"),
    SPEAR_BOW("Spear / Bow Fishing"),
    OTHER("Other / Mixed")
}

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

enum class GearInterest(val label: String) {
    RODS_REELS("Rods & Reels"),
    ELECTRONICS("Electronics / Sonar / GPS"),
    BOATS_MOTORS("Boats / Motors"),
    APPAREL_ACCESSORIES("Apparel & Accessories"),
    GENERAL_GEAR("General Fishing Gear")
}

enum class WaterType(val label: String) {
    FRESHWATER("Freshwater"),
    SALTWATER("Saltwater"),
    BOTH("Both")
}

enum class Purpose(val label: String) {
    FUN("Fun / Recreation"),
    COMPETITION("Competition / Tournament"),
    BOTH("Both")
}

enum class Frequency(val label: String) {
    FEW_PER_YEAR("A few times per year"),
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    VERY_FREQUENT("Multiple times per week")
}

data class FirstTimeQuestionnaireAnswers(
    var countryCode: String = "",
    var regionCode: String = "",
    var waterType: WaterType? = null,
    var purpose: Purpose? = null,
    var frequency: Frequency? = null,
    var platforms: Set<FishingPlatform> = emptySet(),
    var techniques: Set<FishingTechnique> = emptySet(),
    var speciesGroups: Set<SpeciesGroup> = emptySet(),
    var gearInterests: Set<GearInterest> = emptySet()
)
