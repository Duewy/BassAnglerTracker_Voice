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

class CatchEntryTournamentKgs : AppCompatActivity() {

    private lateinit var btnKgsStartFishing: Button
    private lateinit var btnMenu: Button
    private lateinit var btnKgsAlarm: Button
    private lateinit var btnReSetData:Button

    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // UI Elements for Tournament Catch Weights
    private lateinit var firstKgWeight:TextView
    private lateinit var secondKgWeight:TextView
    private lateinit var thirdKgWeight:TextView
    private lateinit var fourthKgWeight:TextView
    private lateinit var fifthKgWeight:TextView
    private lateinit var sixthKgWeight:TextView

    private lateinit var firstGramWeight:TextView
    private lateinit var secondGramWeight:TextView
    private lateinit var thirdGramWeight:TextView
    private lateinit var fourthGramWeight:TextView
    private lateinit var fifthGramWeight:TextView
    private lateinit var sixthGramWeight:TextView

    private lateinit var totalKgWeight: TextView
    private lateinit var totalGramWeight: TextView

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

            firstKgWeight= findViewById(R.id.firstKgWeight)
            secondKgWeight= findViewById(R.id.secondKgWeight)
            thirdKgWeight=findViewById(R.id.thirdKgWeight)
            fourthKgWeight= findViewById(R.id.fourthKgWeight)
            fifthKgWeight= findViewById(R.id.fifthKgWeight)
            sixthKgWeight= findViewById(R.id.sixthKgWeight)


        firstGramWeight= findViewById(R.id.firstGramWeight)
        secondGramWeight= findViewById(R.id.secondGramWeight)
        thirdGramWeight= findViewById(R.id.thirdGramWeight)
        fourthGramWeight= findViewById(R.id.fourthGramWeight)
        fifthGramWeight=  findViewById(R.id.fifthGramWeight)
        sixthGramWeight= findViewById(R.id.sixthGramWeight)


        totalKgWeight = findViewById(R.id.totalKgWeight)
        totalGramWeight = findViewById(R.id.totalGramWeight)


            txtTypeLetter1= findViewById(R.id.txtTypeLetter1)
            txtTypeLetter2= findViewById(R.id.txtTypeLetter2)
            txtTypeLetter3= findViewById(R.id.txtTypeLetter3)
            txtTypeLetter4= findViewById(R.id.txtTypeLetter4)
            txtTypeLetter5= findViewById(R.id.txtTypeLetter5)
            txtTypeLetter6= findViewById(R.id.txtTypeLetter6)


        clearTournamentTextViews()
        dbHelper = CatchDatabaseHelper(this)

        btnKgsStartFishing = findViewById(R.id.btnKgsStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnKgsAlarm = findViewById(R.id.btnKgsAlarm)


        // ✅ Retrieve intent data safely
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        updateTournamentList()

        btnKgsStartFishing.setOnClickListener {
            showWeightPopup()
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }

        btnKgsAlarm.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

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
                            btnKgsAlarm.text = String.format("Alarm: %02d:%02d", alarmHour, alarmMinute)
                        }
                    }
                }
            }
        }

        private fun adjustTextViewVisibility() {
            if (tournamentCatchLimit == 4) {
                fifthKgWeight.alpha = 0.5f
                fifthGramWeight.alpha = 0.5f
                fifthKgWeight.isEnabled = false
                fifthGramWeight.isEnabled = false
                sixthKgWeight.visibility = View.INVISIBLE
                sixthGramWeight.visibility = View.INVISIBLE
            } else if (tournamentCatchLimit == 5) {
                sixthKgWeight.alpha = 0.5f
                sixthGramWeight.alpha = 0.5f
                sixthKgWeight.isEnabled = false
                sixthGramWeight.isEnabled = false
            } else {
                fifthKgWeight.alpha = 1.0f
                fifthGramWeight.alpha = 1.0f
                fifthKgWeight.isEnabled = true
                fifthGramWeight.isEnabled = true
                sixthKgWeight.visibility = View.VISIBLE
                sixthGramWeight.visibility = View.VISIBLE
                sixthKgWeight.alpha = 1.0f
                sixthGramWeight.alpha = 1.0f
                sixthKgWeight.isEnabled = true
                sixthGramWeight.isEnabled = true
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
                lengthDecimalTenthCm = null,
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
            val allCatches = dbHelper.getAllCatches().filter { it.catchType == "tournamentkgs" }
            Log.d("TournamentKgsDebug", "Fetched ${allCatches.size} tournament catches from DB")

            allCatches.forEach {
                Log.d("TournamentKgsDebug", "Tournament Catch: ${it.totalWeightHundredthKg} hundredth kg - ${it.species} (${it.markerType})")
            }
        }


        private fun updateTournamentList() {
        val allCatches = dbHelper.getAllCatches()
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        val kgWeights = listOf(
            firstKgWeight, secondKgWeight, thirdKgWeight,
            fourthKgWeight, fifthKgWeight, sixthKgWeight
        )

        val gramWeights = listOf(
            firstGramWeight, secondGramWeight, thirdGramWeight,
            fourthGramWeight, fifthGramWeight, sixthGramWeight
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
                gramWeights[i].text = "$weightHundredths"
                //typeLetters[i].text = speciesInitial  ???????????????????????????????????????????????????????

                // ✅ Apply color dynamically
                val colorResId = resources.getIdentifier(clipColorName, "color", packageName)
                if (colorResId != 0) {
                    kgWeights[i].setBackgroundResource(colorResId)
                    gramWeights[i].setBackgroundResource(colorResId)

                    // ✅ Ensure text color is white for blue backgrounds
                    if (clipColorName == "clip_blue") {
                        kgWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                        gramWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                    } else {
                        kgWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                        gramWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                    }
                }

                kgWeights[i].invalidate()
                gramWeights[i].invalidate()
            }
            }
        }




        private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        val totalWeightHundredthKg = tournamentCatches.sumOf { it.totalWeightHundredthKg ?: 0 }
        val totalKgs = totalWeightHundredthKg / 100
        val totalHundredths = totalWeightHundredthKg % 100

        totalKgWeight.text = totalKgs.toString()
        totalGramWeight.text = totalHundredths.toString()
        }

        private fun clearTournamentTextViews() {
            val textViews = listOf(
                firstKgWeight, secondKgWeight, thirdKgWeight, fourthKgWeight,
                fifthKgWeight, sixthKgWeight, firstGramWeight, secondGramWeight,
                thirdGramWeight, fourthGramWeight, fifthGramWeight, sixthGramWeight
            )

            textViews.forEach { it.text = "" }

            totalKgWeight.text = "0"
            totalGramWeight.text = "0"
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
                    btnKgsAlarm.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
                    isRed = !isRed
                    flashHandler.postDelayed(this, 500)
                }
            }

            flashHandler.post(flashRunnable)

            handler.postDelayed({
                mediaPlayer?.stop()
                mediaPlayer?.release()
                btnKgsAlarm.setBackgroundColor(Color.TRANSPARENT)
                flashHandler.removeCallbacks(flashRunnable)
            }, 4000)
        }

        private fun getCurrentDateTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
}
