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

class PopupWeightEntryTourKgs : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightTourKgs: EditText
    private lateinit var edtWeightTourGrams: EditText
    private lateinit var btnSaveWeightKgs: Button
    private lateinit var btnCancelKgs: Button

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_tour_kgs)

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""
        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "WHITE")

        Log.d("PopupWeightEntry", "isTournament: $isTournament | catchType: $catchType | selectedSpecies: $selectedSpecies")
        Log.d("PopupWeightEntry", "Available clip colors: ${colorNames.joinToString()}")

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerSpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtWeightTourKgs = findViewById(R.id.edtWeightTourKgs)
        edtWeightTourGrams = findViewById(R.id.edtWeightTourGrams)
        btnSaveWeightKgs = findViewById(R.id.btnSaveWeightKgs)
        btnCancelKgs = findViewById(R.id.btnCancelKgs)

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

        // `````````````` Apply InputFilters to limit values  ````````````````````
        edtWeightTourKgs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Kgs: 0-99
        edtWeightTourGrams.filters = arrayOf(MinMaxInputFilter(0, 99)) // Grams 0 - 99

        // `````````` SAVE btn ````````````````
        btnSaveWeightKgs.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val weightKgs = edtWeightTourKgs.text.toString().toIntOrNull() ?: 0
            val weightGrams = edtWeightTourGrams.text.toString().toIntOrNull() ?: 0
            val totalweightKgs = (weightKgs * 100) + weightGrams

            if (totalweightKgs == 0) {
                Toast.makeText(this, "Weight cannot be 0.00 Kgs!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CLIPS", "✅ Sending Result - weightTotalOz: $totalweightKgs, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")

            val resultIntent = Intent().apply {
                putExtra("weightTotalKgs", totalweightKgs)
                putExtra("selectedSpecies", selectedSpeciesValue)
                putExtra("clip_color", selectedClipColor)
                putExtra("catchType", catchType)
                putExtra("isTournament", isTournament)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // ````````` CANCEL btn ```````````````````
        btnCancelKgs.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//```````````` END ON CREATE ```````````````````````````

}//================== END  ==========================
