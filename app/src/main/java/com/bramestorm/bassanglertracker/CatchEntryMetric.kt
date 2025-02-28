package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryMetric : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var cmSpinner: Spinner
    private lateinit var tensSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var setUpButton: Button
    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_metric)

        speciesSpinner = findViewById(R.id.speciesSpinner)
        cmSpinner = findViewById(R.id.centimeterSpinner)
        tensSpinner = findViewById(R.id.tensSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)
        setUpButton = findViewById(R.id.btnSetUpCM)

        dbHelper = CatchDatabaseHelper(this)
        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        loadCatches()

        // Spinner setup for centimeters (0 - 200 cm)
        val cmList = (0..200).toList()
        val cmAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cmList)
        cmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cmSpinner.adapter = cmAdapter

        // Spinner setup for tenths of a cm (0 - 9)
        val tenthList = (0..9).toList()
        val tenthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tenthList)
        tenthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tensSpinner.adapter = tenthAdapter

        saveButton.setOnClickListener {
            try {
                val species = speciesSpinner.selectedItem.toString()
                val cm = cmSpinner.selectedItem.toString().toInt()
                val tenthCm = tensSpinner.selectedItem.toString().toInt()
                val totalCmTenth = (cm * 10) + tenthCm // ✅ Store total in tenths of cm
                val dateTime = getCurrentTimestamp()

                val newCatch = CatchItem(
                    id = 0,
                    dateTime = dateTime,
                    species = species,
                    totalWeightOz = null,
                    totalLengthA8th = null,
                    weightDecimalTenthKg = null,
                    lengthDecimalTenthCm = totalCmTenth, // ✅ Store metric length in tenths
                    catchType = "length_metric"
                )
                dbHelper.insertCatch(newCatch)
                catchList.add(0, newCatch)
                catchAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

                // Reset spinners
                speciesSpinner.setSelection(0)
                cmSpinner.setSelection(0)
                tensSpinner.setSelection(0)

            } catch (e: Exception) {
                Toast.makeText(this, "Error saving catch: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
        }

        setUpButton.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val fetchedCatches = dbHelper.getAllCatches().filter { it.catchType == "length_metric" }
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
