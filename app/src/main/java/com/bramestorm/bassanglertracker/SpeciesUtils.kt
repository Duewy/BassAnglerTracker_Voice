package com.bramestorm.bassanglertracker

fun getSpeciesImageResId(species: String?): Int {
    return when (species?.lowercase()) {
        "largemouth" -> R.drawable.fish_large_mouth
        "smallmouth" -> R.drawable.fish_small_mouth
        "crappie" -> R.drawable.fish_crappie
        "walleye" -> R.drawable.fish_walleye
        "catfish" -> R.drawable.fish_catfish
        "perch" -> R.drawable.fish_perch
        "pike" -> R.drawable.fish_northern_pike
        "bluegill", "panfish" -> R.drawable.fish_bluegill
        else -> R.drawable.fish_default
    }
}
