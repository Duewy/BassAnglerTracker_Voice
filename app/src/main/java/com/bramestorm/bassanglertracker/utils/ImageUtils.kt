package com.bramestorm.bassanglertracker.utils

import com.bramestorm.bassanglertracker.R

fun getSpeciesImageResId(species: String?): Int {
    return when (species?.trim()?.lowercase()) {
        "largemouth", "largemouthbass" -> R.drawable.fish_large_mouth
        "smallmouth", "smallmouthbass" -> R.drawable.fish_small_mouth
        "crappie" -> R.drawable.fish_crappie
        "walleye" -> R.drawable.fish_walleye
        "catfish" -> R.drawable.fish_catfish
        "perch" -> R.drawable.fish_perch
        "northernpike", "pike" -> R.drawable.fish_northern_pike
        "bluegill", "panfish" -> R.drawable.fish_bluegill
        "rainbowtrout", "brooktrout", "browntrout" -> R.drawable.fish_default
        "laketrout" -> R.drawable.fish_default
        "carp" -> R.drawable.fish_default
        "muskie", "muskellunge" -> R.drawable.fish_default
        "bowfin" -> R.drawable.fish_default
        "gar" -> R.drawable.fish_default
        "saugeye" -> R.drawable.fish_default
        "rockbass" -> R.drawable.fish_default
        "whitebass" -> R.drawable.fish_default
        "stripedbass" -> R.drawable.fish_default
        "sunfish" -> R.drawable.fish_default
        "bullhead", "bull head" -> R.drawable.fish_default
        else -> R.drawable.fish_default
    }
}

