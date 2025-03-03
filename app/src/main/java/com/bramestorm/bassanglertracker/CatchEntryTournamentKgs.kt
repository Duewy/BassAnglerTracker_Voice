package com.bramestorm.bassanglertracker


import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

    private lateinit var firstKgWeight: TextView
    private lateinit var secondKgWeight: TextView
    private lateinit var thirdKgWeight: TextView
    private lateinit var fourthKgWeight: TextView
    private lateinit var fifthKgWeight: TextView
    private lateinit var sixthKgWeight: TextView

    private lateinit var firstGramWeight: TextView
    private lateinit var secondGramWeight: TextView
    private lateinit var thirdGramWeight: TextView
    private lateinit var fourthGramWeight: TextView
    private lateinit var fifthGramWeight: TextView
    private lateinit var sixthGramWeight: TextView

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
    private val REQUEST_WEIGHT_ENTRY = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_kgs)

        // ✅ Initialize TextViews
        firstKgWeight = findViewById(R.id.firstKgWeight)
        secondKgWeight = findViewById(R.id.secondKgWeight)
        thirdKgWeight = findViewById(R.id.thirdKgWeight)
        fourthKgWeight = findViewById(R.id.fourthKgWeight)
        fifthKgWeight = findViewById(R.id.fifthKgWeight)
        sixthKgWeight = findViewById(R.id.sixthKgWeight)

        firstGramWeight = findViewById(R.id.firstGramWeight)
        secondGramWeight = findViewById(R.id.secondGramWeight)
        thirdGramWeight = findViewById(R.id.thirdGramWeight)
        fourthGramWeight = findViewById(R.id.fourthGramWeight)
        fifthGramWeight = findViewById(R.id.fifthGramWeight)
        sixthGramWeight = findViewById(R.id.sixthGramWeight)

        totalKgWeight = findViewById(R.id.totalKgWeight)
        totalGramWeight = findViewById(R.id.totalGramWeight)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetter6)

        clearTournamentTextViews()

        dbHelper = CatchDatabaseHelper(this)

        btnKgsStartFishing = findViewById(R.id.btnKgsStartFishing)
        btnMenu = findViewById(R.id.btnMenu)

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
    }

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntry::class.java)
        intent.putExtra("isTournament", true)
        intent.putExtra("catchType", if (measurementSystem == "weight") "lbsOzs" else "kgs")
        intent.putExtra("selectedSpecies", tournamentSpecies)
        startActivityForResult(intent,REQUEST_WEIGHT_ENTRY)
    }

    private fun updateTournamentList() {
        val allCatches = dbHelper.getAllCatches().filter { it.catchType == "kgs" }
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }
        val tournamentCatches = sortedCatches.take(tournamentCatchLimit)

        val kgWeights = listOf(firstKgWeight, secondKgWeight, thirdKgWeight, fourthKgWeight, fifthKgWeight, sixthKgWeight)
        val gramWeights = listOf(firstGramWeight, secondGramWeight, thirdGramWeight, fourthGramWeight, fifthGramWeight, sixthGramWeight)
        val typeLetters = listOf(txtTypeLetter1, txtTypeLetter2, txtTypeLetter3, txtTypeLetter4, txtTypeLetter5, txtTypeLetter6)

        clearTournamentTextViews()

        runOnUiThread {
            for (i in tournamentCatches.indices) {
                if (i >= 6) break

                val totalWeightHundredthKg = tournamentCatches[i].totalWeightHundredthKg ?: 0
                val weightKgs = totalWeightHundredthKg / 100
                val weightHundredths = totalWeightHundredthKg % 100
                val speciesInitial = tournamentCatches[i].markerType ?: "?"

                kgWeights[i].text = "$weightKgs"
                gramWeights[i].text = "$weightHundredths"
                typeLetters[i].text = speciesInitial
            }
        }

        updateTotalWeight(tournamentCatches)
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
            firstKgWeight, secondKgWeight, thirdKgWeight, fourthKgWeight, fifthKgWeight, sixthKgWeight,
            firstGramWeight, secondGramWeight, thirdGramWeight, fourthGramWeight, fifthGramWeight, sixthGramWeight
        )
        textViews.forEach { it.text = "" }
        totalKgWeight.text = "0"
        totalGramWeight.text = "0"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
