package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryTournament : AppCompatActivity() {

    // Buttons
    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu: Button
    private lateinit var btnAlarm: Button
    private lateinit var btnResetCatch:Button

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

    // Total Weight TextViews
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
    private val requestWeightENTRY = 1001
    private val requestAlarmSET = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)

        // Initialize UI Components
        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnAlarm = findViewById(R.id.btnAlarm)

        // Weight and Species Tracking TextViews

        // ✅ Initialize TextViews
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
        txtTypeLetter2 =findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4= findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5= findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6= findViewById(R.id.txtTypeLetter6)


        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)


        // Retrieve Tournament Configurations
        // ✅ Retrieve intent data safely
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        // Setup Click Listeners
        btnTournamentCatch.setOnClickListener { showWeightPopup() }
        btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnAlarm.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourLbs::class.java)
        intent.putExtra("isTournament", true)

        // ```✅ Ensure "Bass" is passed correctly  ````````````````````````

        if (tournamentSpecies == "Large Mouth" || tournamentSpecies == "Small Mouth") {
            intent.putExtra("tournamentSpecies", "Bass") // ✅ Fix: Pass "Bass" instead of a single species
        } else {
            intent.putExtra("tournamentSpecies", tournamentSpecies)
        }

        startActivity(intent)
    }
// ------------- REACT TO RESULTS  -------------------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestWeightENTRY -> {
                    val weightTotalOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
                    val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
                    if (weightTotalOz > 0) {
                        saveTournamentCatch(weightTotalOz, selectedSpecies)
                    }
                }
                requestAlarmSET -> {
                    val receivedHour = data?.getIntExtra("ALARM_HOURS", -1) ?: -1
                    val receivedMinute = data?.getIntExtra("ALARM_MINUTES", -1) ?: -1
                    if (receivedHour in 0..23 && receivedMinute in 0..59) {
                        alarmHour = receivedHour
                        alarmMinute = receivedMinute
                        btnAlarm.text = String.format("Alarm: %02d:%02d", alarmHour, alarmMinute)
                    }
                }
            }
        }
    }


    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^

    private fun saveTournamentCatch(totalWeightOz: Int, bassType: String) {
        val colorList = listOf("clip_red", "clip_yellow", "clip_green", "clip_blue", "clip_white", "clip_orange")

        // Get the next color in sequence based on total stored catches
        val existingCatches = dbHelper.getCatchesForToday("lbsOzs", todayDate = "todayDate").size
        val assignedColor = colorList[existingCatches % colorList.size] // Cycle through colors
        val speciesInitial = if (bassType == "Large Mouth") "L" else "Small Mouth"
        val weightLbs = totalWeightOz / 16
        val weightOz = totalWeightOz % 16

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = tournamentSpecies,
            totalWeightOz = totalWeightOz,
            totalLengthA8th = null,
            totalWeightHundredthKg = null,
            lengthDecimalTenthCm = null,
            catchType = measurementSystem,
            markerType = bassType,
            clipColor = assignedColor
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
        updateTournamentList()
    }



//################## UPDATE TOURNAMENT LIST   ###################################

    private fun updateTournamentList() {
        val allCatches = dbHelper.getCatchesForToday(catchType = "LbsOzs", todayDate = "dateToday")
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
                val totalWeightOz = tournamentCatches[i].totalWeightOz ?: 0
                val weightLbs = totalWeightOz / 16
                val weightOz = totalWeightOz % 16
                val clipColorName = tournamentCatches[i].clipColor

                // ✅ Debugging Log
                println("DEBUG: Catch #$i -> Color: $clipColorName | Lbs: $weightLbs | Oz: $weightOz")

                realWeights[i].text = weightLbs.toString()
                decWeights[i].text = weightOz.toString()

                // ✅ Apply color dynamically
                val colorResId = resources.getIdentifier(clipColorName, "color", packageName)
                if (colorResId != 0) {
                    realWeights[i].setBackgroundResource(colorResId)
                    decWeights[i].setBackgroundResource(colorResId)

                    // ✅ Ensure text color is white for blue backgrounds
                    if (clipColorName == "clip_blue") {
                        realWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                        decWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                    } else {
                        realWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                        decWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                    }
                }

                realWeights[i].invalidate()
                decWeights[i].invalidate()
            }
        }


        updateTotalWeight(tournamentCatches)
        adjustTextViewVisibility()




    }

    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        val totalWeightOz = tournamentCatches.sumOf { it.totalWeightOz ?: 0 }
        val totalLbs = totalWeightOz / 16
        val totalOz = totalWeightOz % 16

        // 🔍 Debugging Log
        println("DEBUG: Total Weight -> Total OZ: $totalWeightOz | Lbs: $totalLbs | Oz: $totalOz")

        totalRealWeight.text = totalLbs.toString()
        totalDecWeight.text = totalOz.toString()
    }

    private fun clearTournamentTextViews() {
        val textViews = listOf(
            firstRealWeight, secondRealWeight, thirdRealWeight, fourthRealWeight,
            fifthRealWeight, sixthRealWeight, firstDecWeight, secondDecWeight,
            thirdDecWeight, fourthDecWeight, fifthDecWeight, sixthDecWeight
        )

        textViews.forEach { it.text = "" }

        totalRealWeight.text = "0"
        totalDecWeight.text = "0"
    }

    private fun adjustTextViewVisibility() {
        if (tournamentCatchLimit == 4) {
            fifthRealWeight.alpha = 0.5f
            fifthDecWeight.alpha = 0.5f
            fifthRealWeight.isEnabled = false
            fifthDecWeight.isEnabled = false
            sixthRealWeight.visibility = View.INVISIBLE
            sixthDecWeight.visibility = View.INVISIBLE
        } else if (tournamentCatchLimit == 5) {
            sixthRealWeight.alpha = 0.5f
            sixthDecWeight.alpha = 0.5f
            sixthRealWeight.isEnabled = false
            sixthDecWeight.isEnabled = false
        } else {
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


    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
