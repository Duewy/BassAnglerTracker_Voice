package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import android.util.Log

class PopupWeightEntry : Activity() {
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry)


  // ````````````````  Retrieve intent extras  ````````````````````````````````

        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""

        // Debugging Log
        Log.d("PopupWeightEntry", "Received isTournament: $isTournament")
        Log.d("PopupWeightEntry", "Received catchType: $catchType")
        Log.d("PopupWeightEntry", "Received selectedSpecies: $selectedSpecies")

        // UI elements
        val txtPopupTitle: TextView = findViewById(R.id.txtPopupTitle)
        val edtWeightLbs: EditText = findViewById(R.id.edtWeightLbs)
        val edtWeightOz: EditText = findViewById(R.id.edtWeightOz)
        val edtWeightKgs: EditText = findViewById(R.id.edtWeightKgs)
        val edtWeightGrams: EditText = findViewById(R.id.edtWeightGrams) // New field for decimal kg
        val edtLengthInches: EditText = findViewById(R.id.edtLengthInches)
        val edtLengthCM: EditText = findViewById(R.id.edtLengthCM)
        val layoutWeightLbsOzs: LinearLayout = findViewById(R.id.layoutWeightLbsOzs)
        val layoutWeightKgs: LinearLayout = findViewById(R.id.layoutWeightKgs)
       // val layoutLengthInches : LinearLayout = findViewById(R.id.layoutLenghtInches)
       // val layoutLenghtCm : LinearLayout = findViewById(R.id.layoutLenghtCm)
        val btnSaveWeight: Button = findViewById(R.id.btnSaveWeight)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerSpeciesPopUp) // Matches XML ID


        // Get pre-selected tournament species
        val tournamentSpecies = intent.getStringExtra("tournamentSpecies")?.takeIf { it.isNotBlank() } ?: "Large Mouth"

        Log.d("PopupDebug", "Using tournamentSpecies: $tournamentSpecies") // Debugging

// Determine species list based on event type
        val speciesList: Array<String> = if (isTournament) {
            if (tournamentSpecies == "Bass") {
                arrayOf("Large Mouth", "Small Mouth") // ✅ Both Bass species
            } else {
                arrayOf(tournamentSpecies) // ✅ Only the selected species
            }
        } else {
            // FunDay Mode: Full species list
            arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
        }

        Log.d("PopupDebug", "Species List: ${speciesList.joinToString()}") // Debugging

// Populate the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter



        //``````   Hide all weight/length input fields initially  `````````````````````
        edtWeightLbs.visibility = View.GONE
        edtWeightOz.visibility = View.GONE
        edtWeightKgs.visibility = View.GONE
        edtWeightGrams.visibility = View.GONE
        edtLengthInches.visibility = View.GONE
        edtLengthCM.visibility = View.GONE
        layoutWeightLbsOzs.visibility = View.GONE
        layoutWeightKgs.visibility = View.GONE

        // Populate the Spinner
        val radapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        radapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = radapter

        // Adjust UI based on catch type
        edtWeightLbs.visibility = View.GONE
        edtWeightOz.visibility = View.GONE
        edtWeightKgs.visibility = View.GONE
        edtLengthInches.visibility = View.GONE
        edtLengthCM.visibility = View.GONE
        layoutWeightLbsOzs.visibility = View.GONE

        //@@@@@@@@@@@@ SET UP ENTRY TYPE  ##########################

        when (catchType) {

                "lbsOzs" -> {
                txtPopupTitle.text = getString(R.string.title_weight_lbs_oz)
                layoutWeightLbsOzs.visibility = View.VISIBLE
                edtWeightLbs.visibility = View.VISIBLE
                edtWeightOz.visibility = View.VISIBLE
            }

            "kgs" -> {
                txtPopupTitle.text = getString(R.string.title_weight_kgs)
                layoutWeightKgs.visibility = View.VISIBLE
                edtWeightKgs.visibility = View.VISIBLE
                edtWeightGrams.visibility = View.VISIBLE // Show grams input field
            }
            "inches" -> {
                txtPopupTitle.text = "Enter Length (Inches)"
                edtLengthInches.visibility = View.VISIBLE
            }
            "metric" -> {
                txtPopupTitle.text = "Enter Length (CM)"
                edtLengthCM.visibility = View.VISIBLE
            }
            else -> Log.e("PopupWeightEntry", "Unknown catchType: $catchType")
        }

        // Save button functionality
        btnSaveWeight.setOnClickListener {
            val resultIntent = Intent()
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            resultIntent.putExtra("selectedSpecies", selectedSpeciesValue)

            when (catchType) {
                "lbsOzs" -> {
                    val weightLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
                    val weightOz = edtWeightOz.text.toString().toIntOrNull() ?: 0
                    val totalWeightOz = (weightLbs * 16) + weightOz
                    resultIntent.putExtra("weightTotalOz", totalWeightOz)
                }
                "kgs" -> {
                    val weightKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
                    val weightGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
                    val totalWeightKgs = "$weightKgs.$weightGrams".toDoubleOrNull() ?: 0.0 // Store in decimal format
                    resultIntent.putExtra("weightTotalKgs", totalWeightKgs)
                }
                "inches" -> {
                    val lengthInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
                    resultIntent.putExtra("lengthTotalA8th", lengthInches * 8) // Convert to 8ths
                }
                "metric" -> {
                    val lengthCM = edtLengthCM.text.toString().toDoubleOrNull() ?: 0.0
                    resultIntent.putExtra("lengthTotalCM", lengthCM)
                }
            }

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
