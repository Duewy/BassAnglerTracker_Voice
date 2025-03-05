package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

class PopupWeightEntryTourLbs : Activity() {
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_tour_lbs)


  // ````````````````  Retrieve intent extras  ````````````````````````````````

        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""

        // Debugging Log
        Log.d("PopupWeightEntry", "Received isTournament: $isTournament")
        Log.d("PopupWeightEntry", "Received catchType: $catchType")
        Log.d("PopupWeightEntry", "Received selectedSpecies: $selectedSpecies")

        // UI elements
        val edtWeightLbs: EditText = findViewById(R.id.edtWeightTourLbs)
        val edtWeightOz: EditText = findViewById(R.id.edtWeightTourOz)

        val btnSaveWeight: Button = findViewById(R.id.btnSaveWeight)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerSpeciesPopUp) // Matches XML ID


        // Get pre-selected tournament species

        val tournamentSpecies = intent.getStringExtra("tournamentSpecies")?.trim() ?: "Unknown"


// Determine species list based on tournament type
        val speciesList: Array<String> = when {
            isTournament && tournamentSpecies.equals("Bass", ignoreCase = true) -> {
                arrayOf("Large Mouth", "Small Mouth") // ✅ Both Bass species available
            }
            isTournament -> {
                arrayOf(tournamentSpecies) // ✅ Only one species allowed
            }
            else -> {
                arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
            }
        }

// Apply to Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter



        // Save button functionality
        btnSaveWeight.setOnClickListener {
            val resultIntent = Intent()
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            resultIntent.putExtra("selectedSpecies", selectedSpeciesValue)

                    val weightLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
                    val weightOz = edtWeightOz.text.toString().toIntOrNull() ?: 0
                    val totalWeightOz = (weightLbs * 16) + weightOz

            resultIntent.putExtra("weightTotalOz", totalWeightOz)
            resultIntent.putExtra("catchType", catchType)
            resultIntent.putExtra("isTournament", isTournament)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }


        // Cancel button functionality
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
