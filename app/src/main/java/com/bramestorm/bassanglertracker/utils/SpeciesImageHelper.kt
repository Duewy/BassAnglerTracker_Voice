package com.bramestorm.bassanglertracker.utils

import android.util.Log
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager.normalizeSpeciesName


//++++++++++++++++++++++ Pictures of the Fish Species +++++++++++++++++++++++++

object SpeciesImageHelper {


    fun getSpeciesImageResId(species: String): Int {
        val normalized = normalizeSpeciesName(species)
        return when (normalized) {
            "large mouth" -> R.drawable.fish_large_mouth
            "small mouth"-> R.drawable.fish_small_mouth
            "crappie" -> R.drawable.fish_crappie
            "walleye" -> R.drawable.fish_walleye
            "perch" -> R.drawable.fish_perch
            "pike", "northern pike" -> R.drawable.fish_northern_pike
            "catfish" -> R.drawable.fish_catfish
            "panfish"-> R.drawable.fish_bluegill
            "pumpkinseed"-> R.drawable.fish_pumpkinseed
            "bluegill" -> R.drawable.fish_bluegill
            "rainbow trout"-> R.drawable.fish_rainbow_trout
            "brook trout"-> R.drawable.fish_trout
            "brown trout" -> R.drawable.fish_brown_trout
            "lake trout" -> R.drawable.fish_lake_trout
            "sucker" -> R.drawable.fish_sucker
            "ling" -> R.drawable.fish_ling
            "salmon"-> R.drawable.fish_salmon
            "spotted bass"-> R.drawable.fish_spotted_bass
            "carp" -> R.drawable.fish_carp
            "muskie", "muskellunge" -> R.drawable.fish_muskie
            "bowfin" -> R.drawable.fish_bow_fin
            "gar" -> R.drawable.fish_gar
            "drum" -> R.drawable.fish_drum
            "saugeye" -> R.drawable.fish_saugeye
            "sauger" -> R.drawable.fish_saugeye
            "rock bass" -> R.drawable.fish_rock_bass
            "white bass" -> R.drawable.fish_white_bass
            "white fish" -> R.drawable.fish_whitefish
            "striped bass" -> R.drawable.fish_striped_bass
            "sunfish" -> R.drawable.fish_sunfish
            "bull head" -> R.drawable.fish_bull_head
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
