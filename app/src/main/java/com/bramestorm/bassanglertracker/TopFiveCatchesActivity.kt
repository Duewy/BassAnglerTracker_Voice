package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.util.positionedToast
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.io.File
import java.io.FileOutputStream

class TopFiveCatchesActivity : AppCompatActivity() {

    private lateinit var spinnerSpecies: Spinner
    private lateinit var edtMinWeight: EditText
    private lateinit var edtMaxWeight: EditText
    private lateinit var btnGetTop5: Button
    private lateinit var btnShareResults: Button
    private lateinit var listView: ListView
    private lateinit var radioGroupUnits: RadioGroup
    private lateinit var radioLbs: RadioButton
    private lateinit var radioKgs: RadioButton
    private lateinit var radioInches: RadioButton
    private lateinit var radioCm: RadioButton
    private lateinit var txtMinUnits: TextView
    private lateinit var txtMaxUnits: TextView
    private lateinit var btnCancelSummary: Button




    private var results: List<CatchItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_five_catches)

        spinnerSpecies = findViewById(R.id.spinnerSpeciesSummary)
        edtMinWeight = findViewById(R.id.edtMinWeight)
        edtMaxWeight = findViewById(R.id.edtMaxWeight)
        btnGetTop5 = findViewById(R.id.btnGetTop5)
        btnShareResults = findViewById(R.id.btnShareResults)
        listView = findViewById(R.id.listTopCatches)
        radioGroupUnits = findViewById(R.id.radioGroupUnits)
        radioLbs = findViewById(R.id.radioLbs)
        radioKgs = findViewById(R.id.radioKgs)
        radioInches = findViewById(R.id.radioInches)
        radioCm = findViewById(R.id.radioCm)
        txtMinUnits = findViewById(R.id.txtMinUnits)
        txtMaxUnits = findViewById(R.id.txtMaxUnits)
        btnCancelSummary = findViewById(R.id.btnCancelSummary)

// When user presses "Done" on edtMinWeight, move to edtMaxWeight
        edtMinWeight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                edtMaxWeight.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

// When user presses "Done" on edtMaxWeight, hide keyboard
        edtMaxWeight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                edtMaxWeight.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }

// Hide keyboard when search button clicked
        btnGetTop5.setOnClickListener {
            hideKeyboard()
            loadTopCatches()
        }

// Also hide for Share Results
        btnShareResults.setOnClickListener {
            hideKeyboard()
            shareResultsAsCsv()
        }



        radioGroupUnits.setOnCheckedChangeListener { _, _ ->
            updateWeightHints() // update when unit selection changes
        }

        // ...findViewById calls for all views...

        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val adapter = ArrayAdapter(this, R.layout.spinner_item_summary, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // optional: customize later if needed
        spinnerSpecies.adapter = adapter


        btnGetTop5.setOnClickListener {
            loadTopCatches()
        }

        btnShareResults.setOnClickListener {
            shareResultsAsCsv()
        }

        btnCancelSummary.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }//----------- END OnCreate --------------------

    //------------ Hide # Key Pad ---------------
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val view = currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    //------------ LOAD TOP CATCHES -------------------
    private fun loadTopCatches() {
        val db = CatchDatabaseHelper(this)
        val selectedSpecies = spinnerSpecies.selectedItem.toString()
        val unitType = getSelectedMeasurementType()

        val minValue = edtMinWeight.text.toString().toFloatOrNull() ?: 0f
        val maxValue = edtMaxWeight.text.toString().toFloatOrNull() ?: 99999f

        if (minValue > maxValue) {
            positionedToast("⚠️ Minimum value cannot be greater than maximum.")
            return
        }


        val resultsByUnit = when (unitType) {
            "lbs" -> {
                val minOz = (minValue * 16).toInt()
                val maxOz = (maxValue * 16).toInt()
                db.getTopCatchesForSpeciesThisMonth(
                    species = selectedSpecies,
                    minOz = minOz,
                    maxOz = maxOz,
                    limit = 5
                )
            }
            "kgs" -> {
                val minHg = (minValue * 100).toInt()
                val maxHg = (maxValue * 100).toInt()

                db.getTopCatchesByKgForSpeciesThisMonth(
                    species = selectedSpecies,
                    minHundredthsKg = minHg, // renamed var
                    maxHundredthsKg = maxHg,
                    limit = 5
                )

            }
            "inches" -> {
                val min8ths = (minValue * 8).toInt()
                val max8ths = (maxValue * 8).toInt()

                db.getTopCatchesByInchesForSpeciesThisMonth(
                    species = selectedSpecies,
                    min8ths = min8ths,
                    max8ths = max8ths,
                    limit = 5
                )
            }
            "cm" -> {
                val minTenths = (minValue * 10).toInt()
                val maxTenths = (maxValue * 10).toInt()
                db.getTopCatchesByCmForSpeciesThisMonth(
                    species = selectedSpecies,
                    minTenths = minTenths,
                    maxTenths = maxTenths,
                    limit = 5
                )
            }
            else -> emptyList()
        }

        results = resultsByUnit

        if (results.isEmpty()) {
            listView.adapter = null
            positionedToast("❌ No catches matched your search.")
            return
        }

        val displayList = results.map {
            val weight = formatWeightOzToLbsOz(it.totalWeightOz ?: 0)
            "${it.dateTime} - $weight"
        }

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
    }


    //---------------- SHARE CSV -----------------------
    private fun shareResultsAsCsv() {
        if (results.isEmpty()) {
            positionedToast("No results to share.")
            return
        }


        val csvBuilder = StringBuilder()
        csvBuilder.append("Date,Species,Weight (lbs/oz),Weight (kg),Length (in),Length (cm),Catch Type,Marker Type,Latitude,Longitude\n")

        results.forEach {
            val weightLbs = formatWeightOzToLbsOz(it.totalWeightOz ?: 0)

            val weightKg = it.totalWeightHundredthKg?.let { kg ->
                if (kg > 0) formatWeightKg(this@TopFiveCatchesActivity, kg) else ""
            } ?: ""

            val lengthIn = it.totalLengthA8th?.let { a8th ->
                if (a8th > 0) formatLengthA8thToInches(a8th) else ""
            } ?: ""

            val lengthCm = it.totalLengthTenths?.let { cm ->
                if (cm > 0) formatLengthCm(this@TopFiveCatchesActivity, cm) else ""
            } ?: ""

            csvBuilder.append(
                csvBuilder.append(
                    "${it.dateTime}," +
                        "${it.species}," +
                        "$weightLbs," +
                        "$weightKg," +
                        "$lengthIn," +
                        "$lengthCm," +
                        "${it.catchType}," +
                        "${it.markerType ?: ""}," +
                        "${it.latitude ?: ""}," +
                        "${it.longitude ?: ""}\n"
            ))
        }

        val fileName = "Top5Catches_${System.currentTimeMillis()}.csv"
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use {
            it.write(csvBuilder.toString().toByteArray())
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                this@TopFiveCatchesActivity,
                "$packageName.fileprovider",
                file
            ))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "📤 Share CSV via"))
    }

    //----------- GET Measurement Type ----------------------

    private fun getSelectedMeasurementType(): String {
        return when {
            radioLbs.isChecked -> "lbs"
            radioKgs.isChecked -> "kgs"
            radioInches.isChecked -> "inches"
            radioCm.isChecked -> "cm"
            else -> "lbs"
        }
    }

//------------------ MIN and MAX UNITS text -----------------------
    private fun updateWeightHints() {
        when (getSelectedMeasurementType()) {
            "lbs" -> {
                edtMinWeight.hint = "Min Weight"
                edtMaxWeight.hint = "Max Weight"
                txtMinUnits.text = "Lbs"
                txtMaxUnits.text = "Lbs"
            }
            "kgs" -> {
                edtMinWeight.hint = "Min Weight"
                edtMaxWeight.hint = "Max Weight"
                txtMinUnits.text = "kgs"
                txtMaxUnits.text = "kgs"
            }
            "inches" -> {
                edtMinWeight.hint = "Min Length"
                edtMaxWeight.hint = "Max Length"
                txtMinUnits.text = "in"
                txtMaxUnits.text = "in"
            }
            "cm" -> {
                edtMinWeight.hint = "Min Length"
                edtMaxWeight.hint = "Max Length"
                txtMinUnits.text = "cm"
                txtMaxUnits.text = "cm"
            }
        }
    }



}//----------------------- END -----------------------
