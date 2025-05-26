package com.bramestorm.bassanglertracker.voice

import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.getComparisonValueByMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TournamentVoiceFeedback {

    fun analyzeTournamentStats(
        dbHelper: CatchDatabaseHelper,
        tournamentCatchLimit: Int,
        alarmHour: Int,
        alarmMinute: Int,
        currentCatch: CatchItem,
        mode: MeasurementMode
    ): TournamentCatchStats {
        val fullList = dbHelper.getTopTournamentCatches(tournamentCatchLimit + 6)
        val sorted = fullList.sortedByDescending { it.getComparisonValueByMode(mode) }
        val topN = sorted.take(tournamentCatchLimit)

        val totalValue = getTotalValue(topN, mode)
        val smallest = getSmallestCatch(topN, mode)
        val largest = getLargestCatch(topN, mode)
        val thisPosition = sorted.indexOfFirst { it.id == currentCatch.id } + 1

        val lastCatchTime = dbHelper.getLastCatchTimeMillis()
        val nowTime = System.currentTimeMillis()
        val sinceLastMin = ((nowTime - lastCatchTime) / 60000).toInt()

        val alarmMin = alarmHour * 60 + alarmMinute
        val calendar = java.util.Calendar.getInstance()
        val nowMin = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        val minsLeft = alarmMin - nowMin


        return TournamentCatchStats(
            mode = mode,
            species = currentCatch.species,
            totalWeightOz = if (mode == MeasurementMode.LBS_OZ) totalValue.toInt() else 0,
            totalWeightLbs = if (mode == MeasurementMode.LBS_OZ) (totalValue.toInt() / 16) else 0,
            totalWeightRemainingOz = if (mode == MeasurementMode.LBS_OZ) (totalValue.toInt() % 16) else 0,
            totalWeightHundredthKg = if (mode == MeasurementMode.KG) (totalValue * 100).toInt() else null,
            totalWeightKgs = if (mode == MeasurementMode.KG) totalValue.toInt() else 0,
            totalWeightGrams = if (mode == MeasurementMode.KG) ((totalValue * 100).toInt() % 100) else 0,
            totalLengthQuarters = if (mode == MeasurementMode.INCHES) (totalValue * 4).toInt() else null,
            totalLengthInches = if (mode == MeasurementMode.INCHES) totalValue.toInt() else 0,
            totalLengthFourths = if (mode == MeasurementMode.INCHES) ((totalValue * 4).toInt() % 4) else 0,
            totalLengthTenths = if (mode == MeasurementMode.CM) (totalValue * 10).toInt() else null,
            totalLengthCms = if (mode == MeasurementMode.CM) totalValue.toInt() else 0,
            totalLengthDec = if (mode == MeasurementMode.CM) ((totalValue * 10).toInt() % 10) else 0,
            catchType = currentCatch.catchType,
            clipColor = currentCatch.clipColor,
            totalCatches = sorted.size,
            smallestCatch = smallest,
            largestCatch = largest,
            thisCatchPosition = thisPosition,
            timeSinceLastCatchMin = sinceLastMin,
            timeUntilAlarmMin = if (minsLeft > 0) minsLeft else 0,
            currentTime = SimpleDateFormat("h:mm a", Locale.ROOT).format(Date()),
            alarmTime = String.format(Locale.ROOT, "%02d:%02d", alarmHour, alarmMinute),
            fullCatchList = topN
        )
    }

    fun getCatchSummaryResponse(stats: TournamentCatchStats): String {
        return buildString {
            when (stats.mode) {
                MeasurementMode.LBS_OZ -> append("Your total weight is ${stats.totalWeightLbs} pounds ${stats.totalWeightRemainingOz} ounces. ")
                MeasurementMode.KG     -> append("Your total weight is ${stats.totalWeightKgs}.${stats.totalWeightGrams} kilograms. ")
                MeasurementMode.INCHES -> append("Your total length is ${stats.totalLengthInches} and ${stats.totalLengthFourths} inches. ")
                MeasurementMode.CM     -> append("Your total length is ${stats.totalLengthCms}.${stats.totalLengthDec} centimeters. ")
            }

            stats.smallestCatch?.let {
                val oz = it.totalWeightOz ?: 0
                append("Your smallest fish is ${it.species} at ${oz / 16} pounds ${oz % 16} ounces. ")
            }

            stats.largestCatch?.let {
                val oz = it.totalWeightOz ?: 0
                append("Biggest so far is ${it.species}, ${oz / 16} pounds ${oz % 16} ounces. ")
            }

            if (stats.thisCatchPosition > 0) {
                append("This catch ranks number ${stats.thisCatchPosition}. ")
            }

            if (stats.timeSinceLastCatchMin != null && stats.timeSinceLastCatchMin > 0) {
                append("It’s been ${stats.timeSinceLastCatchMin} minutes since your last catch. ")
            }

            if (stats.timeUntilAlarmMin != null && stats.timeUntilAlarmMin > 0) {
                append("${stats.timeUntilAlarmMin} minutes remain in the tournament.")
            }
        }
    }

    // ????????????? Find Out which Catch Entry Tournament we are Using  ?????????
    private fun getTotalValue(list: List<CatchItem>, mode: MeasurementMode): Int{
        return list.sumOf { it.getComparisonValueByMode(mode) }
    }

    private fun getSmallestCatch(list: List<CatchItem>, mode: MeasurementMode): CatchItem? {
        return list.minByOrNull { it.getComparisonValueByMode(mode) }
    }

    private fun getLargestCatch(list: List<CatchItem>, mode: MeasurementMode): CatchItem? {
        return list.maxByOrNull { it.getComparisonValueByMode(mode) }
    }

}
