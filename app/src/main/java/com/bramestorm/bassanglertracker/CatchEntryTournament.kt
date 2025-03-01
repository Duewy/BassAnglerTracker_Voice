package com.bramestorm.bassanglertracker


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.PopupWeightEntry
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

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetter6)

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
        val intent = Intent(this, PopupWeightEntry::class.java)
        intent.putExtra("isTournament", true)
        intent.putExtra("catchType", if (measurementSystem == "weight") "lbsOzs" else "kgs")
        intent.putExtra("selectedSpecies", tournamentSpecies)
        startActivityForResult(intent,REQUEST_WEIGHT_ENTRY)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_WEIGHT_ENTRY && resultCode == Activity.RESULT_OK) {
            val weightTotalOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
            val weightTotalKgs = data?.getDoubleExtra("weightTotalKgs", 0.0) ?: 0.0
            val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""

            if (measurementSystem == "weight") {
                if (weightTotalOz > 0) {
                    saveTournamentCatch(weightTotalOz, selectedSpecies)
                } else {
                    saveTournamentCatchKgs(weightTotalKgs, selectedSpecies)
                }
            }
        }
    }

    private fun saveTournamentCatch(totalWeightOz: Int, species: String) {
        val weightLbs = totalWeightOz / 16
        val weightOz = totalWeightOz % 16

        val colorList = listOf("clip_red", "clip_yellow", "clip_green", "clip_blue", "clip_white", "clip_orange")
        val existingCatches = dbHelper.getAllCatches().size
        val assignedColor = colorList[existingCatches % colorList.size] // Cycle through colors

        Log.d("DEBUG", "Saving Catch -> Lbs: $weightLbs, Oz: $weightOz | Species: $species")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = totalWeightOz,  // ✅ Now correctly storing total weight in ounces
            totalLengthA8th = null,
            weightDecimalTenthKg = null,
            lengthDecimalTenthCm = null,
            catchType = "lbsOzs",
            markerType = species,
            clipColor = assignedColor
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()
        updateTournamentList()
    }



    private fun saveTournamentCatchKgs(weightKgs: Double, species: String) {
        val weightStoredKgs = (weightKgs * 100).toInt() // ✅ Store as an integer (hundredths of kgs)

        val colorList = listOf("clip_red", "clip_yellow", "clip_green", "clip_blue", "clip_white", "clip_orange")
        val existingCatches = dbHelper.getAllCatches().size
        val assignedColor = colorList[existingCatches % colorList.size] // Cycle through colors

        Log.d("DEBUG", "Saving Catch -> Weight in Kgs: $weightKgs | Stored as: $weightStoredKgs | Species: $species")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = null,
            weightDecimalTenthKg = weightStoredKgs.toInt(), // ✅ Store Kg weight
            totalLengthA8th = null,
            lengthDecimalTenthCm = null,
            catchType = "kgs",
            markerType = species,
            clipColor = assignedColor
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()
        updateTournamentList()

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()
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

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        clearTournamentTextViews()

        runOnUiThread {
            for (i in tournamentCatches.indices) {
                if (i >= 6) break // ✅ Prevent out-of-bounds errors

                val totalWeightOz = tournamentCatches[i].totalWeightOz ?: 0
                val weightLbs = (totalWeightOz / 16)  // ✅ Ensure integer division
                val weightOz = (totalWeightOz % 16)  // ✅ Extract remaining ounces
                val clipColorName = tournamentCatches[i].clipColor
                val speciesInitial = tournamentCatches[i].markerType ?: "?" // ✅ Get species letter

                // ✅ Debugging Log: Confirm correct weight separation
                Log.d("DEBUG", "Updating UI -> Stored Oz: $totalWeightOz | Calculated Lbs: $weightLbs | Oz: $weightOz | Letter: $speciesInitial")

                // ✅ Update UI
                realWeights[i].text = "$weightLbs Lbs" // ✅ Ensure correct lbs value
                decWeights[i].text = "$weightOz oz"    // ✅ Ensure correct oz value
                typeLetters[i].text = speciesInitial   // ✅ Assign species letter

                // ✅ Apply color dynamically
                val colorResId = resources.getIdentifier(clipColorName, "color", packageName)
                if (colorResId != 0) {
                    realWeights[i].setBackgroundResource(colorResId)
                    decWeights[i].setBackgroundResource(colorResId)
                    typeLetters[i].setBackgroundResource(colorResId)

                    // ✅ Ensure text is visible on different backgrounds
                    val textColor = if (clipColorName == "clip_blue") R.color.clip_white else R.color.black
                    realWeights[i].setTextColor(resources.getColor(textColor, theme))
                    decWeights[i].setTextColor(resources.getColor(textColor, theme))
                    typeLetters[i].setTextColor(resources.getColor(textColor, theme))
                }

                realWeights[i].invalidate()
                decWeights[i].invalidate()
                typeLetters[i].invalidate()
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
        // ✅ Debugging Log: Check stored weight before inserting
        Log.d("DEBUG", "Saving Catch -> Total Weight in Oz: $totalWeightOz ")

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
