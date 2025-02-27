package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*
import android.view.View
import android.util.Log


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

    private lateinit var listViewTournamentCatches: ListView
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

        totalRealWeight= findViewById(R.id.totalRealWeight)
        totalDecWeight= findViewById(R.id.totalDecWeight)

        // ✅ Now clear all TextViews
        clearTournamentTextViews()

        // ✅ Initialize buttons and database
        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        listViewTournamentCatches = findViewById(R.id.listViewTournamentCatches)
        btnMenu = findViewById(R.id.btnMenu)

        // ✅ Retrieve intent data safely
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        // ✅ Debugging: Show received values in Toast
        Toast.makeText(
            this,
            "Catches: $tournamentCatchLimit, Species: $tournamentSpecies, Markers: $typeOfMarkers, Culling: $isCullingEnabled, Unit: $measurementSystem",
            Toast.LENGTH_LONG
        ).show()

        // ✅ Load existing tournament catches
        updateTournamentList()

        // ✅ Show popup for weight entry when button is clicked
        btnTournamentCatch.setOnClickListener {
            showWeightPopup()
        }

        // ✅ Button to navigate back to the SetUp page
        btnMenu.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }
    }

    // ✅ Clears all 12 weight TextViews and the TOTAL Weight
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

        totalRealWeight.text= "0"
        totalDecWeight.text= "0"
    }

    // ✅@@@@@@@@@@  Show the weight entry POPUP  @@@@@@@@@@@@@@@@@@@@@@@

    private fun showWeightPopup() {
        val popup = PopupWeightEntry(this) { weightLbs, weightOz, bassType ->
            saveTournamentCatch(weightLbs, weightOz, bassType)
        }
        popup.show()
    }


    // ✅ Save tournament catch and update the list
    private fun saveTournamentCatch(weightLbs: Int, weightOz: Int, bassType: String) {
        val catch = CatchItem(
            id = 0, // Auto-increment ID
            dateTime = getCurrentDateTime(),
            species = tournamentSpecies,
            weightLbs = weightLbs,
            weightOz = weightOz,
            weightDecimal = null,
            lengthA8th = null,
            lengthInches = null,
            lengthDecimal = null,
            catchType = measurementSystem,
            markerType = bassType
        )

        dbHelper.insertCatch(catch)

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()

        // ✅ Update the UI immediately
        updateTournamentList()
    }


    // ✅ Update the tournament list and fill the text fields


    private fun updateTournamentList() {
        val allCatches = dbHelper.getAllCatches()

        // ✅ Log if the database is retrieving anything
        Log.d("TournamentDebug", "Total catches retrieved: ${allCatches.size}")

        if (allCatches.isEmpty()) {
            Log.d("TournamentDebug", "No catches found in the database!")
            return  // Exit if no catches are found
        }

        val sortedCatches = allCatches.sortedByDescending {
            (it.weightLbs ?: 0) * 16 + (it.weightOz ?: 0)
        }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        // ✅ Log the catches retrieved from the database
        for (catch in tournamentCatches) {
            Log.d(
                "TournamentDebug",
                "Catch -> ${catch.weightLbs} lbs ${catch.weightOz} oz | Species: ${catch.species}"
            )
        }

        // ✅ Populate the TextViews with the retrieved catches
        val realWeights = listOf(firstRealWeight, secondRealWeight, thirdRealWeight,
            fourthRealWeight, fifthRealWeight, sixthRealWeight)

        val decWeights = listOf(firstDecWeight, secondDecWeight, thirdDecWeight,
            fourthDecWeight, fifthDecWeight, sixthDecWeight)

        clearTournamentTextViews()

        for (i in tournamentCatches.indices) {
            realWeights[i].text = tournamentCatches[i].weightLbs.toString()
            decWeights[i].text = tournamentCatches[i].weightOz.toString()

            // ✅ Log what is being updated in the UI
            Log.d("TournamentDebug", "Updating TextView [$i]: ${tournamentCatches[i].weightLbs} lbs ${tournamentCatches[i].weightOz} oz")
        }

        // ✅ Update the total weight after populating individual weights
        updateTotalWeight(tournamentCatches)

        // ✅ Adjust visibility for extra TextViews
        adjustTextViewVisibility()
    }



    // **************** Update TOTAL WEIGHT  ************************
private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
    var totalLbs = 0
    var totalOzs = 0

    for (catch in tournamentCatches) {
        totalLbs += catch.weightLbs ?: 0
        totalOzs += catch.weightOz ?: 0
    }

    // Convert excess ounces to pounds
    totalLbs += totalOzs / 16
    totalOzs %= 16

    // Update total weight TextViews
    totalRealWeight.text = totalLbs.toString()
    totalDecWeight.text = totalOzs.toString()
}

    // `````````````````` ADJUST TEXTVIEW VISIBILITY `````````````````
    private fun adjustTextViewVisibility() {
        if (tournamentCatchLimit == 4) {
            // 5th weight fields → Greyed out
            fifthRealWeight.alpha = 0.5f
            fifthDecWeight.alpha = 0.5f
            fifthRealWeight.isEnabled = false
            fifthDecWeight.isEnabled = false

            // 6th weight fields → Invisible
            sixthRealWeight.visibility = View.INVISIBLE
            sixthDecWeight.visibility = View.INVISIBLE
        } else if (tournamentCatchLimit == 5) {
            // 6th weight fields → Greyed out
            sixthRealWeight.alpha = 0.5f
            sixthDecWeight.alpha = 0.5f
            sixthRealWeight.isEnabled = false
            sixthDecWeight.isEnabled = false
        } else {
            // All TextViews fully visible & enabled if limit is 6
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

//*************** Get Current TIME DATE  ***********************
private fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}


// ------------------------- END  ----------------------
}
