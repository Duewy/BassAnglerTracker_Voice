package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryTournament : AppCompatActivity() {

    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu: Button

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
    private lateinit var totalRealWeight: TextView
    private lateinit var totalDecWeight: TextView

    private lateinit var dbHelper: CatchDatabaseHelper

    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)

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

        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)

        clearTournamentTextViews()

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)

        // ✅ Retrieve intent data safely
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        updateTournamentList()

        btnTournamentCatch.setOnClickListener {
            showWeightPopup()
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }
    }

    private fun showWeightPopup() {
        val popup = PopupWeightEntry(this) { weightLbs, weightOz, bassType ->
            val totalWeightOz = (weightLbs * 16) + weightOz // ✅ Convert to whole ounces
            saveTournamentCatch(totalWeightOz, bassType)
        }
        popup.show()
    }


    private fun saveTournamentCatch(totalWeightOz: Int, bassType: String) {
        val colorList = listOf("clip_red", "clip_yellow", "clip_green", "clip_blue", "clip_white", "clip_orange")

        // Get the next color in sequence based on total stored catches
        val existingCatches = dbHelper.getAllCatches().size
        val assignedColor = colorList[existingCatches % colorList.size] // Cycle through colors

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = tournamentSpecies,
            totalWeightOz = totalWeightOz,
            totalLengthA8th = null,
            weightDecimalTenthKg = null,
            lengthDecimalTenthCm = null,
            catchType = measurementSystem,
            markerType = bassType,
            clipColor = assignedColor
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
        updateTournamentList()
    }


    private fun updateTournamentList() {
        val allCatches = dbHelper.getAllCatches()
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

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
