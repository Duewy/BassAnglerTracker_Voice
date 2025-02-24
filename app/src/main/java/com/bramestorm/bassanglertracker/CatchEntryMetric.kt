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
    private lateinit var centimeterSpinner: Spinner
    private lateinit var decSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var catchAdapter: ArrayAdapter<String>
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_metric)

        speciesSpinner = findViewById(R.id.speciesSpinner)
        centimeterSpinner = findViewById(R.id.centimeterSpinner)
        decSpinner = findViewById(R.id.decSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)

        dbHelper = CatchDatabaseHelper(this)


        // Centimeter Spinner (Example: 1- 99 Cm)
        val cmList = (1..99).toList()
        val cmAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cmList)
        cmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        centimeterSpinner.adapter = cmAdapter

        // dec  Spinner (0 to 9 for dec increments)
        val decList = (0..9).toList()
        val decAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, decList)
        decAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        decSpinner.adapter = decAdapter


        catchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchList.map { it.toString() })
        listView.adapter = catchAdapter

        loadCatches()

        saveButton.setOnClickListener {
            val species = speciesSpinner.selectedItem.toString()
            val length_Centimeters = centimeterSpinner.selectedItem.toString().toInt()
            val length_CmDec = decSpinner.selectedItem.toString().toInt()
            val length_TotalCm = length_Centimeters + (length_CmDec / 10.0).toFloat()
            val dateTime = getCurrentTimestamp()

            val newCatch = CatchItem(0, dateTime, species, length_Centimeters, length_CmDec, length_TotalCm)
            dbHelper.insertCatch(dateTime, species, length_Centimeters, length_CmDec, length_TotalCm)

            catchList.add(0, newCatch)
            catchAdapter.notifyDataSetChanged()

            Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

            speciesSpinner.setSelection(0)
            centimeterSpinner.setSelection(0)
            decSpinner.setSelection(0)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
        }

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUpCM)
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

