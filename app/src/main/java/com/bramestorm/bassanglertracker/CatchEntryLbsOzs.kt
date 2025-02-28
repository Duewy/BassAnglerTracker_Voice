package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryLbsOzs : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var lbsSpinner: Spinner
    private lateinit var ozSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_lbs_ozs)

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp3)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        speciesSpinner = findViewById(R.id.speciesSpinner)
        lbsSpinner = findViewById(R.id.lbsSpinner)
        ozSpinner = findViewById(R.id.ozSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)

        dbHelper = CatchDatabaseHelper(this)

        catchAdapter = CatchItemAdapter(this, catchList)
        listView.adapter = catchAdapter

        loadCatches()

        val lbsList = (0..50).toList()
        lbsSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lbsList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val ozList = (0..15).toList()
        ozSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ozList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        saveButton.setOnClickListener {
            try {
                val species = speciesSpinner.selectedItem.toString()
                val weightLbs = lbsSpinner.selectedItem.toString().toInt()
                val weightOz = ozSpinner.selectedItem.toString().toInt()
                val totalWeightOz = (weightLbs * 16) + weightOz // ✅ Store weight as total ounces
                val dateTime = getCurrentTimestamp()

                val newCatch = CatchItem(
                    id = 0,
                    dateTime = dateTime,
                    species = species,
                    totalWeightOz = totalWeightOz,
                    totalLengthA8th = null,
                    weightDecimalTenthKg = null,  // ✅ Fix: Add null for metric weight
                    lengthDecimalTenthCm = null,
                    catchType = "weight_imperial"
                )


                dbHelper.insertCatch(newCatch)
                catchList.add(0, newCatch)
                catchAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

                speciesSpinner.setSelection(0)
                lbsSpinner.setSelection(0)
                ozSpinner.setSelection(0)
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving catch: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val fetchedCatches = dbHelper.getAllCatches().filter { it.catchType == "weight_imperial" }
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
