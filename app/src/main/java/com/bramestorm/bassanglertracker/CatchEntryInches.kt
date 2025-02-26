package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryInches : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var inchesSpinner: Spinner
    private lateinit var eighthSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var setUpButton: Button
    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_inches)

        // Initialize views
        speciesSpinner = findViewById(R.id.speciesSpinner)
        inchesSpinner = findViewById(R.id.inchesSpinner)
        eighthSpinner = findViewById(R.id.eightSpinner)
         saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)
        setUpButton = findViewById(R.id.btnSetUpInches)

        // Initialize database helper and custom adapter
        dbHelper = CatchDatabaseHelper(this)
        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        // Load existing length (imperial) catches from the database
        loadCatches()

        // Setup spinner for inches (values 0 to 50)
        val inchesList = (0..50).toList()
        val inchesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, inchesList)
        inchesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inchesSpinner.adapter = inchesAdapter

        // Setup spinner for 1/8 increments (values 0 to 7)
        val eighthList = (0..7).toList()
        val eighthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eighthList)
        eighthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        eighthSpinner.adapter = eighthAdapter

        // Save new catch when saveButton is clicked
        saveButton.setOnClickListener {
            try {
                val species = speciesSpinner.selectedItem.toString()
                val inches = inchesSpinner.selectedItem.toString().toInt()
                val eighth = eighthSpinner.selectedItem.toString().toInt()
                // Calculate the final length: inches plus fractional part (eighth/8)
                val lengthDecimal = inches.toFloat() + (eighth / 8f)
                val dateTime = getCurrentTimestamp()

                val newCatch = CatchItem(
                    id = 0,
                    dateTime = dateTime,
                    species = species,
                    weightLbs = null,
                    weightOz = null,
                    weightDecimal = null,
                    lengthA8th = eighth,
                    lengthInches = inches,
                    lengthDecimal = lengthDecimal,
                    catchType = "lengthInches"
                )

                // Insert the new catch into the database and update our list
                dbHelper.insertCatch(newCatch)
                catchList.add(0, newCatch)
                catchAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

                // Reset spinners to initial positions
                speciesSpinner.setSelection(0)
                inchesSpinner.setSelection(0)
                eighthSpinner.setSelection(0)
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving catch: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // ListView item click for edit/delete (currently only delete)
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
        }

        // Button to navigate back to the SetUp page
        setUpButton.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val fetchedCatches = dbHelper.getAllCatches().filter { it.catchType == "length_imperial" }
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
