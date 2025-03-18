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

    //------------------- ON CREATE --------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)

        dbHelper = CatchDatabaseHelper(this)

        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnAlarm = findViewById(R.id.btnAlarm)
        btnResetCatch = findViewById(R.id.btnResetCatch)

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

        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 =findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4= findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5= findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6= findViewById(R.id.txtTypeLetter6)

        txtColorLetter1 = findViewById(R.id.txtColorLetter1)
        txtColorLetter2 = findViewById(R.id.txtColorLetter2)
        txtColorLetter3 = findViewById(R.id.txtColorLetter3)
        txtColorLetter4 = findViewById(R.id.txtColorLetter4)
        txtColorLetter5 = findViewById(R.id.txtColorLetter5)
        txtColorLetter6 = findViewById(R.id.txtColorLetter6)

        speciesLetters = listOf(txtTypeLetter1, txtTypeLetter2, txtTypeLetter3, txtTypeLetter4, txtTypeLetter5, txtTypeLetter6)


        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        btnMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnTournamentCatch.setOnClickListener { showWeightPopup() }

        btnResetCatch.setOnClickListener {
            dbHelper.setTournamentResetPoint()
            Toast.makeText(this, "Tournament Reset! Only new catches will count.", Toast.LENGTH_SHORT).show()
            updateTournamentList()
        }

       // handler.postDelayed(checkAlarmRunnable, 60000)
        updateTournamentList()
    }
    // ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    //'''''''''''''' Pop Up Entry ''''''''''''''''''''''''''''

    private fun showWeightPopup() {
        Log.d("TOURNAMENT_DEBUG", "🔄 showWeightPopup() called. Refreshing the list.")

        val intent = Intent(this, PopupWeightEntryTourLbs::class.java)
        intent.putExtra("isTournament", true)

        // ✅ Send species correctly
        intent.putExtra("tournamentSpecies", tournamentSpecies)

        // ✅ Pass species list correctly (Bass includes both)
        val speciesList = if (tournamentSpecies == "Bass") {
            arrayListOf("Large Mouth", "Small Mouth")
        } else {
            arrayListOf(tournamentSpecies)
        }
        intent.putStringArrayListExtra("speciesList", speciesList)

        intent.putStringArrayListExtra("availableClipColors", ArrayList(getAvailableClipColors()))
        startActivityForResult(intent, 1005)
    }


    //'''''''''''''''''' Triggered when data returns from PopUp '''''''''''''''''''''''''

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("TOURNAMENT_DEBUG", "🔥 onActivityResult triggered: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == 1005 && resultCode == Activity.RESULT_OK) {

            val newCatch = data?.getSerializableExtra("NEW_CATCH") as? CatchItem
            if (newCatch != null) {
                Log.d("TOURNAMENT_DEBUG", "✅ Received new tournament catch: $newCatch")
                dbHelper.insertCatch(newCatch)
                updateTournamentList()  // Refresh UI
            } else {
                Log.e("TOURNAMENT_DEBUG", "❌ No tournament catch received!")
            }
        }
    }


    // `````````````` Available Colors  ````````````````````````````````

    private fun getAvailableClipColors(): List<String> {
        Log.d("TOURNAMENT_DEBUG", "🔄 getAvailableClipColors() called. Refreshing the list.")

        val allCatches = dbHelper.getCatchesForToday("LbsOzs", getCurrentDate())
        val usedClipColors = allCatches.mapNotNull { it.clipColor?.uppercase() }.toSet()
        return listOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE").filterNot { it in usedClipColors }
    }

    private fun getColorFromName(colorName: String): Int {
        Log.d("TOURNAMENT_DEBUG", "🔄 getColorFromName() called. Refreshing the list.")

        return when (colorName.uppercase()) {
            "RED" -> resources.getColor(R.color.clip_red, theme)
            "YELLOW" -> resources.getColor(R.color.clip_yellow, theme)
            "GREEN" -> resources.getColor(R.color.clip_green, theme)
            "BLUE" -> resources.getColor(R.color.clip_blue, theme)
            "WHITE" -> resources.getColor(R.color.clip_white, theme)
            "ORANGE" -> resources.getColor(R.color.clip_orange, theme)
            else -> resources.getColor(R.color.black, theme) // Default to black if undefined
        }
    }



    // ```````````````` Tag Clip Color to LETTER for color blind '''''''''''''''''

    private fun getClipColorLetter(color: String): String {
        Log.d("TOURNAMENT_DEBUG", "🔄 getClipColorLetter) called. Refreshing the list.")

        return when (color) {
            "BLUE" -> "B"
            "GREEN" -> "G"
            "YELLOW" -> "Y"
            "RED" -> "R"
            "ORANGE" -> "O"
            "WHITE" -> "W"
            else -> ""
        }
    }

    // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for culling values ~~~~~~~~~~~~~
    private fun adjustTextViewVisibility() {
        Log.d("TOURNAMENT_DEBUG", "🔄 adjustTextViewVisibility() called. Refreshing the list.")

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

    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {
        Log.d("TOURNAMENT_DEBUG", "🔄 clearTournamentTextViews() called. Refreshing the list.")

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

    //-------------- Up Date List ---------------------------

    private fun updateTournamentList() {

        Log.d("TOURNAMENT_DEBUG", "🔄 updateTournamentList() called. Refreshing the list.")

        val allCatches = dbHelper.getCatchesForToday("LbsOzs", getCurrentDate())
            .sortedByDescending { it.totalWeightOz ?: 0 }
        val tournamentCatches = if (isCullingEnabled) allCatches.take(tournamentCatchLimit) else allCatches

        val realWeights = listOf(
            firstRealWeight, secondRealWeight, thirdRealWeight,
            fourthRealWeight, fifthRealWeight, sixthRealWeight
        )

        val decWeights = listOf(
            firstDecWeight, secondDecWeight, thirdDecWeight,
            fourthDecWeight, fifthDecWeight, sixthDecWeight
        )

        clearTournamentTextViews()

        for (i in tournamentCatches.indices) {
            val catch = tournamentCatches[i]
            val weightLbs = ((catch.totalWeightOz ?: 0) / 16)
            val weightOz = ((catch.totalWeightOz ?: 0) % 16)
            realWeights[i].text = weightLbs.toString()
            decWeights[i].text = weightOz.toString()

            // ✅ Fetch stored clip color properly
            val catchClipColor = catch.clipColor?.uppercase() ?: "WHITE"

            // ✅ Apply background color
            realWeights[i].setBackgroundColor(getColorFromName(catchClipColor))
            decWeights[i].setBackgroundColor(getColorFromName(catchClipColor))
            speciesLetters[i].text = getClipColorLetter(catchClipColor)

            // ✅ Ensure proper text color for readability
            if (catchClipColor == "BLUE" || catchClipColor == "GREEN") {
                realWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                decWeights[i].setTextColor(resources.getColor(R.color.clip_white, theme))
                speciesLetters[i].setTextColor(resources.getColor(R.color.clip_white, theme))
            } else {
                realWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                decWeights[i].setTextColor(resources.getColor(R.color.black, theme))
                speciesLetters[i].setTextColor(resources.getColor(R.color.black, theme))
            }

            Log.d("TOURNAMENT_DEBUG", "🎣 Catch ${i + 1}: $weightLbs lbs, $weightOz oz, Color: $catchClipColor")
            realWeights[i].invalidate()
            decWeights[i].invalidate()
        }

        updateTotalWeight(tournamentCatches)
        adjustTextViewVisibility()
    }




    //''''''''''''' Up Date Total Weight '''''''''''''''''''''''''''''''''''''
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        Log.d("TOURNAMENT_DEBUG", "🔄 updateTotalWeight() called. Refreshing the list.")

        val totalWeightOz = tournamentCatches.sumOf { it.totalWeightOz ?: 0 }
        val totalLbs = (totalWeightOz / 16)
        val totalOz = (totalWeightOz % 16)
        totalRealWeight.text = totalLbs.toString()
        totalDecWeight.text = totalOz.toString()
    }


    //!!!!!!!!!!!!!! Current Date !!!!!!!!!!!!!!!!!!!!!!!!!

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    //++++++++++++++++ Date and Time  +++++++++++++++++++++++++++++
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
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


}//```````````````  END  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
