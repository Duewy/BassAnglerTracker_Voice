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

class PopupWeightEntryTourLbs : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightLbs: EditText
    private lateinit var edtWeightOz: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var btnCancel: Button

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_tour_lbs)

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
        edtWeightLbs = findViewById(R.id.edtWeightTourLbs)
        edtWeightOz = findViewById(R.id.edtWeightTourOz)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)
        btnCancel = findViewById(R.id.btnCancel)

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

        edtWeightLbs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Lbs: 0-99
        edtWeightOz.filters = arrayOf(MinMaxInputFilter(0, 15)) // Ozs 0 - 15

    // `````````` SAVE btn ````````````````
        btnSaveWeight.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val weightLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
            val weightOz = edtWeightOz.text.toString().toIntOrNull() ?: 0
            val totalWeightOz = ((weightLbs * 16) + weightOz)

            if (totalWeightOz == 0) {
                Toast.makeText(this, "🚫 Weight cannot be 0 lbs 0 oz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CLIPS", "✅ Sending Result - weightTotalOz: $totalWeightOz, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")

            val resultIntent = Intent().apply {
                putExtra("weightTotalOz", totalWeightOz)
                putExtra("selectedSpecies", selectedSpeciesValue)
                putExtra("clip_color", selectedClipColor)
                putExtra("catchType", catchType)
                putExtra("isTournament", isTournament)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

      // ````````` CANCEL btn ```````````````````
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//```````````` END ON CREATE ```````````````````````````

}//================== END  ==========================
