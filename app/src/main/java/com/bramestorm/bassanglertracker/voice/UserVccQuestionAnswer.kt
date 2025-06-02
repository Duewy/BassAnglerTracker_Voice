package com.bramestorm.bassanglertracker.voice

import android.content.Context
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Version 2 — Unified voice Q&A system for Tournament and Fun Day mode.
 */
object UserVccQuestionAnswer {

    private var allCatches: List<CatchItem> = emptyList()
    private var topCatches: List<CatchItem> = emptyList()
    private var catchLimit: Int = 5
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1

    fun preloadData(context: Context, dbHelper: CatchDatabaseHelper) {
        catchLimit = SharedPreferencesManager.getNumberOfCatches(context)
        alarmHour = SharedPreferencesManager.getAlarmHour(context)
        alarmMinute = SharedPreferencesManager.getAlarmMinute(context)

        allCatches = dbHelper.getTopTournamentCatches(999)
        topCatches = allCatches.take(catchLimit)
    }

    fun handleVccQuestion(context: Context, question: String): String? {
        val normalized = question.lowercase(Locale.ROOT).trim()

        return when {
            normalized.contains("time now") -> getTimeNow()
            normalized.contains("time left") || normalized.contains("until alarm") -> getTimeLeft()
            normalized.contains("catch limit") || normalized.contains("goal") -> getCatchLimit()
            normalized.contains("how many") && normalized.contains("caught") -> getCatchCount()
            normalized.contains("total weight") -> getTotalWeight()
            normalized.contains("total length") -> getTotalLength()
            normalized.contains("largest fish") || normalized.contains("biggest fish") -> getLargestFish()
            normalized.contains("smallest fish caught") || normalized.contains("smallest fish today") -> getSmallestOverall()
            normalized.contains("smallest catch") || normalized.contains("next fish to cull") -> getSmallestOnList()
            normalized.contains("time since last catch") -> getTimeSinceLastCatch()
            normalized.contains("cull") -> getCullingStatus(context)
            else -> null
        }
    }

    private fun getTimeNow(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR)
        val minute = now.get(Calendar.MINUTE)
        val amPm = if (now.get(Calendar.AM_PM) == Calendar.PM) "PM" else "AM"
        val formattedMinute = String.format("%02d", minute)
        return "The time now is $hour:$formattedMinute $amPm.Over and Out"
    }

    private fun getTimeLeft(): String {
        if (alarmHour == -1 || alarmMinute == -1) return "No alarm has been set.Over and Out"

        val now = Calendar.getInstance()
        val alarm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmHour)
            set(Calendar.MINUTE, alarmMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DATE, 1)
        }

        val millisLeft = alarm.timeInMillis - now.timeInMillis
        val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(millisLeft)

        val amPm = if (alarmHour >= 12) "PM" else "AM"
        val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
        val formattedMinute = String.format("%02d", alarmMinute)

        return if (minutesLeft > 0)
            "You have $minutesLeft minutes left until $displayHour:$formattedMinute $amPm.Over and Out"
        else
            "Time is up. The alarm was set for $displayHour:$formattedMinute $amPm.Over and Out"
    }

    private fun getCatchLimit(): String {
        return "The catch limit today is your top $catchLimit fish.Over and Out"
    }

    private fun getCatchCount(): String {
        return "You have logged ${allCatches.size} fish so far today.Over and Out"
    }

    private fun getTotalWeight(): String {
        val totalOz = allCatches.sumOf { it.totalWeightOz ?: 0 }
        val lbs = totalOz / 16
        val oz = totalOz % 16
        return "The total weight of your logged fish is $lbs pounds and $oz ounces.Over and Out"
    }

    private fun getTotalLength(): String {
        val totalInches = allCatches.sumOf { it.totalLengthQuarters ?: 0 }
        val whole = totalInches / 4
        val quarters = totalInches % 4
        return "The total combined length is $whole and $quarters quarter inches.Over and Out"
    }

    private fun getLargestFish(): String {
        val largest = allCatches.maxByOrNull { it.totalWeightOz ?: 0 }
        return if (largest != null)
            "Your largest fish so far is ${largest.totalWeightOz} ounces.Over and Out"
        else
            "No catches recorded yet.Over and Out"
    }

    private fun getSmallestOverall(): String {
        val smallest = allCatches.minByOrNull { it.totalWeightOz ?: Int.MAX_VALUE }
        return if (smallest != null)
            "The smallest fish caught today is ${smallest.totalWeightOz} ounces.Over and Out"
        else
            "You have not caught any fish yet.Over and Out"
    }

    private fun getSmallestOnList(): String {
        val smallest = topCatches.minByOrNull { it.totalWeightOz ?: Int.MAX_VALUE }
        return if (smallest != null)
            "The smallest fish on your leaderboard list is ${smallest.totalWeightOz} ounces.Over and Out"
        else
            "There are not enough fish logged to calculate that.Over and Out"
    }

    private fun getTimeSinceLastCatch(): String {
        val last = allCatches.maxByOrNull { it.dateTime ?: "" } ?: return "You have no recent catches.Over and Out"

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val lastTime = sdf.parse(last.dateTime) ?: return "Date format error in last catch.Over and Out"

        val now = System.currentTimeMillis()
        val diffMillis = now - lastTime.time
        val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(diffMillis)

        return "Your last catch was about $minutesAgo minutes ago.Over and Out"
    }

    private fun getCullingStatus(context: Context): String {
        val limit = SharedPreferencesManager.getNumberOfCatches(context)
        val isCulling = SharedPreferencesManager.isCullingEnabled(context)

        return if (isCulling && limit > 0)
            "Culling is enabled. Only your top $limit fish will be kept.Over and Out"
        else
            "Culling is not enabled. All catches will be recorded.Over and Out"
    }

}//=== END= User Vcc Question Answer =========================