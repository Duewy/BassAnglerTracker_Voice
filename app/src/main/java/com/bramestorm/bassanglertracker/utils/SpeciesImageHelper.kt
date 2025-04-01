package com.bramestorm.bassanglertracker.utils

import com.bramestorm.bassanglertracker.R

        //++++++++++++++++++++++ Pictures of the Fish Species +++++++++++++++++++++++++

object SpeciesImageHelper {
    fun getSpeciesImageResId(speciesName: String): Int {
        return when (speciesName.trim().lowercase()) {
            "largemouth", "largemouth bass" -> R.drawable.fish_large_mouth
            "smallmouth", "smallmouth bass" -> R.drawable.fish_small_mouth
            "crappie" -> R.drawable.fish_crappie
            "walleye" -> R.drawable.fish_walleye
            "catfish" -> R.drawable.fish_catfish
            "perch" -> R.drawable.fish_perch
            "pike", "northern pike" -> R.drawable.fish_northern_pike
            "bluegill", "panfish" -> R.drawable.fish_bluegill
            "rainbow trout"-> R.drawable.fish_rainbow_trout
            "brook trout"-> R.drawable.fish_trout
            "brown trout" -> R.drawable.fish_trout
            "lake trout" -> R.drawable.fish_lake_trout
            "ling" -> R.drawable.fish_ling
            "salmon"-> R.drawable.fish_salmon
            "carp" -> R.drawable.fish_carp
            "muskie", "muskellunge" -> R.drawable.fish_muskie
            "bowfin" -> R.drawable.fish_bow_fin
            "gar" -> R.drawable.fish_default
            "saugeye" -> R.drawable.fish_default
            "rock bass" -> R.drawable.fish_default
            "white bass" -> R.drawable.fish_default
            "striped bass" -> R.drawable.fish_default
            "sunfish" -> R.drawable.fish_default
            "bullhead" -> R.drawable.fish_default
            "tarpon"-> R.drawable.sw_fish_tarpon
            "grouper"-> R.drawable.sw_fish_grouper
            "red snapper" -> R.drawable.sw_fish_red_snapper
            else -> R.drawable.fish_default
        }
    }
}
