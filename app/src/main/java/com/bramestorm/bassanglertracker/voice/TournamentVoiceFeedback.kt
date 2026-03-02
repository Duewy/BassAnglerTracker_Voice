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

        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val catchType = currentCatch.catchType   // e.g. "tournament_pounds"
        val fullList = dbHelper.getCatchesForToday(catchType, todaysDate)

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
            totalWeightHundredthPounds = if (mode == MeasurementMode.POUNDS) (totalValue * 100).toInt() else 0,
            totalWeightPounds = if (mode == MeasurementMode.POUNDS) totalValue.toInt() else 0,
            totalWeightDec = if (mode == MeasurementMode.POUNDS) ((totalValue * 100).toInt() % 100) else 0,
            totalWeightHundredthKg = if (mode == MeasurementMode.KG) (totalValue * 100).toInt() else 0,
            totalWeightKgs = if (mode == MeasurementMode.KG) totalValue.toInt() else 0,
            totalWeightGrams = if (mode == MeasurementMode.KG) ((totalValue * 100).toInt() % 100) else 0,
            totalLengthQuarters = if (mode == MeasurementMode.INCHES) (totalValue * 4).toInt() else 0,
            totalLengthInches = if (mode == MeasurementMode.INCHES) totalValue.toInt() else 0,
            totalLengthFourths = if (mode == MeasurementMode.INCHES) ((totalValue * 4).toInt() % 4) else 0,
            totalLengthTenths = if (mode == MeasurementMode.CM) (totalValue * 10).toInt() else 0,
            totalLengthCms = if (mode == MeasurementMode.CM) totalValue.toInt() else 0,
            totalLengthDec = if (mode == MeasurementMode.CM) ((totalValue * 10).toInt() % 10) else 0,
            catchType = currentCatch.catchType,
            clipColor = currentCatch.clipColor ?: "",
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
    //==== END === Analyze Tournament Stats ===================================

    fun getCatchSummaryResponse(stats: TournamentCatchStats): String {
        return buildString {
            // 1️⃣ Total
            when (stats.mode) {
                MeasurementMode.LBS_OZ ->
                    append("Your total weight is ${stats.totalWeightLbs} pounds ${stats.totalWeightRemainingOz} ounces. ")
                MeasurementMode.POUNDS ->
                    append("Your total weight is ${stats.totalWeightPounds}.${stats.totalWeightDec} pounds. ")
                MeasurementMode.KG ->
                    append("Your total weight is ${stats.totalWeightKgs}.${stats.totalWeightGrams} kilograms. ")
                MeasurementMode.INCHES ->
                    append("Your total length is ${stats.totalLengthInches} and ${stats.totalLengthFourths} inches. ")
                MeasurementMode.CM ->
                    append("Your total length is ${stats.totalLengthCms}.${stats.totalLengthDec} centimeters. ")
            }

            // 2️⃣ Smallest fish
            stats.smallestCatch?.let {
                val description = when (stats.mode) {
                    MeasurementMode.LBS_OZ -> {
                        val oz = it.totalWeightOz
                        val lbs = oz?.div(16)
                        val remOz = oz?.rem(16)
                        "$lbs pounds $remOz ounces"
                    }
                    MeasurementMode.POUNDS -> {
                        val pounds = it.totalWeightHundredthPounds?.div(100)
                        val dec = it.totalWeightHundredthPounds?.rem(100)
                        "$pounds point $dec pounds"
                    }
                    MeasurementMode.KG -> {
                        val kg = it.totalWeightHundredthKg?.div(100)
                        val grams = it.totalWeightHundredthKg?.rem(100)
                        "$kg point $grams kilograms"
                    }
                    MeasurementMode.INCHES -> {
                        val quarters = it.totalLengthQuarters
                        val inches = quarters?.div(4)
                        val rem = quarters?.rem(4)
                        "$inches inches and $rem fourths"
                    }
                    MeasurementMode.CM -> {
                        val tenths = it.totalLengthTenths
                        val cm = tenths?.div(10)
                        val dec = tenths?.rem(10)
                        "$cm point $dec centimeters"
                    }
                }
                append("Your smallest fish is ${it.species} at $description. ")
            }

            // 3️⃣ Largest fish
            stats.largestCatch?.let {
                val description = when (stats.mode) {
                    MeasurementMode.LBS_OZ -> {
                        val oz = it.totalWeightOz
                        val lbs = oz?.div(16)
                        val remOz = oz?.rem(16)
                        "$lbs pounds $remOz ounces"
                    }
                    MeasurementMode.POUNDS -> {
                        val pounds = it.totalWeightHundredthPounds?.div(100)
                        val dec = it.totalWeightHundredthPounds?.rem(100)
                        "$pounds point $dec pounds"
                    }
                    MeasurementMode.KG -> {
                        val kg = it.totalWeightHundredthKg?.div(100)
                        val grams = it.totalWeightHundredthKg?.rem(100)
                        "$kg point $grams kilograms"
                    }
                    MeasurementMode.INCHES -> {
                        val quarters = it.totalLengthQuarters
                        val inches = quarters?.div(4)
                        val rem = quarters?.rem(4)
                        "$inches inches and $rem fourths"
                    }
                    MeasurementMode.CM -> {
                        val tenths = it.totalLengthTenths
                        val cm = tenths?.div(10)
                        val dec = tenths?.rem(10)
                        "$cm point $dec centimeters"
                    }
                }
                append("Biggest so far is ${it.species} at $description. ")
            }

            // 4️⃣ Position
            if (stats.thisCatchPosition > 0) {
                append("This catch is number ${stats.thisCatchPosition} on the list. ")
            }

            // 5️⃣ Time since last
            if (stats.timeSinceLastCatchMin > 0) {
                append("It’s been ${stats.timeSinceLastCatchMin} minutes since your last catch. ")
            }

            // 6️⃣ Time remaining
            if (stats.timeUntilAlarmMin > 0) {
                append("${stats.timeUntilAlarmMin} minutes remain in the tournament.")
            }
        }
    }


    // ✅ Determine value using measurement mode already provided

    private fun getTotalValue(list: List<CatchItem>, mode: MeasurementMode): Int{
        return list.sumOf { it.getComparisonValueByMode(mode)!! }
    }

    private fun getSmallestCatch(list: List<CatchItem>, mode: MeasurementMode): CatchItem? {
        return list.minByOrNull { it.getComparisonValueByMode(mode)!! }
    }

    private fun getLargestCatch(list: List<CatchItem>, mode: MeasurementMode): CatchItem? {
        return list.maxByOrNull { it.getComparisonValueByMode(mode)!! }
    }


}
