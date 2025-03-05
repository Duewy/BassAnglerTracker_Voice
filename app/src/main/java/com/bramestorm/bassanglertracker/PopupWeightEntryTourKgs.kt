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

class PopupWeightEntryTourKgs : Activity() {

         private var isTournament: Boolean = false
        private var catchType: String = ""
        private var selectedSpecies: String = ""

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.popup_weight_entry_tour_kgs)


            // ````````````````  Retrieve intent extras  ````````````````````````````````

            isTournament = intent.getBooleanExtra("isTournament", false)
            catchType = intent.getStringExtra("catchType") ?: ""
            selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""


            // UI elements

            val edtWeightKgs: EditText = findViewById(R.id.edtWeightTourKgs)
            val edtWeightGrams: EditText = findViewById(R.id.edtWeightTourGrams) // New field for decimal kg

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

                val weightKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
                val weightGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
                val totalWeightHundredthKg = ((weightKgs * 100).toInt() + weightGrams/10)

                resultIntent.putExtra("weightTotalKgs", totalWeightHundredthKg)
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

