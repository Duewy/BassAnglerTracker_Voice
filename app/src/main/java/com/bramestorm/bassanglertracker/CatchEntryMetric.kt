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

class CatchEntryMetric : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var tensSpinner: Spinner    // Represents tens of centimeters (0–9; multiplied by 10)
    private lateinit var cmSpinner: Spinner      // Represents ones of centimeters (0–9)
    private lateinit var decimalSpinner: Spinner // Represents the decimal part (0–9) for centimeters
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var setUpButton: Button

    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_metric)

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUpCM)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }

        // Initialize views.
        speciesSpinner = findViewById(R.id.speciesSpinner)
        tensSpinner = findViewById(R.id.tensSpinner)
        cmSpinner = findViewById(R.id.centimeterSpinner)
        decimalSpinner = findViewById(R.id.decimalSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)

        dbHelper = CatchDatabaseHelper(this)

        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        // Set up the species spinner using an array resource.
        val speciesAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.species_list,  // Ensure this resource is defined and nonempty in res/values/arrays.xml
            android.R.layout.simple_spinner_item
        )
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        // Set up the list adapter.
        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        loadCatches()

        // Setup spinner for tens (0 to 9) -- representing tens of centimeters.
        val tensList = (0..9).toList()
        val tensAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tensList)
        tensAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tensSpinner.adapter = tensAdapter

        // Setup spinner for ones of centimeters (0 to 9)
        val cmList = (0..9).toList()
        val cmAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cmList)
        cmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cmSpinner.adapter = cmAdapter

        // Setup spinner for the decimal part (0 to 9)
        val decimalList = (0..9).toList()
        val decAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, decimalList)
        decAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        decimalSpinner.adapter = decAdapter


        // &&&&&&&&&&&& SAVE CATCH BUTTON    &&&&&&&&&&&&&&&&&&&&&&&&&&

        saveButton.setOnClickListener {
            try {
                val species = speciesSpinner.selectedItem.toString()
                val tensValue = tensSpinner.selectedItem.toString().toInt()
                val cmValue = cmSpinner.selectedItem.toString().toInt()
                val decimalValue = decimalSpinner.selectedItem.toString().toInt()
                val tensCm = tensValue * 10  // tens spinner value multiplied by 10
                val onesCm = cmValue
                val decimalPart = decimalValue
                                            // Calculate length: tensCm + onesCm + (decimalPart/10)
                val lengthCm = (tensCm.toFloat() + onesCm.toFloat() + (decimalPart / 10f))
                val dateTime = getCurrentTimestamp()


                val newCatch = CatchItem(
                    id = 0,
                    dateTime = dateTime,
                    species = species,
                    weightLbs = null,
                    weightOz = null,
                    weightDecimal = null,
                    lengthA8th = null,
                    lengthInches = null,
                    lengthDecimal = lengthCm,
                    catchType = "length_metric"
                )

                dbHelper.insertCatch(newCatch)
                // Add the new catch to our list
                catchList.add(0, newCatch)
                catchAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

        // ******  Reset spinners. *************
                speciesSpinner.setSelection(0)
                tensSpinner.setSelection(0)
                cmSpinner.setSelection(0)
                decimalSpinner.setSelection(0)
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving catch: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            showEditDeleteDialog(catchList[position])
        }

    }


        private fun loadCatches() {
            catchList.clear()
            val fetchedCatches = dbHelper.getAllCatches().filter { it.catchType == "length_metric\"" }
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
