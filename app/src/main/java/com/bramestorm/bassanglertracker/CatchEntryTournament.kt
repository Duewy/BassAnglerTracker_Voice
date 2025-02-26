package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryTournament : AppCompatActivity() {

    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu:Button
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

        // Initialize UI components
        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        listViewTournamentCatches = findViewById(R.id.listViewTournamentCatches)
        btnMenu = findViewById((R.id.btnMenu))

        // Retrieve intent data safely
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        // Debugging: Show received values in Toast
        Toast.makeText(
            this,
            "Catches: $tournamentCatchLimit, Species: $tournamentSpecies, Markers: $typeOfMarkers, Culling: $isCullingEnabled, Unit: $measurementSystem",
            Toast.LENGTH_LONG
        ).show()

        // Load existing tournament catches
        updateTournamentList()

        // Show popup for weight entry when button is clicked
        btnTournamentCatch.setOnClickListener {
            showWeightPopup()
        }

        // Button to navigate back to the SetUp page
        btnMenu.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showWeightPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_weight_entry, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find popup UI elements
        val editWeightLbs = popupView.findViewById<EditText>(R.id.editWeightLbs)
        val editWeightOz = popupView.findViewById<EditText>(R.id.editWeightOz)
        val radioGroupBassType = popupView.findViewById<RadioGroup>(R.id.radioGroupBassType)
        val btnSaveWeight = popupView.findViewById<Button>(R.id.btnSaveWeight)

        btnSaveWeight.setOnClickListener {
            val weightLbs = editWeightLbs.text.toString().toIntOrNull() ?: 0
            val weightOz = editWeightOz.text.toString().toIntOrNull() ?: 0

            if (weightLbs == 0 && weightOz == 0) {
                Toast.makeText(this, "Enter a valid weight!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get selected Bass type
            val selectedBassType = when (radioGroupBassType.checkedRadioButtonId) {
                R.id.radioLargeMouth -> "Large Mouth Bass"
                R.id.radioSmallMouth -> "Small Mouth Bass"
                else -> "Bass"
            }

            popupWindow.dismiss()
            saveTournamentCatch(weightLbs, weightOz, selectedBassType)
        }

        popupWindow.showAtLocation(btnTournamentCatch, Gravity.CENTER, 0, 0)
    }

    private fun saveTournamentCatch(weightLbs: Int, weightOz: Int, bassType: String) {
        val catch = CatchItem(
            id = 0, // Assuming 0 for auto-increment
            dateTime = getCurrentDateTime(),
            species = tournamentSpecies,
            weightLbs = weightLbs,
            weightOz = weightOz,
            weightDecimal = null,
            lengthA8th = null,
            lengthInches = null,
            lengthDecimal = null,
            catchType = measurementSystem,
            markerType = bassType // Stores "Large Mouth Bass" or "Small Mouth Bass"
        )

        dbHelper.insertCatch(catch)

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
        updateTournamentList()
    }

    private fun updateTournamentList() {
        val allCatches = dbHelper.getAllCatches()
        val sortedCatches = allCatches.sortedByDescending {
            (it.weightLbs ?: 0) * 16 + (it.weightOz ?: 0)
        }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            tournamentCatches.map {
                "${it.markerType}: ${it.weightLbs ?: 0} lbs ${it.weightOz ?: 0} oz"
            }
        )
        listViewTournamentCatches.adapter = adapter
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
