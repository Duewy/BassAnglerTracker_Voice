package com.bramestorm.bassanglertracker.utils

object FishSpecies {

    //----------------- initial list of Species ----------------------
    val allSpeciesList = listOf(
        "large mouth",
        "small mouth",
        "crappie",
        "walleye",
        "catfish",
        "perch",
        "pike",
        "bluegill",
        "spotted bass",
        "rainbow trout",
        "brook trout",
        "brown trout",
        "lake trout",
        "salmon",
        "carp",
        "muskie",
        "white bass",
        "rock bass",
        "bowfin",
        "ling",
        "sucker",
        "drum",
        "striped bass",
        "saugeye",
        "sunfish",
        "gar",
        "bull head",
        "tarpon",
        "grouper",
        "red snapper"
    )

    // ----------------- DEFAULT SPECIES INITIALS ----------------------
    // Canonical initials for built-in species ONLY
    // User-added species will be appended via SharedPreferencesManager
    val defaultSpeciesInitials: Map<String, String> = mapOf(

        // Bass
        "large mouth"   to "LM",
        "small mouth"   to "SM",
        "spotted bass"  to "SP",
        "striped bass"  to "SB",
        "white bass"    to "WB",
        "rock bass"     to "RB",

        // Panfish / Common
        "crappie"       to "CP",
        "bluegill"      to "BG",
        "sunfish"       to "SF",
        "perch"         to "PH",
        "bull head"     to "BH",

        // Predators
        "walleye"       to "WE",
        "pike"          to "PK",
        "muskie"        to "MK",
        "gar"           to "GR",
        "bowfin"        to "BF",

        // Trout / Salmon
        "rainbow trout" to "RT",
        "brook trout"   to "BT",
        "brown trout"   to "BR",
        "lake trout"    to "LT",
        "salmon"        to "SL",

        // Other freshwater
        "carp"          to "CP",
        "sucker"        to "SK",
        "drum"          to "DR",
        "ling"          to "LG",
        "saugeye"       to "SG",

        // Saltwater
        "tarpon"        to "TP",
        "grouper"       to "GP",
        "red snapper"   to "RS"
    )
}
