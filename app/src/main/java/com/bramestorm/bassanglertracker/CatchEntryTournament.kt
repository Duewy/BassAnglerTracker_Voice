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
    private lateinit var btnAlarm: Button
    private lateinit var btnResetCatch: Button

    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1

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


    private lateinit var totalRealWeight: TextView
    private lateinit var totalDecWeight: TextView

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

    private val weightEntryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val weightTotalOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
            val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""

            Log.d("DB_DEBUG", "✅ Received weightTotalOz: $weightTotalOz, selectedSpecies: $selectedSpecies")

            if (weightTotalOz > 0) {
                saveTournamentCatch(weightTotalOz, selectedSpecies)
            } else {
                Log.e("DB_DEBUG", "⚠️ Invalid weight! Catch not saved.")
            }
        } else {
            Log.e("DB_DEBUG", "⚠️ Activity Result was CANCELLED or ERROR")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
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

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 =findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4= findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5= findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6= findViewById(R.id.txtTypeLetter6)
        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)

        btnResetCatch = findViewById(R.id.btnResetCatch)

        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        btnTournamentCatch.setOnClickListener { showWeightPopup() }
        btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnAlarm.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }
        val dbHelper = CatchDatabaseHelper(this)
        btnResetCatch.setOnClickListener {
            dbHelper.setTournamentResetPoint()
            Toast.makeText(this, "Tournament Reset! Only new catches will count.", Toast.LENGTH_SHORT).show()
            updateTournamentList()
        }

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourLbs::class.java)
        intent.putExtra("isTournament", true)

        if (tournamentSpecies == "Large Mouth" || tournamentSpecies == "Small Mouth") {
            intent.putExtra("tournamentSpecies", "Bass")
        } else {
            intent.putExtra("tournamentSpecies", tournamentSpecies)
        }
        weightEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalOz: Int, bassType: String) {
        val formattedDate = getCurrentDateTime()

        val allCatches = dbHelper.getCatchesForToday("LbsOzs", getCurrentDate())
        val tournamentCatches = if (isCullingEnabled) allCatches.take(tournamentCatchLimit) else allCatches
        val usedColors = tournamentCatches.mapNotNull { it.clipColor }
            .mapNotNull { colorName ->
                try { ClipColor.valueOf(colorName.uppercase()) } catch (e: IllegalArgumentException) { null }
            }.toSet()
        val assignedColor = ClipColor.getAvailableColor(usedColors)

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: ${assignedColor.name}")

        val speciesInitial = if (bassType == "Large Mouth") "L" else "S"

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
            clipColor = assignedColor.name.uppercase()
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
        updateTournamentList()
    }

    // ``````````````` UPDATE TOTAL WEIGHT ``````````````````````
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        val totalWeightOz = tournamentCatches.sumOf { it.totalWeightOz ?: 0 }
        val totalLbs = totalWeightOz / 16
        val totalOz = totalWeightOz % 16
        totalRealWeight.text = totalLbs.toString()
        totalDecWeight.text = totalOz.toString()
    }

    //################## UPDATE TOURNAMENT LIST   ###################################

    private fun updateTournamentList() {
        val formattedDate = getCurrentDate()
        val usedColors = mutableSetOf<ClipColor>()
        val assignedColorMap = mutableMapOf<Int, ClipColor>()
        val allCatches = dbHelper.getCatchesForToday(catchType = "LbsOzs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        val realWeights = listOf(
            firstRealWeight, secondRealWeight, thirdRealWeight,
            fourthRealWeight, fifthRealWeight, sixthRealWeight
        )

        val decWeights = listOf(
            firstDecWeight, secondDecWeight, thirdDecWeight,
            fourthDecWeight, fifthDecWeight, sixthDecWeight
        )

        clearTournamentTextViews()

        runOnUiThread {
            for (i in tournamentCatches.indices) {
                if (i >= realWeights.size || i >= decWeights.size) {
                    Log.e("DB_DEBUG", "⚠️ Skipping index $i - Out of bounds for weight lists.")
                    continue // Skip if out of bounds
                }

                val catch = tournamentCatches[i]
                val totalWeightOz = catch.totalWeightOz ?: 0
                val weightLbs = totalWeightOz / 16
                val weightOz = totalWeightOz % 16

                // ✅ Retrieve the correct clip color from the database (instead of reassigning!)
                val clipColorName = catch.clipColor?.uppercase() ?: "RED"
                val clipColor = try {
                    ClipColor.valueOf(clipColorName)
                } catch (e: IllegalArgumentException) {
                    ClipColor.RED // Default to RED if the database value is incorrect
                }

                usedColors.add(clipColor)  // Track used colors
                assignedColorMap[catch.id] = clipColor // Store the assigned color

                Log.d("DB_DEBUG", "🎨 Assigned Color: ${clipColor.name} for Catch ID: ${catch.id}")

                realWeights[i].text = weightLbs.toString()
                decWeights[i].text = weightOz.toString()

                // ✅ Apply background color properly
                realWeights[i].setBackgroundResource(clipColor.resId)
                decWeights[i].setBackgroundResource(clipColor.resId)

                // ✅ Ensure proper text color for readability
                if (clipColor == ClipColor.BLUE) {
                    realWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                    decWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                } else {
                    realWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                    decWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                }

                realWeights[i].invalidate()
                decWeights[i].invalidate()
            }

        }

        updateTotalWeight(tournamentCatches)
        adjustTextViewVisibility()
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

        companion object {
            fun getAvailableColor(usedColors: Set<ClipColor>): ClipColor {
                val availableColors = entries.filter { it !in usedColors }
                return availableColors.firstOrNull() ?: entries[usedColors.size % entries.size]
            }
        }
    }


    // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for culling values ~~~~~~~~~~~~~
    private fun adjustTextViewVisibility() {
        when (tournamentCatchLimit) {
            4 -> {
                fifthRealWeight.alpha = 0.5f
                fifthDecWeight.alpha = 0.5f
                fifthRealWeight.isEnabled = false
                fifthDecWeight.isEnabled = false
                sixthRealWeight.visibility = View.INVISIBLE
                sixthDecWeight.visibility = View.INVISIBLE
            }
            5 -> {
                sixthRealWeight.alpha = 0.5f
                sixthDecWeight.alpha = 0.5f
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


    // +++++++++++++++++ CHECK ALARM ++++++++++++++++++++++++

    private val checkAlarmRunnable = object : Runnable {
        override fun run() {
            val calendar = Calendar.getInstance()
            if (calendar.get(Calendar.HOUR_OF_DAY) == alarmHour && calendar.get(Calendar.MINUTE) == alarmMinute) {
                startAlarm()
            }
            handler.postDelayed(this, 60000)
        }
    }
    // Start Alarm
    private fun startAlarm() {
        // ✅ Ensure raw file exists
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound) // Change to actual filename
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
}
