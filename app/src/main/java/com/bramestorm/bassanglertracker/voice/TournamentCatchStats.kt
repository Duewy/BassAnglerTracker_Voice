package com.bramestorm.bassanglertracker.voice

import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode

data class TournamentCatchStats(
    val mode: MeasurementMode,
    val species: String = "",
    val totalWeightOz: Int = 0,
    val totalWeightLbs: Int = 0,
    val totalWeightRemainingOz: Int = 0,
    val totalWeightPounds: Int = 0,
    val totalWeightDec: Int = 0,
    val totalWeightHundredthPounds: Int = 0,
    val totalWeightHundredthKg: Int = 0,
    val totalWeightKgs: Int = 0,
    val totalWeightGrams: Int = 0,
    val totalLengthTenths: Int = 0,
    val totalLengthCms: Int = 0,
    val totalLengthDec: Int = 0,
    val totalLengthQuarters: Int = 0,
    val totalLengthInches: Int = 0,
    val totalLengthFourths: Int = 0,
    val catchType: String = "",
    val clipColor: String = "",
    val totalCatches: Int = 0,
    val smallestCatch: CatchItem? = null,
    val largestCatch: CatchItem? = null,
    val thisCatchPosition: Int = 0,
    val timeSinceLastCatchMin: Int = 0,
    val timeUntilAlarmMin: Int = 0,
    val currentTime: String = "",
    val alarmTime: String = "",
    val fullCatchList: List<CatchItem> = emptyList()
)
