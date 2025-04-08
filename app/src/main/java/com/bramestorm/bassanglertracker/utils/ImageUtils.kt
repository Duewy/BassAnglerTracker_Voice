package com.bramestorm.bassanglertracker.utils

import android.util.Log
import com.bramestorm.bassanglertracker.R

fun getSpeciesImageResId(species: String?): Int {
    return when (species?.trim()?.lowercase()) {
        "largemouth", "large mouth" -> R.drawable.fish_large_mouth
        "smallmouth", "small mouth" -> R.drawable.fish_small_mouth
        "crappie" -> R.drawable.fish_crappie
        "walleye" -> R.drawable.fish_walleye
        "catfish" -> R.drawable.fish_catfish
        "perch" -> R.drawable.fish_perch
        "northern pike", "pike" -> R.drawable.fish_northern_pike
        "bluegill" -> R.drawable.fish_bluegill
        "rainbow trout" -> R.drawable.fish_rainbow_trout
        "brook trout" -> R.drawable.fish_brown_trout
        "brown trout" -> R.drawable.fish_brown_trout
        "lake trout" -> R.drawable.fish_lake_trout
        "carp" -> R.drawable.fish_carp
        "salmon" -> R.drawable.fish_salmon
        "burbot", "ling" -> R.drawable.fish_ling
        "muskellunge", "muskie" -> R.drawable.fish_muskie
        "bowfin" -> R.drawable.fish_bow_fin
        "gar" -> R.drawable.fish_gar
        "saugeye" -> R.drawable.fish_saugeye
        "rock bass" -> R.drawable.fish_rock_bass
        "white bass" -> R.drawable.fish_white_bass
        "striped bass" -> R.drawable.fish_striped_bass
        "sucker" -> R.drawable.fish_sucker
        "sunfish" -> R.drawable.fish_sunfish
        "panfish" -> R.drawable.fish_default
        "bullhead", "bull head" -> R.drawable.fish_bull_head
        "drum" -> R.drawable.fish_drum
        "tarpon" -> R.drawable.sw_fish_tarpon
        "grouper" -> R.drawable.sw_fish_grouper
        "red snapper" -> R.drawable.sw_fish_red_snapper
        else -> {
            Log.w("SpeciesImage", "🔍 No image for species: \"$species\" → using default.")
            R.drawable.fish_default
        }

    }
}




