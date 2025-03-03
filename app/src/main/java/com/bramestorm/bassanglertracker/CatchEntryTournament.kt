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

    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // Weight Display TextViews
    private lateinit var weightTextViews: List<TextView>
    private lateinit var decimalTextViews: List<TextView>
    private lateinit var speciesLetters: List<TextView>

    // Total Weight TextViews
    private lateinit var totalRealWeight: TextView
    private lateinit var totalDecWeight: TextView

    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper

    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
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
        weightTextViews = listOf(
            findViewById(R.id.firstRealWeight), findViewById(R.id.secondRealWeight),
            findViewById(R.id.thirdRealWeight), findViewById(R.id.fourthRealWeight),
            findViewById(R.id.fifthRealWeight), findViewById(R.id.sixthRealWeight)
        )

        decimalTextViews = listOf(
            findViewById(R.id.firstDecWeight), findViewById(R.id.secondDecWeight),
            findViewById(R.id.thirdDecWeight), findViewById(R.id.fourthDecWeight),
            findViewById(R.id.fifthDecWeight), findViewById(R.id.sixthDecWeight)
        )

        speciesLetters = listOf(
            findViewById(R.id.txtTypeLetter1), findViewById(R.id.txtTypeLetter2),
            findViewById(R.id.txtTypeLetter3), findViewById(R.id.txtTypeLetter4),
            findViewById(R.id.txtTypeLetter5), findViewById(R.id.txtTypeLetter6)
        )

        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)

        // Retrieve Tournament Configurations
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
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

    /** Opens the weight entry popup */
    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntry::class.java)
        intent.putExtra("isTournament", true)
        intent.putExtra("catchType", if (measurementSystem == "weight") "lbsOzs" else "kgs")
        intent.putExtra("selectedSpecies", tournamentSpecies)
        startActivityForResult(intent, requestWeightENTRY)
    }

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

    private fun saveTournamentCatch(totalWeightOz: Int, species: String) {
        val weightLbs = totalWeightOz / 16
        val weightOz = totalWeightOz % 16
        val speciesInitial = if (species == "Large Mouth") "L" else "S"

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = totalWeightOz,
            totalLengthA8th = null,
            lengthDecimalTenthCm = null,
            totalWeightHundredthKg = null,
            catchType = "Lbs",
            markerType = speciesInitial,
            clipColor = null
        )


        dbHelper.insertCatch(catch)
        updateTournamentList()
    }

    private fun updateTournamentList() {
        val allCatches = dbHelper.getAllCatches().sortedByDescending { it.totalWeightOz ?: 0 }
        val catchesToShow = if (isCullingEnabled) allCatches.take(tournamentCatchLimit) else allCatches

        for (i in catchesToShow.indices) {
            if (i >= weightTextViews.size) break
            val weightOz = catchesToShow[i].totalWeightOz ?: 0
            weightTextViews[i].text = "${weightOz / 16} Lbs"
            decimalTextViews[i].text = "${weightOz % 16} oz"
            speciesLetters[i].text = catchesToShow[i].markerType ?: "?"
        }
    }

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
