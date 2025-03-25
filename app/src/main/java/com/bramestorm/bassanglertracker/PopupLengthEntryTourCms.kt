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

class PopupLengthEntryTourCms : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtLengthCms: EditText
    private lateinit var edtLengthDec: EditText
    private lateinit var btnSaveLengthCms: Button
    private lateinit var btnCancelCms: Button

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_length_entry_tour_cms)

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""
        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtLengthCms = findViewById(R.id.edtLengthTourCms)
        edtLengthDec = findViewById(R.id.edtLengthTourDec)
        btnSaveLengthCms = findViewById(R.id.btnSaveLengthCms)
        btnCancelCms = findViewById(R.id.btnCancelCms)

        // ************  Setup Species Spinner *********************        // if Small Mouth is selected then Small Mouth is at top of Spinner
        val tournamentSpecies = intent.getStringExtra("tournamentSpecies")?.trim() ?: "Unknown"
        val speciesList: Array<String> = when {
            isTournament && tournamentSpecies.equals("Large Mouth Bass", ignoreCase = true) -> {
                arrayOf("Large Mouth", "Small Mouth")
            }
            isTournament && tournamentSpecies.equals("Small Mouth Bass", ignoreCase = true) -> {
                arrayOf("Small Mouth", "Large Mouth")
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


        // `````````` btn SAVE  ````````````````
        btnSaveLengthCms.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val lengthCms = edtLengthCms.text.toString().toIntOrNull() ?: 0
            val lengthDec = edtLengthDec.text.toString().toIntOrNull() ?: 0
            val totalLengthTenths= ((lengthCms * 10) + lengthDec)

            if ( totalLengthTenths == 0) {
                Toast.makeText(this, "Length cannot be 0.o cms!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CLIPS", "✅ Sending Result - weightLengthCms: $totalLengthTenths, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")

            val resultIntent = Intent().apply {
                putExtra("lengthTotalCms", totalLengthTenths)
                putExtra("selectedSpecies", selectedSpeciesValue)
                putExtra("clip_color", selectedClipColor)
                putExtra("catchType", catchType)
                putExtra("isTournament", isTournament)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // ````````` btn CANCEL  ```````````````````
        btnCancelCms.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//```````````` END ON CREATE ```````````````````````````

}//================== END  ==========================
