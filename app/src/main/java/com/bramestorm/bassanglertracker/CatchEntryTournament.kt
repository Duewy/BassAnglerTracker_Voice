package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CatchEntryTournament : AppCompatActivity() {

    // Buttons
    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu: Button
    private lateinit var btnMainPg:Button
    private lateinit var btnAlarm: Button


    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var alarmTriggered: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // Weight Display TextViews
    private lateinit var firstRealWeight: TextView
    private lateinit var secondRealWeight: TextView
    private lateinit var thirdRealWeight: TextView
    private lateinit var fourthRealWeight: TextView
    private lateinit var fifthRealWeight: TextView
    private lateinit var sixthRealWeight: TextView

    private lateinit var firstDecWeight: TextView
    private lateinit var secondDecWeight: TextView
    private lateinit var thirdDecWeight: TextView
    private lateinit var fourthDecWeight: TextView
    private lateinit var fifthDecWeight: TextView
    private lateinit var sixthDecWeight: TextView

    private lateinit var decimalTextViews: List<TextView>
    private lateinit var speciesLetters: List<TextView>

    private lateinit var txtTypeLetter1:TextView
    private lateinit var txtTypeLetter2:TextView
    private lateinit var txtTypeLetter3:TextView
    private lateinit var txtTypeLetter4:TextView
    private lateinit var txtTypeLetter5:TextView
    private lateinit var txtTypeLetter6:TextView

    private lateinit var txtColorLetter1:TextView
    private lateinit var txtColorLetter2:TextView
    private lateinit var txtColorLetter3:TextView
    private lateinit var txtColorLetter4:TextView
    private lateinit var txtColorLetter5:TextView
    private lateinit var txtColorLetter6:TextView


    private lateinit var totalRealWeight: TextView
    private lateinit var totalDecWeight: TextView

    private var availableClipColors: List<ClipColor> = emptyList()


    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper

    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"

    // Request Codes
    private val requestWeightENTRY = 1005
    private val requestAlarmSET = 1006


    // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
        private val weightEntryLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val weightTotalOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
                val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
                val clipColor = data?.getStringExtra("clip_color") ?: ""

                Log.d("DB_DEBUG", "✅ Received weightTotalOz: $weightTotalOz, selectedSpecies: $selectedSpecies, clip_color: $clipColor")

                if (weightTotalOz > 0) {
                    saveTournamentCatch(weightTotalOz, selectedSpecies, clipColor)
                }
            }
        }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnMainPg = findViewById(R.id.btnMainPg)
        btnAlarm = findViewById(R.id.btnAlarm)

        // Assign TextViews
        firstRealWeight = findViewById(R.id.firstRealWeight)
        secondRealWeight = findViewById(R.id.secondRealWeight)
        thirdRealWeight = findViewById(R.id.thirdRealWeight)
        fourthRealWeight = findViewById(R.id.fourthRealWeight)
        fifthRealWeight = findViewById(R.id.fifthRealWeight)
        sixthRealWeight = findViewById(R.id.sixthRealWeight)

        firstDecWeight = findViewById(R.id.firstDecWeight)
        secondDecWeight = findViewById(R.id.secondDecWeight)
        thirdDecWeight = findViewById(R.id.thirdDecWeight)
        fourthDecWeight = findViewById(R.id.fourthDecWeight)
        fifthDecWeight = findViewById(R.id.fifthDecWeight)
        sixthDecWeight = findViewById(R.id.sixthDecWeight)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetter6)

        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)

        txtColorLetter1 = findViewById(R.id.txtColorLetter1)
        txtColorLetter2 = findViewById(R.id.txtColorLetter2)
        txtColorLetter3 = findViewById(R.id.txtColorLetter3)
        txtColorLetter4 = findViewById(R.id.txtColorLetter4)
        txtColorLetter5 = findViewById(R.id.txtColorLetter5)
        txtColorLetter6 = findViewById(R.id.txtColorLetter6)



        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        btnTournamentCatch.setOnClickListener { showWeightPopup() }
        btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainPg.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }
        btnAlarm.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }
        val dbHelper = CatchDatabaseHelper(this)

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourLbs::class.java)
        intent.putExtra("isTournament", true)

        if (tournamentSpecies == "Large Mouth") {
            intent.putExtra("tournamentSpecies", "Large Mouth Bass")
        } else         if (tournamentSpecies == "Small Mouth") {
            intent.putExtra("tournamentSpecies", "Small Mouth Bass")
        } else{
            intent.putExtra("tournamentSpecies", tournamentSpecies)
        }

        // 🔥 Send available clip colors as String array
        val colorNames = availableClipColors.map { it.name }.toTypedArray()
        intent.putExtra("availableClipColors", colorNames)

        weightEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalOz: Int, bassType: String, clipColor: String) {
        val availableColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "LbsOzs",
            date = getCurrentDate(),
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )
        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val speciesInitial = if (bassType == "Large Mouth") "L" else "S"

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = bassType,
            totalWeightOz = weightTotalOz,
            totalLengthA8th = null,
            totalWeightHundredthKg = null,
            totalLengthTenths = null,
            catchType = "LbsOzs",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()


        updateTournamentList()
    }

    private fun getTopTournamentCatches(): List<CatchItem> {
        val allCatches = dbHelper.getCatchesForToday("LbsOzs", getCurrentDate())
        val sorted = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
        return if (isCullingEnabled) sorted.take(tournamentCatchLimit) else sorted
    }


    // ``````````````` UPDATE TOTAL WEIGHT ``````````````````````
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalWeightOz ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalWeightOz = catchesToUse.sumOf { it.totalWeightOz ?: 0 }
        val totalLbs = totalWeightOz / 16
        val totalOz = totalWeightOz % 16

        totalRealWeight.text = totalLbs.toString()
        totalDecWeight.text = totalOz.toString()

        // !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
        val currentCount = dbHelper
            .getCatchesForToday("LbsOzs", getCurrentDate())
            .sortedByDescending { it.totalWeightOz ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            val message = getMotivationalMessage(currentCount, tournamentCatchLimit)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }



    //################## UPDATE TOURNAMENT LIST   ###################################

    private fun updateTournamentList() {
        val formattedDate = getCurrentDate()
        val realWeights = listOf(
            firstRealWeight, secondRealWeight, thirdRealWeight,
            fourthRealWeight, fifthRealWeight, sixthRealWeight
        )

        val decWeights = listOf(
            firstDecWeight, secondDecWeight, thirdDecWeight,
            fourthDecWeight, fifthDecWeight, sixthDecWeight
        )
        val allCatches = dbHelper.getCatchesForToday(catchType = "LbsOzs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        // ✨ Clean way to update available clip colors
        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "LbsOzs",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )

        Log.d("CLIP_COLOR", "🎨 Available Colors: $availableClipColors")

        clearTournamentTextViews()

        runOnUiThread {
            for (i in tournamentCatches.indices) {
                if (i >= realWeights.size || i >= decWeights.size) continue

                val catch = tournamentCatches[i]
                val totalWeightOz = catch.totalWeightOz ?: 0
                val weightLbs = totalWeightOz / 16
                val weightOz = totalWeightOz % 16

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realWeights[i].text = weightLbs.toString()
                decWeights[i].text = weightOz.toString()

                realWeights[i].setBackgroundResource(clipColor.resId)
                decWeights[i].setBackgroundResource(clipColor.resId)

                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realWeights[i].setTextColor(textColor)
                decWeights[i].setTextColor(textColor)

                realWeights[i].invalidate()
                decWeights[i].invalidate()
            }
            // 👇 ADD THIS FOR COLORBLIND ACCESSIBILITY 👇
            val colorLetters = listOf(
                txtColorLetter1, txtColorLetter2, txtColorLetter3,
                txtColorLetter4, txtColorLetter5, txtColorLetter6
            )

            colorLetters.forEach { it.text = "" } // Clear first

            for (i in tournamentCatches.indices) {
                if (i >= tournamentCatchLimit || i >= colorLetters.size) break

                val catch = tournamentCatches[i]
                val colorInitial = when (catch.clipColor?.uppercase()) {
                    "BLUE" -> "B"
                    "RED" -> "R"
                    "GREEN" -> "G"
                    "YELLOW" -> "Y"
                    "ORANGE" -> "O"
                    "WHITE" -> "W"
                    else -> "?"
                }
                colorLetters[i].text = colorInitial

                val speciesCode = getSpeciesCode(catch.species ?: "")
                when (i) {
                    0 -> txtTypeLetter1.text = speciesCode
                    1 -> txtTypeLetter2.text = speciesCode
                    2 -> txtTypeLetter3.text = speciesCode
                    3 -> txtTypeLetter4.text = speciesCode
                    4 -> txtTypeLetter5.text = speciesCode
                    5 -> txtTypeLetter6.text = speciesCode
                }

        }

    }

        updateTotalWeight(tournamentCatches)
        adjustTextViewVisibility()

        Handler(Looper.getMainLooper()).postDelayed({
            when (tournamentCatchLimit) {
                4 -> {
                    blinkTextViewTwice(fourthRealWeight)
                    blinkTextViewTwice(fourthDecWeight)
                }
                5 -> {
                    blinkTextViewTwice(fifthRealWeight)
                    blinkTextViewTwice(fifthDecWeight)
                }
                6 -> {
                    blinkTextViewTwice(sixthRealWeight)
                    blinkTextViewTwice(sixthDecWeight)
                }
            }
        }, 300) // Wait


    }


    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {
        firstRealWeight.text = ""
        secondRealWeight.text = ""
        thirdRealWeight.text = ""
        fourthRealWeight.text = ""
        fifthRealWeight.text = ""
        sixthRealWeight.text = ""

        firstDecWeight.text = ""
        secondDecWeight.text = ""
        thirdDecWeight.text = ""
        fourthDecWeight.text = ""
        fifthDecWeight.text = ""
        sixthDecWeight.text = ""

        totalRealWeight.text = "0"
        totalDecWeight.text = "0"
    }

    // %%%%%%%%%%%% Clip Color assignment  %%%%%%%%%%%%%%%%%%%%%%%

    enum class ClipColor(val resId: Int) {
        RED(R.color.clip_red),
        BLUE(R.color.clip_blue),
        GREEN(R.color.clip_green),
        YELLOW(R.color.clip_yellow),
        ORANGE(R.color.clip_orange),
        WHITE(R.color.clip_white);
    }

    //????????????? AVAILABLE COLORS   ???????????????????????
    private fun calculateAvailableClipColors(
        dbHelper: CatchDatabaseHelper,
        catchType: String,
        date: String,
        tournamentCatchLimit: Int,
        isCullingEnabled: Boolean
    ): List<ClipColor> {
        val allCatches = dbHelper.getCatchesForToday(catchType, date)
        val sorted = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
        val topCatches = sorted.take(tournamentCatchLimit) // ✅ Always limit to top N

        val usedColors = topCatches.mapNotNull { it.clipColor }
            .mapNotNull {
                try { ClipColor.valueOf(it.uppercase()) } catch (_: Exception) { null }
            }
            .toSet()

        return ClipColor.entries.filter { it !in usedColors }
    }



    // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for culling values ~~~~~~~~~~~~~
    private fun adjustTextViewVisibility() {
        when (tournamentCatchLimit) {
            4 -> {
                fifthRealWeight.alpha = 0.3f
                fifthDecWeight.alpha = 0.3f
                fifthRealWeight.isEnabled = false
                fifthDecWeight.isEnabled = false
                sixthRealWeight.visibility = View.INVISIBLE
                sixthDecWeight.visibility = View.INVISIBLE
            }
            5 -> {
                sixthRealWeight.alpha = 0.3f
                sixthDecWeight.alpha = 0.3f
                sixthRealWeight.isEnabled = false
                sixthDecWeight.isEnabled = false
                txtTypeLetter6.isEnabled = false
            }
            else -> {
                fifthRealWeight.alpha = 1.0f
                fifthDecWeight.alpha = 1.0f
                fifthRealWeight.isEnabled = true
                fifthDecWeight.isEnabled = true
                sixthRealWeight.visibility = View.VISIBLE
                sixthDecWeight.visibility = View.VISIBLE
                sixthRealWeight.alpha = 1.0f
                sixthDecWeight.alpha = 1.0f
                sixthRealWeight.isEnabled = true
                sixthDecWeight.isEnabled = true
            }
        }
    }

    //!!!!!!!!!!!!!!!! Get SPECIES Letter !!!!!!!!!!!!!!!!!

    private fun getSpeciesCode(species: String): String {
        return when (species.uppercase()) {
            "LARGE MOUTH" -> "LM"
            "SMALL MOUTH" -> "SM"
            "WALLEYE"     -> "WE"
            "PIKE"        -> "PK"
            "PERCH"       -> "PH"
            "PANFISH"     -> "PF"
            "CATFISH"     -> "CF"
            "CRAPPIE"     -> "CP"
            else          -> "--"
        }
    }

//!!!!!!!!!!!!!!!!! MOTIVATIONAL MESSAGES !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private fun getMotivationalMessage(currentCount: Int, totalNeeded: Int): String {
        val remaining = totalNeeded - currentCount

        val messages = listOf(
            "🔥 You're on fire!",
            "🎣 Keep casting, you're almost there!",
            "💪 One catch at a time!",
            "🏆 That one’s a game-changer!",
            "👏 Nice pull! You're stacking ‘em!",
            "🚀 You're climbing that leaderboard!",
            "🌊 The lake is yours today!",
            "💯 Crushing it, keep going!",
            "🎉 You’ve got $currentCount so far — only $remaining to go!",
            "💥 That puts you at $currentCount — get that next one!"
        )

        return messages.random()
    }


    // +++++++++++++++++ CHECK ALARM ++++++++++++++++++++++++

    private val checkAlarmRunnable = object : Runnable {
        override fun run() {
            val calendar = Calendar.getInstance()
            val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
            val nowMinute = calendar.get(Calendar.MINUTE)

            Log.d("ALARM_DEBUG", "🕒 Checking alarm... Now: $nowHour:$nowMinute, Set: $alarmHour:$alarmMinute")

            if (!alarmTriggered && nowHour == alarmHour && nowMinute == alarmMinute) {
                alarmTriggered = true
                Log.d("ALARM_DEBUG", "🔔 Alarm triggered!")
                startAlarm()
            }

            handler.postDelayed(this, 60000)  // ✅ every 60 seconds now
        }
    }


    // Start Alarm
    private fun startAlarm() {
        // ✅ Ensure raw file exists
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.start()

        val flashHandler = Handler()
        var isRed = true
        val flashRunnable = object : Runnable {
            override fun run() {
                btnAlarm.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
                isRed = !isRed
                flashHandler.postDelayed(this, 500)
            }
        }

        flashHandler.post(flashRunnable)

        handler.postDelayed({
            mediaPlayer?.stop()
            mediaPlayer?.release()
            btnAlarm.setBackgroundColor(Color.TRANSPARENT)
            flashHandler.removeCallbacks(flashRunnable)
        }, 4000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("ALARM_DEBUG", "📥 onActivityResult triggered with requestCode=$requestCode")

        if (requestCode == requestAlarmSET && resultCode == Activity.RESULT_OK) {
            alarmHour = data?.getIntExtra("ALARM_HOUR", -1) ?: -1
            alarmMinute = data?.getIntExtra("ALARM_MINUTE", -1) ?: -1
            alarmTriggered = false // ✅ reset so the alarm can trigger again

            Log.d("ALARM_DEBUG", "✅ Alarm Set - hour=$alarmHour, minute=$alarmMinute")

            if (alarmHour != -1 && alarmMinute != -1) {
                val amPm = if (alarmHour >= 12) "PM" else "AM"
                val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
                val formattedMinute = String.format("%02d", alarmMinute)

                val timeString = "$displayHour:$formattedMinute $amPm"
                btnAlarm.text = "⏰ $timeString"

                Toast.makeText(this, "Alarm Set for $timeString", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //++++++++++++++++ Date and Time  +++++++++++++++++++++++++++++
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    //************** DATE *****************************
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // @@@@@@@@@@@@@ BLINK  @@@@@@@@@@@@@@@@@@@@@@@@@
    private fun blinkTextViewTwice(textView: TextView) {
        val blink = AnimationUtils.loadAnimation(this, R.anim.blink)

        // Delay 1 second before first blink
        Handler(Looper.getMainLooper()).postDelayed({
            textView.startAnimation(blink)

            // Delay slightly before doing the second blink
            Handler(Looper.getMainLooper()).postDelayed({
                textView.startAnimation(blink)
            }, 700) // Wait ~1 blink duration
        }, 1000) // Initial 1 second delay
    }


}//################## END  ################################