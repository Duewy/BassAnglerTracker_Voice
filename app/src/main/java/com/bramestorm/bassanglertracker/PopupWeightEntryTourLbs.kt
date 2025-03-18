package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast

class PopupWeightEntryTourLbs : Activity() {
    private var isTournament: Boolean = false
    private lateinit var spinnerSpecies: Spinner
    private lateinit var clipColorSpinner: Spinner
    private lateinit var edtWeightLbs: EditText
    private lateinit var edtWeightOz: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_tour_lbs)

        isTournament = intent.getBooleanExtra("isTournament", false)
        val tournamentSpecies = intent.getStringExtra("tournamentSpecies")?.trim() ?: "Unknown"

        edtWeightLbs = findViewById(R.id.edtWeightTourLbs)
        edtWeightOz = findViewById(R.id.edtWeightTourOz)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)
        btnCancel = findViewById(R.id.btnCancel)
        spinnerSpecies = findViewById(R.id.spinnerSpeciesPopUp)
        clipColorSpinner = findViewById(R.id.clipColorSpinner)

        setupSpeciesSpinner(tournamentSpecies)
        setupClipColorSpinner()

        btnSaveWeight.setOnClickListener { saveWeightEntry() }
        btnCancel.setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }
    }

    private fun setupSpeciesSpinner(tournamentSpecies: String) {
        val speciesList = intent.getStringArrayListExtra("speciesList") ?: arrayListOf()

        if (speciesList.isEmpty()) {
            Log.e("SPECIES_DEBUG", "🚨 No species list received! Using default.")
            speciesList.add("Unknown") // Fallback if no species were sent
        }

        val speciesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = speciesAdapter
    }


    private fun setupClipColorSpinner() {
        val availableClipColors = intent.getStringArrayListExtra("availableClipColors") ?: arrayListOf()

        if (availableClipColors.isEmpty()) {
            Log.e("DB_DEBUG", "🚨 ERROR: No available colors! Using default list.")
            return
        }

        val adapter = ClipColorSpinnerAdapter(this, availableClipColors)
        clipColorSpinner.adapter = adapter
    }


    private fun saveWeightEntry() {
        val resultIntent = Intent()
        val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
        val weightLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
        val weightOz = edtWeightOz.text.toString().toIntOrNull() ?: 0
        val totalWeightOz = (weightLbs * 16) + weightOz
        val selectedClipColor = clipColorSpinner.selectedItem.toString()

        if (totalWeightOz <= 0) {
            Toast.makeText(this, "Invalid weight! Must be greater than 0.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DB_DEBUG", "✅ Sending Result - weightTotalOz: $totalWeightOz, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")
        Toast.makeText(this, "Saved: $totalWeightOz oz, $selectedSpeciesValue, $selectedClipColor", Toast.LENGTH_SHORT).show()

        resultIntent.putExtra("weightTotalOz", totalWeightOz)
        resultIntent.putExtra("selectedSpecies", selectedSpeciesValue)
        resultIntent.putExtra("clipColor", selectedClipColor)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun getColorFromName(colorName: String): Int {
        return when (colorName.uppercase()) {
            "RED" -> android.graphics.Color.RED
            "BLUE" -> android.graphics.Color.BLUE
            "GREEN" -> android.graphics.Color.GREEN
            "YELLOW" -> android.graphics.Color.YELLOW
            "ORANGE" -> android.graphics.Color.rgb(255, 165, 0) // Orange color
            "WHITE" -> android.graphics.Color.WHITE
            else -> android.graphics.Color.GRAY
        }
    }
}
