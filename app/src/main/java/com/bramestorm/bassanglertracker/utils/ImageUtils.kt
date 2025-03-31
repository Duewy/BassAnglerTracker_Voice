package com.bramestorm.bassanglertracker.utils

import com.bramestorm.bassanglertracker.R


fun getSpeciesImageResId(species: String?): Int {
    return when (species?.lowercase()) {
        "largemouth", "largemouth bass" -> R.drawable.fish_large_mouth
        "smallmouth", "smallmouth bass" -> R.drawable.fish_small_mouth
        "crappie" -> R.drawable.fish_crappie
        "walleye" -> R.drawable.fish_walleye
        "catfish" -> R.drawable.fish_catfish
        "perch" -> R.drawable.fish_perch
        "northern pike", "pike" -> R.drawable.fish_northern_pike
        "bluegill", "panfish" -> R.drawable.fish_bluegill
        "rainbow trout", "brook trout", "brown trout" -> R.drawable.fish_default
        "lake trout" -> R.drawable.fish_default
        "carp" -> R.drawable.fish_default
        "muskie", "muskellunge" -> R.drawable.fish_default
        "bowfin" -> R.drawable.fish_default
        "gar" -> R.drawable.fish_default
        "saugeye" -> R.drawable.fish_default
        "rock bass" -> R.drawable.fish_default
        "white bass" -> R.drawable.fish_default
        "striped bass" -> R.drawable.fish_default
        "sunfish" -> R.drawable.fish_default
        "bullhead" -> R.drawable.fish_default
        else -> R.drawable.fish_default
    }
}
