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
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CatchEntryTournamentKgs : AppCompatActivity() {

    private lateinit var btnStartFishingKgs: Button
    private lateinit var btnSetUpTourKgs: Button
    private lateinit var btnMainKgs: Button
    private lateinit var btnAlarmKgs: Button


    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // UI Elements for Tournament Catch Weights
    private lateinit var firstRealWeightKgs:TextView
    private lateinit var secondRealWeightKgs:TextView
    private lateinit var thirdRealWeightKgs:TextView
    private lateinit var fourthRealWeightKgs:TextView
    private lateinit var fifthRealWeightKgs:TextView
    private lateinit var sixthRealWeightKgs:TextView

    private lateinit var firstDecWeightKgs:TextView
    private lateinit var secondDecWeightKgs:TextView
    private lateinit var thirdDecWeightKgs:TextView
    private lateinit var fourthDecWeightKgs:TextView
    private lateinit var fifthDecWeightKgs:TextView
    private lateinit var sixthDecWeightKgs:TextView

    private lateinit var totalRealWeightKgs: TextView
    private lateinit var totalDecWeightKgs: TextView

    private lateinit var txtTypeLetter1: TextView
    private lateinit var txtTypeLetter2: TextView
    private lateinit var txtTypeLetter3: TextView
    private lateinit var txtTypeLetter4: TextView
    private lateinit var txtTypeLetter5: TextView
    private lateinit var txtTypeLetter6: TextView

    private lateinit var dbHelper: CatchDatabaseHelper

    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"
    private val requestWeightENTRY = 1001
    private val requestAlarmSET = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_kgs)

        // ✅ Initialize TextViews

        firstRealWeightKgs= findViewById(R.id.firstRealWeightKgs)
        secondRealWeightKgs= findViewById(R.id.secondRealWeightKgs)
        thirdRealWeightKgs=findViewById(R.id.thirdRealWeightKgs)
        fourthRealWeightKgs= findViewById(R.id.fourthRealWeightKgs)
        fifthRealWeightKgs= findViewById(R.id.fifthRealWeightKgs)
        sixthRealWeightKgs= findViewById(R.id.sixthRealWeightKgs)


        firstDecWeightKgs= findViewById(R.id.firstDecWeightKgs)
        secondDecWeightKgs= findViewById(R.id.secondDecWeightKgs)
        thirdDecWeightKgs= findViewById(R.id.thirdDecWeightKgs)
        fourthDecWeightKgs= findViewById(R.id.fourthDecWeightKgs)
        fifthDecWeightKgs=  findViewById(R.id.fifthDecWeightKgs)
        sixthDecWeightKgs= findViewById(R.id.sixthDecWeightKgs)


        totalRealWeightKgs = findViewById(R.id.totalRealWeightKgs)
        totalDecWeightKgs = findViewById(R.id.totalDecWeightKgs)


        txtTypeLetter1= findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2= findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3= findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4= findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5= findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6= findViewById(R.id.txtTypeLetter6)


        clearTournamentTextViews()
        dbHelper = CatchDatabaseHelper(this)

        btnStartFishingKgs = findViewById(R.id.btnStartFishingKgs)
        btnMainKgs= findViewById(R.id.btnMainKgs)
        btnAlarmKgs = findViewById(R.id.btnAlarmKgs)


        // ✅ Retrieve intent data safely
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        updateTournamentList()

        btnStartFishingKgs.setOnClickListener {
            showWeightPopup()
        }

        btnMainKgs.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }

        btnAlarmKgs.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ############ END On Create  #################################


    /** Opens the weight entry popup */
    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourKgs::class.java)
        intent.putExtra("isTournament", true)

        // ✅ Ensure "Bass" tournament passes both Large Mouth & Small Mouth
        if (tournamentSpecies == "Large Mouth" || tournamentSpecies == "Small Mouth") {
            intent.putExtra("tournamentSpecies", "Bass") // ✅ Fix: Pass "Bass" instead of one species
        } else {
            intent.putExtra("tournamentSpecies", tournamentSpecies)
        }

        startActivityForResult(intent, requestWeightENTRY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestWeightENTRY -> {
                    val weightHundredthKg  = data?.getIntExtra("weightTotalHundredthKg", 0) ?: 0
                    val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
                    if (weightHundredthKg  > 0) {
                        saveTournamentCatch(weightHundredthKg , selectedSpecies)
                    }
                }
                requestAlarmSET -> {
                    val receivedHour = data?.getIntExtra("ALARM_HOURS", -1) ?: -1
                    val receivedMinute = data?.getIntExtra("ALARM_MINUTES", -1) ?: -1
                    if (receivedHour in 0..23 && receivedMinute in 0..59) {
                        alarmHour = receivedHour
                        alarmMinute = receivedMinute
                        btnAlarmKgs.text = String.format("Alarm: %02d:%02d", alarmHour, alarmMinute)
                    }
                }
            }
        }
    }

    private fun adjustTextViewVisibility() {
        if (tournamentCatchLimit == 4) {
            fifthRealWeightKgs.alpha = 0.5f
            fifthDecWeightKgs.alpha = 0.5f
            fifthRealWeightKgs.isEnabled = false
            fifthDecWeightKgs.isEnabled = false
            sixthRealWeightKgs.visibility = View.INVISIBLE
            sixthDecWeightKgs.visibility = View.INVISIBLE
        } else if (tournamentCatchLimit == 5) {
            sixthRealWeightKgs.alpha = 0.5f
            sixthDecWeightKgs.alpha = 0.5f
            sixthRealWeightKgs.isEnabled = false
            sixthDecWeightKgs.isEnabled = false
        } else {
            fifthRealWeightKgs.alpha = 1.0f
            fifthDecWeightKgs.alpha = 1.0f
            fifthRealWeightKgs.isEnabled = true
            fifthDecWeightKgs.isEnabled = true
            sixthRealWeightKgs.visibility = View.VISIBLE
            sixthDecWeightKgs.visibility = View.VISIBLE
            sixthRealWeightKgs.alpha = 1.0f
            sixthDecWeightKgs.alpha = 1.0f
            sixthRealWeightKgs.isEnabled = true
            sixthDecWeightKgs.isEnabled = true
        }
    }


    private fun saveTournamentCatch(weightHundredthKg: Int, species: String) {
        val speciesInitial = if (species == "Large Mouth") "L" else "S"

        Log.d("TournamentKgsDebug", "Attempting to Save Catch: Weight=${weightHundredthKg}, Species=${species}")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = null,
            totalLengthA8th = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = weightHundredthKg,
            catchType = "kgs",
            markerType = speciesInitial,
            clipColor = null
        )

        val result = dbHelper.insertCatch(catch) // Ensure `insertCatch` returns a value
        logAllTournamentCatches()
        updateTournamentList()
    }


    private fun logAllTournamentCatches() {
        val allCatches = dbHelper.getCatchesForToday(catchType = "totalWeightHundredthKg", todaysDate = "dateToday")
        Log.d("TournamentKgsDebug", "Fetched ${allCatches.size} tournament catches from DB")

        allCatches.forEach {
            Log.d("TournamentKgsDebug", "Tournament Catch: ${it.totalWeightHundredthKg} hundredth kg - ${it.species} (${it.markerType})")
        }
    }


    private fun updateTournamentList() {
        val allCatches = dbHelper.getCatchesForToday(catchType = "totalWeightHundredthKg", todaysDate = "dateToday")
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        val kgWeights = listOf(
            firstRealWeightKgs, secondRealWeightKgs, thirdRealWeightKgs,
            fourthRealWeightKgs, fifthRealWeightKgs, sixthRealWeightKgs
        )

        val DecWeightKgss = listOf(
            firstDecWeightKgs, secondDecWeightKgs, thirdDecWeightKgs,
            fourthDecWeightKgs, fifthDecWeightKgs, sixthDecWeightKgs
        )

        clearTournamentTextViews()

        runOnUiThread {
            for (i in tournamentCatches.indices) {
                val totalWeightHundredthKg = tournamentCatches[i].totalWeightHundredthKg ?: 0
                val weightKgs = totalWeightHundredthKg / 100
                val weightHundredths = totalWeightHundredthKg % 100
                val speciesInitial = tournamentCatches[i].markerType ?: "?"
                val clipColorName = tournamentCatches[i].clipColor

                kgWeights[i].text = "$weightKgs"
                DecWeightKgss[i].text = "$weightHundredths"
                //typeLetters[i].text = speciesInitial  ???????????????????????????????????????????????????????

                // ✅ Apply color dynamically
                val colorResId = resources.getIdentifier(clipColorName, "color", packageName)
                if (colorResId != 0) {
                    kgWeights[i].setBackgroundResource(colorResId)
                    DecWeightKgss[i].setBackgroundResource(colorResId)

                    // ✅ Ensure text color is white for blue backgrounds
                    if (clipColorName == "clip_blue") {
                        kgWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                        DecWeightKgss[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                    } else {
                        kgWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                        DecWeightKgss[i].setTextColor(resources.getColor(R.color.black, theme))
                    }
                }

                kgWeights[i].invalidate()
                DecWeightKgss[i].invalidate()
            }
        }
    }




    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        val totalWeightHundredthKg = tournamentCatches.sumOf { it.totalWeightHundredthKg ?: 0 }
        val totalKgs = totalWeightHundredthKg / 100
        val totalHundredths = totalWeightHundredthKg % 100

        totalRealWeightKgs.text = totalKgs.toString()
        totalDecWeightKgs.text = totalHundredths.toString()
    }

    private fun clearTournamentTextViews() {
        val textViews = listOf(
            firstRealWeightKgs, secondRealWeightKgs, thirdRealWeightKgs, fourthRealWeightKgs,
            fifthRealWeightKgs, sixthRealWeightKgs, firstDecWeightKgs, secondDecWeightKgs,
            thirdDecWeightKgs, fourthDecWeightKgs, fifthDecWeightKgs, sixthDecWeightKgs
        )

        textViews.forEach { it.text = "" }

        totalRealWeightKgs.text = "0"
        totalDecWeightKgs.text = "0"
    }

// +++++++++++++++++++++++++++++ ALARM  +++++++++++++++++++++++++++++++

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
                btnAlarmKgs.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
                isRed = !isRed
                flashHandler.postDelayed(this, 500)
            }
        }

        flashHandler.post(flashRunnable)

        handler.postDelayed({
            mediaPlayer?.stop()
            mediaPlayer?.release()
            btnAlarmKgs.setBackgroundColor(Color.TRANSPARENT)
            flashHandler.removeCallbacks(flashRunnable)
        }, 4000)
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
