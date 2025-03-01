package com.bramestorm.bassanglertracker


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.bramestorm.bassanglertracker.R

class PopupWeightEntry : Activity() {
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry)

        // Retrieve intent extras
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""

        // UI elements
        val txtPopupTitle: TextView = findViewById(R.id.txtPopupTitle)
        val edtWeightLbs: EditText = findViewById(R.id.edtWeightLbs)
        val edtWeightOz: EditText = findViewById(R.id.edtWeightOz)
        val edtWeightKgs: EditText = findViewById(R.id.edtWeightKgs)
        val edtLengthInches: EditText = findViewById(R.id.edtLengthInches)
        val edtLengthCM: EditText = findViewById(R.id.edtLengthCM)
        val btnSaveWeight: Button = findViewById(R.id.btnSaveWeight)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerSpecies)
        val layoutWeightLbsOzs: LinearLayout = findViewById(R.id.layoutWeightLbsOzs)

        // Populate Species Spinner
        val speciesList = arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, speciesList)
        spinnerSpecies.adapter = adapter

        // If Tournament mode, preselect species from intent
        if (isTournament) {
            val speciesIndex = speciesList.indexOf(selectedSpecies).takeIf { it >= 0 } ?: 0
            spinnerSpecies.setSelection(speciesIndex)
            spinnerSpecies.isEnabled = false  // Lock species selection in tournament mode
        }

        // Adjust UI based on catch type
        edtWeightLbs.visibility = View.GONE
        edtWeightOz.visibility = View.GONE
        edtWeightKgs.visibility = View.GONE
        edtLengthInches.visibility = View.GONE
        edtLengthCM.visibility = View.GONE
        layoutWeightLbsOzs.visibility = View.GONE

        when (catchType) {
            "lbsOzs" -> {
                txtPopupTitle.text = "Enter Weight (Lbs/Oz)"
                layoutWeightLbsOzs.visibility = View.VISIBLE
                edtWeightLbs.visibility = View.VISIBLE
                edtWeightOz.visibility = View.VISIBLE
            }
            "kgs" -> {
                txtPopupTitle.text = "Enter Weight (Kgs)"
                edtWeightKgs.visibility = View.VISIBLE
            }
            "inches" -> {
                txtPopupTitle.text = "Enter Length (Inches)"
                edtLengthInches.visibility = View.VISIBLE
            }
            "metric" -> {
                txtPopupTitle.text = "Enter Length (CM)"
                edtLengthCM.visibility = View.VISIBLE
            }
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
                    val weightKgs = edtWeightKgs.text.toString().toDoubleOrNull() ?: 0.0
                    resultIntent.putExtra("weightTotalKgs", weightKgs)
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
