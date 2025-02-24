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
    private lateinit var catchAdapter: ArrayAdapter<String>
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_lbs_ozs)

        speciesSpinner = findViewById(R.id.speciesSpinner)
        lbsSpinner = findViewById(R.id.lbsSpinner)
        ozSpinner = findViewById(R.id.ozSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)

        dbHelper = CatchDatabaseHelper(this)

        catchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchList.map { it.toString() })
        listView.adapter = catchAdapter

        loadCatches()

        // Inches Spinner (Example: 1-50 inches)
        val lbsList = (0..50).toList()
        val lbsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lbsList)
        lbsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        lbsSpinner.adapter = lbsAdapter

// 8th Increments Spinner (0 to 7 for 1/8th increments)
        val decList = (0..7).toList()
        val decAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, decList)
        decAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ozSpinner.adapter = decAdapter

        saveButton.setOnClickListener {
            val species = speciesSpinner.selectedItem.toString()
            val weightLbs = lbsSpinner.selectedItem.toString().toInt()
            val weightOz = ozSpinner.selectedItem.toString().toInt()
            val weightDecimal = (weightLbs.toFloat() + (weightOz / 16f)) // ✅ Now everything is Float
            val dateTime = getCurrentTimestamp()

            val newCatch = CatchItem(0, dateTime, species, weightLbs, weightOz, weightDecimal.toFloat())
            dbHelper.insertCatch(dateTime, species, weightLbs, weightOz, weightDecimal.toFloat())

            catchList.add(0, newCatch)
            catchAdapter.notifyDataSetChanged()

            Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

            speciesSpinner.setSelection(0)
            lbsSpinner.setSelection(0)
            ozSpinner.setSelection(0)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
        }

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp3)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val fetchedCatches = dbHelper.getAllCatches()

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
