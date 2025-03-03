package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryKgs : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var weightKgSpinner: Spinner
    private lateinit var tenthSpinner: Spinner
    private lateinit var hundredthSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var setUpButton: Button
    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Set up species spinner
        val speciesAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.species_list,
            android.R.layout.simple_spinner_item
        )
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        // Set up spinners for weight
        val kgList = (0..9).toList()
        weightKgSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kgList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val tenthList = (0..9).toList()
        tenthSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tenthList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val hundredthList = (0..9).toList()
        hundredthSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hundredthList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        loadCatches()

        saveButton.setOnClickListener {
            try {
                val species = speciesSpinner.selectedItem.toString()
                val wholeKg = weightKgSpinner.selectedItem.toString().toInt()
                val tenth = tenthSpinner.selectedItem.toString().toInt()
                val hundredth = hundredthSpinner.selectedItem.toString().toInt()
                val totalWeightHundredthKg = (wholeKg * 100) + (tenth * 10) + hundredth
                val colorList = listOf("clip_red", "clip_yellow", "clip_green", "clip_blue", "clip_white", "clip_orange")
                val existingCatches = dbHelper.getCatchCount()
                val assignedColor = colorList[existingCatches % colorList.size] // Cycle through colors
                val dateTime = getCurrentTimestamp()

                val newCatch = CatchItem(
                    id = 0,
                    dateTime = dateTime,
                    species = species,
                    totalWeightOz = null,
                    totalLengthA8th = null,
                    totalWeightHundredthKg = totalWeightHundredthKg,
                    lengthDecimalTenthCm = null,
                    clipColor = null,
                    catchType = "kgs"
                )


                dbHelper.insertCatch(newCatch)
                catchList.add(0, newCatch)
                catchAdapter.notifyDataSetChanged()

                Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

                // Reset spinners
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
        val fetchedCatches = dbHelper.getAllCatches().filter { it.catchType == "kgs" }

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