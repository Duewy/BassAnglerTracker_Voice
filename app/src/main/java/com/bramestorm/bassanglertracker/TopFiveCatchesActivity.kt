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
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager.normalizeSpeciesName
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.getSpeciesImageResId
import com.bramestorm.bassanglertracker.utils.positionedToast
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
    private lateinit var radioPounds: RadioButton
    private lateinit var radioKgs: RadioButton
    private lateinit var radioInches: RadioButton
    private lateinit var radioCm: RadioButton

    private lateinit var btnFromDateTop5: Button
    private lateinit var btnToDateTop5: Button

    private var fromDate: String = ""
    private var toDate: String = ""

    private lateinit var txtMinUnits: TextView
    private lateinit var txtMaxUnits: TextView
    private lateinit var btnCancelSummary: Button


    private var results: List<CatchItem> = emptyList()

    private fun setupUnitRadioButtons() {
        val allRadios = listOf(radioLbs, radioPounds, radioKgs, radioInches, radioCm)

        fun selectRadio(selected: RadioButton) {
            allRadios.forEach { it.isChecked = (it == selected) }
            updateWeightHints()
        }

        radioLbs.setOnClickListener   { selectRadio(radioLbs) }
        radioPounds.setOnClickListener{ selectRadio(radioPounds) }
        radioKgs.setOnClickListener   { selectRadio(radioKgs) }
        radioInches.setOnClickListener{ selectRadio(radioInches) }
        radioCm.setOnClickListener    { selectRadio(radioCm) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_five_catches)

        spinnerSpecies = findViewById(R.id.spinnerSpeciesSummary)

        edtMinWeight = findViewById(R.id.edtMinWeight)
        edtMaxWeight = findViewById(R.id.edtMaxWeight)

        btnGetTop5 = findViewById(R.id.btnGetTop5)
        btnShareResults = findViewById(R.id.btnShareResults)
        listView = findViewById(R.id.listTopCatches)

        btnFromDateTop5 = findViewById(R.id.btnFromDateTop5)
        btnToDateTop5 = findViewById(R.id.btnToDateTop5)

        radioGroupUnits = findViewById(R.id.radioGroupUnits)
        radioLbs = findViewById(R.id.radioLbs)
        radioPounds = findViewById(R.id.radioPounds)
        radioKgs = findViewById(R.id.radioKgs)
        radioInches = findViewById(R.id.radioInches)
        radioCm = findViewById(R.id.radioCm)

        txtMinUnits = findViewById(R.id.txtMinUnits)
        txtMaxUnits = findViewById(R.id.txtMaxUnits)

        btnCancelSummary = findViewById(R.id.btnCancelSummary)

        //--  Select the To and From Dates ----
        btnFromDateTop5.setOnClickListener {
            showDatePicker { date ->
                fromDate = date
                btnFromDateTop5.text = "From: $date"
            }
        }

        btnToDateTop5.setOnClickListener {
            showDatePicker { date ->
                toDate = date
                btnToDateTop5.text = "To: $date"
            }
        }

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

        // Set initial selection
        radioLbs.isChecked = true

        // Wire up manual group logic
        setupUnitRadioButtons()

        // Make sure labels match the initial selection
        updateWeightHints()



        // Get the saved species names
        val speciesNames = SharedPreferencesManager.getSelectedSpeciesList(this)

        // Map them into SpeciesItem so we get name + image
        val speciesItems = speciesNames.map { name ->
            SpeciesItem(
                name = name,
                imageResId = getSpeciesImageResId(name)  // your existing helper
            )
        }

// Use your existing image+text adapter from SetUp
        val adapter = SpeciesSpinnerAdapter(this, speciesItems)
        spinnerSpecies.adapter = adapter



        btnCancelSummary.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }//----------- END OnCreate --------------------

    //------------ Hide # Key Pad ---------------
    private fun hideKeyboard() {
        val imm =
            getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val view = currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // ------- Set up Date Selection ---------
    private fun formatDate(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month + 1, day)
    }


    //------------ LOAD TOP CATCHES -------------------
    private fun loadTopCatches() {
        val db = CatchDatabaseHelper(this)
        val selectedSpecies = normalizeSpeciesName(spinnerSpecies.selectedItem.toString())
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
                db.getTopCatchesByLbsWithinDateRange(
                    species = selectedSpecies,
                    minOz = minOz,
                    maxOz = maxOz,
                    fromDate = fromDateOrDefault,
                    toDate = toDateOrDefault,
                    limit = 5
                )
            }

            "pounds" -> {
                val minPounds = (minValue * 100).toInt()
                val maxPounds = (maxValue * 100).toInt()

                db.getTopCatchesByPoundsWithinDateRange(
                    species = selectedSpecies,
                    minHundredthsPounds = minPounds,
                    maxHundredthsPounds = maxPounds,
                    fromDate = fromDateOrDefault,
                    toDate = toDateOrDefault,
                    limit = 5
                )

            }

            "kgs" -> {
                val minHg = (minValue * 100).toInt()
                val maxHg = (maxValue * 100).toInt()

                db.getTopCatchesByKgWithinDateRange(
                    species = selectedSpecies,
                    minHundredthsKg = minHg, // renamed var
                    maxHundredthsKg = maxHg,
                    fromDate = fromDateOrDefault,
                    toDate = toDateOrDefault,
                    limit = 5
                )

            }

            "inches" -> {
                val minQuarters = (minValue * 4).toInt()
                val maxQuarters = (maxValue * 4).toInt()

                db.getTopCatchesByInchesWithinDateRange(
                    species = selectedSpecies,
                    minQuarters = minQuarters,
                    maxQuarters = maxQuarters,
                    fromDate = fromDateOrDefault,
                    toDate = toDateOrDefault,
                    limit = 5
                )
            }

            "cm" -> {
                val minTenths = (minValue * 10).toInt()
                val maxTenths = (maxValue * 10).toInt()

                db.getTopCatchesByCmWithinDateRange(
                    species = selectedSpecies,
                    minTenths = minTenths,
                    maxTenths = maxTenths,
                    fromDate = fromDateOrDefault,
                    toDate = toDateOrDefault,
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

        val displayList = results.map { catch ->
            val formattedValue = when (unitType) {
                "lbs" -> {
                    // stored as totalWeightOz
                    formatWeightOzToLbsOz(catch.totalWeightOz ?: 0)
                }

                "pounds" -> {
                    catch.totalWeightHundredthPounds?.let { pounds ->
                        if (pounds > 0) {
                            formatWeightPounds(this@TopFiveCatchesActivity, pounds)
                        } else {
                            ""
                        }
                    } ?: ""
                }

                "kgs" -> {
                    catch.totalWeightHundredthKg?.let { kg ->
                        if (kg > 0) {
                            formatWeightKg(this@TopFiveCatchesActivity, kg)
                        } else {
                            ""
                        }
                    } ?: ""
                }

                "inches" -> {
                    catch.totalLengthQuarters?.let { quarters ->
                        if (quarters > 0) {
                            formatLengthQuartersToInches(quarters)
                        } else {
                            ""
                        }
                    } ?: ""
                }

                "cm" -> {
                    catch.totalLengthTenths?.let { cm ->
                        if (cm > 0) {
                            formatLengthCm(this@TopFiveCatchesActivity, cm)
                        } else {
                            ""
                        }
                    } ?: ""
                }

                else -> ""
            }

            "${catch.dateTime} – $formattedValue"
        }
        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayList
        )
    }


    //---------------- SHARE CSV -----------------------
    private fun shareResultsAsCsv() {
        if (results.isEmpty()) {
            positionedToast("No results to share.")
            return
        }


        val csvBuilder = StringBuilder()
        csvBuilder.append("Date,Species,Weight (lbs/oz), Weight (pounds) ,Weight (kg),Length (in),Length (cm),Catch Type,Marker Type,Latitude,Longitude\n")

        results.forEach {
            val weightLbs = formatWeightOzToLbsOz(it.totalWeightOz ?: 0)

            val weightPounds =  it.totalWeightHundredthPounds?.let{ pounds ->
                if (pounds > 0) formatWeightPounds( this@TopFiveCatchesActivity, pounds) else ""
            }?:""

            val weightKg = it.totalWeightHundredthKg?.let { kg ->
                if (kg > 0) formatWeightKg(this@TopFiveCatchesActivity, kg) else ""
            } ?: ""

            val lengthIn = it.totalLengthQuarters?.let { a4th ->
                if (a4th > 0) formatLengthQuartersToInches(a4th) else ""
            } ?: ""

            val lengthCm = it.totalLengthTenths?.let { cm ->
                if (cm > 0) formatLengthCm(this@TopFiveCatchesActivity, cm) else ""
            } ?: ""

                csvBuilder.append(
                    "${it.dateTime}," +
                        "${it.species}," +
                        "$weightLbs," +
                        "$weightPounds," +
                        "$weightKg," +
                        "$lengthIn," +
                        "$lengthCm," +
                        "${it.catchType}," +
                        "${it.markerType ?: ""}," +
                        "${it.latitude ?: ""}," +
                        "${it.longitude ?: ""}\n"
            )
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
            radioPounds.isChecked -> "pounds"
            radioKgs.isChecked -> "kgs"
            radioInches.isChecked -> "inches"
            radioCm.isChecked -> "cm"
            else -> "lbs" // default go to units
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
            "pounds" -> {
                edtMinWeight.hint = "Min Weight"
                edtMaxWeight.hint = "Max Weight"
                txtMinUnits.text = "pounds"
                txtMaxUnits.text = "pounds"
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

    //---------- Date picker for From / To (same as PopupQueryDate) ----------
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                // uses your date_iso string resource: "%04d-%02d-%02d"
                val selectedDate = getString(R.string.date_iso, year, month + 1, day)
                onDateSelected(selectedDate)
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Default = from Jan 1 of this year to today, if user didn't pick dates
    private val fromDateOrDefault: String
        get() = if (fromDate.isNotBlank()) fromDate else getDefaultFromDate()

    private val toDateOrDefault: String
        get() = if (toDate.isNotBlank()) toDate else getDefaultToDate()

    private fun getDefaultFromDate(): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        return String.format(
            "%04d-%02d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    private fun getDefaultToDate(): String {
        val cal = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }



}//----------------------- END -----------------------
