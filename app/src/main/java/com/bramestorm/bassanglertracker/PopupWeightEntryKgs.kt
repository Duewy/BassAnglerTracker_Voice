package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper

class PopupWeightEntryKgs : Activity() {

    private var selectedSpecies: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_kgs)

        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        val edtWeightKgs: EditText = findViewById(R.id.edtWeightKgs)
        val edtWeightGrams: EditText = findViewById(R.id.edtWeightGrams)
        val btnSaveWeight: Button = findViewById(R.id.btnSaveWeight)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        loadSpeciesSpinner()

        edtWeightKgs.filters = arrayOf(MinMaxInputFilter(0, 99))
        edtWeightGrams.filters = arrayOf(MinMaxInputFilter(0, 99))

        btnSaveWeight.setOnClickListener {
            val resultIntent = Intent()

            val weightKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
            val weightGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
            val totalWeightHundredthKg = ((weightKgs * 100) + weightGrams)

            if (totalWeightHundredthKg == 0) {
                Toast.makeText(this, "Weight cannot be 0 Kgs!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultIntent.putExtra("weightTotalKg", totalWeightHundredthKg)
            resultIntent.putExtra("selectedSpecies", selectedSpecies)

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSpeciesSpinner()
    }

    private fun loadSpeciesSpinner() {
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerKgsSpeciesPopUp)

        val savedSpecies = SharedPreferencesManager.getSelectedSpeciesList(this).ifEmpty {
            SharedPreferencesManager.getMasterSpeciesList(this)
        }

        val speciesList = savedSpecies.map { speciesName ->
            val imageRes = SpeciesImageHelper.getSpeciesImageResId(speciesName)
            SpeciesItem(speciesName, imageRes)
        }

        Log.d("POPUP_SPINNER", "Species list reloaded: $speciesList")

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerSpecies.adapter = adapter

        if (speciesList.isNotEmpty()) {
            selectedSpecies = speciesList[0].name
        }

        spinnerSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedSpecies = speciesList[position].name
                Log.d("DB_DEBUG", "Species selected: $selectedSpecies")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSpecies = ""
            }
        }
    }

    class MinMaxInputFilter(private val min: Int, private val max: Int) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            return try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in min..max) null else ""
            } catch (e: NumberFormatException) {
                ""
            }
        }
    }
}
