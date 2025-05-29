package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.util.Log
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.getComparisonValueByMode
import com.bramestorm.bassanglertracker.models.MotivationContext


fun generateMotivationalMessage(context: MotivationContext): String {
    val remaining = context.totalNeeded - context.currentCount
    val percent: Int = if (context.smallestComparisonValue > 0)
        ((context.comparisonValue - context.smallestComparisonValue) / context.smallestComparisonValue) * 100
    else 0

    return when {
        context.isNewBiggestOfDay && context.currentCount >= 4 -> {
            Log.d("MOTIVATION", "Triggered: NewBiggestOfDay")
            newBiggestCatchMessages.random()
        }
        percent > 20 -> {
            Log.d("MOTIVATION", "Triggered: BigImprovement ($percent%)")
            getBigImprovementMessage(percent)
        }
        context.timeSinceLastCatchMillis > 10 * 60 * 1000 -> {
            Log.d("MOTIVATION", "Triggered: SlowReturn (${context.timeSinceLastCatchMillis}ms)")
            slowReturnMessages.random()
        }
        context.currentCount == context.totalNeeded -> {
            Log.d("MOTIVATION", "Triggered: FinalCatch")
            finalCatchMessages.random()
        }
        remaining == 2 -> {
            Log.d("MOTIVATION", "Triggered: getTwoRemaining($remaining)")
            getTwoRemainingMessage(remaining)
        }
        remaining == 1 -> {
            Log.d("MOTIVATION", "Triggered: getOneRemaining($remaining)")
            getOneRemainingMessage(remaining)
        }
        else -> {
            Log.d("MOTIVATION", "Triggered: GeneralMessage")
            generalMessages.random()
        }
    }



}// ==== END == Generate Motivational Messages ===================

fun getMotivationalMessage(
    context: Context,
    catchItemId: Int,
    tournamentCatchLimit: Int,
    comparisonModeRaw: String
): String? {
    val dbHelper = CatchDatabaseHelper(context)
    val catch = dbHelper.getCatchById(catchItemId) ?: return null

    // 🔄 Convert raw string to MeasurementMode (safe fallback = LBS_OZ)
    val mode = when (comparisonModeRaw.lowercase()) {
        "lbsozs", "weight", "lbs" -> MeasurementMode.LBS_OZ
        "kgs", "weightkg"         -> MeasurementMode.KG
        "inches", "length"        -> MeasurementMode.INCHES
        "cm", "lengthcm"          -> MeasurementMode.CM
        else                      -> MeasurementMode.LBS_OZ
    }

    val topCatches = dbHelper.getTopTournamentCatches(tournamentCatchLimit)
    val smallest = topCatches
        .minByOrNull { it.getComparisonValueByMode(mode) }
        ?.getComparisonValueByMode(mode)
        ?: catch.getComparisonValueByMode(mode)

    val isNewBiggestOfDay = topCatches.firstOrNull()?.id == catch.id
    val lastCatchTime = dbHelper.getLastCatchTimeMillis()
    val now = System.currentTimeMillis()
    val timeSinceLastCatch = now - lastCatchTime

    val contextObj = MotivationContext(
        currentCount = topCatches.size,
        totalNeeded = tournamentCatchLimit,
        timeSinceLastCatchMillis = timeSinceLastCatch,
        comparisonValue = catch.getComparisonValueByMode(mode),
        smallestComparisonValue = smallest,
        isNewBiggestOfDay = isNewBiggestOfDay
    )

    return generateMotivationalMessage(contextObj)
}


private val newBiggestCatchMessages = listOf(
        "🐋 That’s your biggest of the day!",
        "🏆 That fish tops the charts today!",
        "📈 New personal best — for now!",
        "🎯 Biggest so far — let’s keep going!",
        "🔝 You just raised the bar!",
        "⚖️ Heaviest one today — nice!",
        "🔥 That's the new benchmark!",
        "🥇 Leader of the day — so far!",
        "🌊 Big splash for the big catch!",
        "🚀 That one moved the needle!",
        "🎣 That’s the one to beat today!",
        "🧭 You’re dialed into the big ones!"
    )


private fun getBigImprovementMessage(percent: Number): String {
        val messageTemplates = listOf(
            "💥 That’s a monster upgrade!",
            "📈 Huge bump — that’ll change the board!",
            "🎉 That’s a %.0f%% improvement — nice!",
            "💪 Crushing the smaller ones now!",
            "🔥 Boom! That’s a serious upgrade!",
            "🚀 That fish just lifted your average!",
            "🎯 Right on target — %.0f%% better!",
            "⚡ That's a power play!",
            "🎣 Reinforcements just arrived!",
            "🆙 %.0f%% boost — your team just leveled up!",
            "👊 That’ll shake things up — %.0f%% gain!",
            "🏹 Nailed it! That’s a %.0f%% improvement!"
        )
        val message = messageTemplates.random()
        return if (message.contains("%")) String.format(message, percent) else message
    }


    private val slowReturnMessages = listOf(
        "⏳ That took a while — glad you’re back!",
        "🐢 Slow and steady? Let’s pick up the pace!",
        "🕰️ That’s a comeback catch right there!",
        "🌥️ Took a break? You're back in it!",
        "🐌 We were starting to worry!",
        "📻 Long radio silence — now you’re back!",
        "🍀 Break’s over — lucky cast!",
        "🧭 Found them again, huh?",
        "🎯 Dialed back in!",
        "🛶 Sometimes you need to regroup!",
        "🌊 That one woke the lake up!",
        "🎬 Back on the board!"
    )


    private val finalCatchMessages = listOf(
        "🎯 You did it — full team locked in!",
        "🏁 That’s your final catch — time to cull!",
        "✅ All slots filled. Let’s see who stays!",
        "⚖️ Let the sorting begin!",
        "🔄 Now it is all about upgrades!",
        "🎒 Bag’s full — time to refine!",
        "🌟 Team is looking solid!",
        "🎲 It’s game time now!",
        "📊 Time to analyze and cull!",
        "🏹 Hit the target. Let's optimize!",
        "🧠 Now comes the strategy!"
    )

    private val generalMessages = listOf(
        "🎣 Nice! Keep that line wet!",
        "👏 Another one for the score board!",
        "💯 Keep stacking them!",
        "🔥 You’re in the groove now!",
        "🎉 Another step toward the win!",
        "🚣 Smooth sailing!",
        "🦅 Sharp cast, solid catch!",
        "🐟 That's how to play!",
        "🌅 Fishing like a pro!",
        "📸 There is one for the highlight reel!",
        "⚓ Locked in and hauling!",
        "🥳 Reel ’em in!",
        "🛠️ Adding to the masterpiece!",
        "💎 That one sparkles!",
        "🎵 You’re in rhythm now!",
        "🔄 Steady and strong!",
        "🥾 No wasted steps — solid catch!",
        "🌊 You’re making waves!",
        "🧃 Fresh pull!",
        "🎯 Right on mark!"
    )

    private fun getTwoRemainingMessage(remaining: Number): String {
        val messageTemplates = listOf(
            "🎯 Just $remaining left — almost weigh-in ready!",
            "⚙️ That bag's filling. $remaining more!",
            "🐟 Still some room for the picture hogs",
            "💡 You're setting up the perfect lineup!"
        )
        return messageTemplates.random()
    }

    private fun getOneRemainingMessage(remaining: Number): String {
        val messageTemplates = listOf(
            "🧨 $remaining more and you're ready to cull!",
            "🎒 Just $remaining fish from a full weigh-in bag!",
            "🦈 Perfect time to tag the next monster",
            "🚀 One final push — make it count!"
        )
        return messageTemplates.random()
    }




