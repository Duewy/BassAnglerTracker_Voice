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
import com.bramestorm.bassanglertracker.PopupWeightEntryLbs.MinMaxInputFilter

class PopupLengthEntryTourInches : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtLengthInches: EditText
    private lateinit var edtLengthQuarters: EditText
    private lateinit var btnSaveLengthInches: Button
    private lateinit var btnCancelInches: Button

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_length_entry_tour_inches)

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""
        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtLengthInches = findViewById(R.id.edtLengthTourInches)
        edtLengthQuarters = findViewById(R.id.edtLengthQuarters)
        btnSaveLengthInches = findViewById(R.id.btnSaveLengthInches)
        btnCancelInches = findViewById(R.id.btnCancelInches)

        // ************  Setup Species Spinner *********************        // if Small Mouth is selected then Small Mouth is at top of Spinner
        val tournamentSpecies = intent.getStringExtra("tournamentSpecies")?.trim() ?: "Unknown"
        val speciesList: Array<String> = when {
            isTournament && tournamentSpecies.equals("Large Mouth Bass", ignoreCase = true) -> {
                arrayOf("Large Mouth", "Small Mouth")
            }
            isTournament && tournamentSpecies.equals("Small Mouth Bass", ignoreCase = true) -> {
                arrayOf("Small Mouth", "Large Mouth")
            }

            isTournament && tournamentSpecies.equals("Spotted Bass", ignoreCase = true) -> {
                arrayOf("Spotted Bass","Small Mouth", "Large Mouth")    // Southern States Have All Three Bass Species
            }
            isTournament -> {
                arrayOf(tournamentSpecies)
            }
            else -> {
                arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
            }
        }
        val speciesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = speciesAdapter

        // ****************  Setup Clip Color Spinner ****************
        val availableColorNames = intent.getStringArrayExtra("availableClipColors") ?: emptyArray()
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter

        edtLengthInches.filters = arrayOf(MinMaxInputFilter(0, 99)) // Inches: 0-99
        edtLengthQuarters.filters = arrayOf(MinMaxInputFilter(0, 3)) //  0 - 3 / 4ths

        // `````````` btn SAVE  ````````````````
        btnSaveLengthInches.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val lengthInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
            val lengthQuarters = edtLengthQuarters.text.toString().toIntOrNull() ?: 0

            val totalLengthQuarters= ((lengthInches * 4) + lengthQuarters)

            if ( totalLengthQuarters == 0) {
                Toast.makeText(this, "Length cannot be 0 Inches!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CLIPS", "✅ Sending Result - weightLengthInches: $totalLengthQuarters, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")

            val resultIntent = Intent().apply {
                putExtra("lengthTotalInches", totalLengthQuarters)
                putExtra("selectedSpecies", selectedSpeciesValue)
                putExtra("clip_color", selectedClipColor)
                putExtra("catchType", catchType)
                putExtra("isTournament", isTournament)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // ````````` btn CANCEL  ```````````````````
        btnCancelInches.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//```````````` END ON CREATE ```````````````````````````

}//================== END  ==========================
