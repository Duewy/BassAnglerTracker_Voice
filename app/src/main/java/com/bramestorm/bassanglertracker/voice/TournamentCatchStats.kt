package com.bramestorm.bassanglertracker.voice

import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode

data class TournamentCatchStats(
    val mode: MeasurementMode,
    val species: String?,
    val totalWeightOz: Int,
    val totalWeightLbs: Int,
    val totalWeightRemainingOz: Int,
    val totalWeightPounds : Int,
    val totalWeightDec : Int,
    val totalWeightHundredthPounds : Int,
    val totalWeightHundredthKg: Int?,
    val totalWeightKgs: Int,
    val totalWeightGrams: Int,
    val totalLengthTenths: Int?,
    val totalLengthCms: Int,
    val totalLengthDec: Int,
    val totalLengthQuarters: Int?,
    val totalLengthInches: Int,
    val totalLengthFourths: Int,
    val catchType: String?,
    val clipColor: String?,
    val totalCatches: Int,
    val smallestCatch: CatchItem?,
    val largestCatch: CatchItem?,
    val thisCatchPosition: Int,
    val timeSinceLastCatchMin: Int?,
    val timeUntilAlarmMin: Int?,
    val currentTime: String,
    val alarmTime: String,
    val fullCatchList: List<CatchItem>
)
