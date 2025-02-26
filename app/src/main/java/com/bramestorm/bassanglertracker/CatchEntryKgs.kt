package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryKgs : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var weightKgSpinner: Spinner      // Whole kg (0–9)
    private lateinit var tenthSpinner: Spinner           // Tenths (0–9)
    private lateinit var hundredthSpinner: Spinner       // Hundredths (0–9)
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var setUpButton: Button

    // Using a custom adapter that directly uses a mutable list of CatchItem objects.
    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()

    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure the layout file name is exactly as used in your project.
        setContentView(R.layout.activity_catch_entry_kgs)

        // Initialize views
        speciesSpinner = findViewById(R.id.speciesSpinner)
        weightKgSpinner = findViewById(R.id.weightKgSpinner)
        tenthSpinner = findViewById(R.id.decimalSpinner)
        hundredthSpinner = findViewById(R.id.hundredthSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)
        setUpButton = findViewById(R.id.btnSetUp3)

        dbHelper = CatchDatabaseHelper(this)

        // Set up species spinner using an array resource
        val speciesAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.species_list, // Ensure this array resource exists in res/values/arrays.xml
            android.R.layout.simple_spinner_item
        )
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        // Optional: add an onItemSelectedListener for debugging species selection
        speciesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                Log.d("CatchEntryKgs", "Species selected: ${speciesSpinner.selectedItem}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up spinners for weight
        val kgList = (0..9).toList()
        val kgAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kgList)
        kgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weightKgSpinner.adapter = kgAdapter

        val tenthList = (0..9).toList()
        val tenthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tenthList)
        tenthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tenthSpinner.adapter = tenthAdapter

        val hundredthList = (0..9).toList()
        val hundredthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hundredthList)
        hundredthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        hundredthSpinner.adapter = hundredthAdapter

        // Use the custom adapter for the ListView that directly uses our mutable catchList.
        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        loadCatches()

        saveButton.setOnClickListener {
            try {
                val species = speciesSpinner.selectedItem.toString()
                val wholeKg = weightKgSpinner.selectedItem.toString().toInt()
                val tenth = tenthSpinner.selectedItem.toString().toInt()
                val hundredth = hundredthSpinner.selectedItem.toString().toInt()
                // Calculate final weight, e.g., 3 + 0.7 + 0.08 = 3.78 kg
                val weightKg = wholeKg.toFloat() + (tenth / 10f) + (hundredth / 100f)
                val dateTime = getCurrentTimestamp()

                val newCatch = CatchItem(
                    id = 0,
                    dateTime = dateTime,
                    species = species,
                    weightLbs = null,
                    weightOz = null,
                    weightDecimal = weightKg,
                    lengthA8th = null,
                    lengthInches = null,
                    lengthDecimal = null,
                    catchType = "weight_metric"
                )
                dbHelper.insertCatch(newCatch)
                catchList.add(0, newCatch)
                catchAdapter.notifyDataSetChanged()

                Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

                // Reset spinners to first item (index 0)
                speciesSpinner.setSelection(0)
                weightKgSpinner.setSelection(0)
                tenthSpinner.setSelection(0)
                hundredthSpinner.setSelection(0)
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving catch: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            showEditDeleteDialog(catchList[position])
        }

        setUpButton.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val fetchedCatches = dbHelper.getAllCatches().filter { it.catchType == "weight_metric" }
        if (fetchedCatches.isEmpty()) {
            Toast.makeText(this, "No catches found", Toast.LENGTH_SHORT).show()
        }
        catchList.addAll(fetchedCatches)
        catchAdapter.notifyDataSetChanged()
    }

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ ->
                Toast.makeText(this, "Edit feature coming soon!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Delete") { _, _ ->
                dbHelper.deleteCatch(catchItem.id)
                loadCatches()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
