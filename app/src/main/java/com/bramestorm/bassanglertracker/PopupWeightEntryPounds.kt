package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper

class PopupWeightEntryPounds : Activity() {

    private var selectedSpecies: String = ""

    companion object {
        const val EXTRA_WEIGHT_POUNDS = "weightTotalPounds"
        const val EXTRA_SPECIES   = "selectedSpecies"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        setContentView(R.layout.popup_weight_entry_pounds)

        val edtWeightLbs: EditText = findViewById(R.id.edtWeightPounds)
        val edtWeightDec: EditText = findViewById(R.id.edtWeightDec)
        val btnSaveWeight: Button = findViewById(R.id.btnSaveWeight)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        loadSpeciesSpinner() // Populates spinner and sets up listener

        edtWeightLbs.filters = arrayOf(MinMaxInputFilter(0, 99))
        edtWeightDec.filters = arrayOf(MinMaxInputFilter(0, 99))

        btnSaveWeight.setOnClickListener {
            val resultIntent = Intent()
            val weightLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
            val weightDec = edtWeightDec.text.toString().toIntOrNull() ?: 0
            val totalWeightPounds = (weightLbs * 100) + weightDec

            if (totalWeightPounds == 0) {
                Toast.makeText(this, "Weight cannot be 0.00 Pounds!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultIntent.putExtra(EXTRA_WEIGHT_POUNDS, totalWeightPounds)
            resultIntent.putExtra(EXTRA_SPECIES, selectedSpecies)

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        edtWeightLbs.requestFocus()
        edtWeightLbs.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(edtWeightLbs, InputMethodManager.SHOW_IMPLICIT)
        }
    }
//------------- END On Create ---------------------------------

    // -------------- Min & Max Input Filter to enforce value limits --------------------------
    class MinMaxInputFilter(private val min: Int, private val max: Int) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in min..max) {
                    return null // Accept input
                }
            } catch (e: NumberFormatException) {
                // Ignore invalid input
            }
            return "" // Reject input if out of range
        }
    }

    override fun onResume() {
        super.onResume()
        loadSpeciesSpinner()
    }

    private fun loadSpeciesSpinner() {
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerInchesSpeciesPopUp)

        val speciesList = SharedPreferencesManager
            .getSpeciesCatalogue(this)
            .map { speciesName ->
                SpeciesItem(
                    speciesName,
                    SpeciesImageHelper.getSpeciesImageResId(speciesName)
                )
            }

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerSpecies.adapter = adapter

        if (speciesList.isNotEmpty()) {
            selectedSpecies = speciesList[0].name
        }

        spinnerSpecies.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedSpecies = speciesList[position].name
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedSpecies = ""
                }
            }
    }

}
