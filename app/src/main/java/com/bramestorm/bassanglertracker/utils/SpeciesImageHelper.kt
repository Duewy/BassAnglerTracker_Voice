package com.bramestorm.bassanglertracker.utils

import android.util.Log
import com.bramestorm.bassanglertracker.R

        //++++++++++++++++++++++ Pictures of the Fish Species +++++++++++++++++++++++++

object SpeciesImageHelper {

    // -- 🔒 Private helper to normalize species name from various forms "Large Mouth, Largemouth, largemouth, large mouth" are now all the same

    fun normalizeSpeciesName(name: String?): String {
        return name?.trim()?.lowercase()?.replace("\\s+".toRegex(), " ") ?: ""
    }


    fun getSpeciesImageResId(species: String): Int {
        val normalized = normalizeSpeciesName(species)
        return when (normalized) {
            "large mouth", "largemouth" -> R.drawable.fish_large_mouth
            "small mouth", "smallmouth"-> R.drawable.fish_small_mouth
            "crappie" -> R.drawable.fish_crappie
            "walleye" -> R.drawable.fish_walleye
            "perch" -> R.drawable.fish_perch
            "pike", "northern pike" -> R.drawable.fish_northern_pike
            "catfish" -> R.drawable.fish_catfish
            "panfish", "bluegill" -> R.drawable.fish_bluegill
            "rainbow trout"-> R.drawable.fish_rainbow_trout
            "brook trout"-> R.drawable.fish_trout
            "brown trout" -> R.drawable.fish_brown_trout
            "lake trout" -> R.drawable.fish_lake_trout
            "ling" -> R.drawable.fish_ling
            "salmon"-> R.drawable.fish_salmon
            "carp" -> R.drawable.fish_carp
            "muskie", "muskellunge" -> R.drawable.fish_muskie
            "bowfin" -> R.drawable.fish_bow_fin
            "gar" -> R.drawable.fish_gar
            "saugeye" -> R.drawable.fish_saugeye
            "rock bass" -> R.drawable.fish_default
            "white bass" -> R.drawable.fish_white_bass
            "striped bass" -> R.drawable.fish_striped_bass
            "sunfish" -> R.drawable.fish_sunfish
            "bullhead" -> R.drawable.fish_bull_head
            "tarpon"-> R.drawable.sw_fish_tarpon
            "grouper"-> R.drawable.sw_fish_grouper
            "red snapper" -> R.drawable.sw_fish_red_snapper
            else -> {
                Log.w("SpeciesImageHelper", "Unknown species image: $species")
                R.drawable.fish_default
            }

        }
    }
}
