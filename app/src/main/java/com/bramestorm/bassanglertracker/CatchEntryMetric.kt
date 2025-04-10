package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryMetric : AppCompatActivity() {

    private lateinit var btnSetUp3Cm: Button
    private lateinit var btnOpenLengthCmPopup: Button
    private lateinit var simpleCmListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = ""
    private var totalLengthTenths: Int = 0
    private val requestLengthEntry = 1004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_metric)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Cm = findViewById(R.id.btnSetUp3Cm)
        btnOpenLengthCmPopup = findViewById(R.id.btnOpenLengthCmPopup)
        simpleCmListView = findViewById(R.id.simpleCmListView)

        updateListViewCm() // Load today's catches into ListView

        btnOpenLengthCmPopup.setOnClickListener {
            openLengthCmPopup()
        }

        btnSetUp3Cm.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        simpleCmListView.setOnItemLongClickListener { parent, view, position, id ->
            if (catchList.isEmpty()) {
                Toast.makeText(this, "No catches available", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener true
            }

            if (position >= catchList.size) {
                Log.e("DB_DEBUG", "⚠️ Invalid position: $position, Catch List Size: ${catchList.size}")
                return@setOnItemLongClickListener true
            }

            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
            true
        }


    }//`````````` END ON-CREATE `````````````



    private fun openLengthCmPopup() {
        val intent = Intent(this, PopupLengthEntryMetric::class.java)
        startActivityForResult(intent, requestLengthEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestLengthEntry && resultCode == Activity.RESULT_OK) {
            totalLengthTenths= data?.getIntExtra("lengthTotalCms", 0) ?: 0
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: selectedSpecies


            //  CALL `saveCatch()` IMMEDIATELY AFTER WEIGHT IS RECEIVED
            if (totalLengthTenths > 0) {
                selectedSpecies = normalizeSpeciesName(selectedSpecies)
                saveCatch()
                Log.d("DB_DEBUG", "✅ saveCatch is called")
            } else {
                Log.e("DB_DEBUG", "⚠️ Invalid weight! Catch not saved.")
            }
        }
    }

    // %%%%%%%%%%% SAVE CATCH  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    private fun saveCatch() {
        Log.d("DB_DEBUG", "🔍 We are in saveCatch().")
        val newCatch = CatchItem(
            id = 0,
            latitude = null,
            longitude = null,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalLengthA8th = null,
            totalLengthTenths = totalLengthTenths,
            totalWeightHundredthKg = null,
            catchType = "metric",
            markerType = selectedSpecies,
            clipColor = null
        )

        val success = dbHelper.insertCatch(newCatch)

        if (success) {
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }

        if (success) {
            totalLengthTenths= 0 // ✅ Move this after successful save
        }
        updateListViewCm()  // ✅ Now only updates the UI, no extra insert
    }


    //:::::::::::::::: UPDATE LIST VIEW in time_Date Order ::::::::::::::::::::::::::::::::

    private fun updateListViewCm() {
        Log.d("DB_DEBUG", "🔍 We are in updateListView().")

        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val todaysCatches = dbHelper.getCatchesForToday("metric", todaysDate)
            .sortedByDescending { it.dateTime }  // Sort by dateTime (newest first)


        // ✅ Make sure catchList is updated BEFORE updating the ListView
        catchList.clear()
        catchList.addAll(todaysCatches)

        val catchDisplayList = todaysCatches.map {   // are we mapping the viewList on the weight? it should be the ID # or dateTime... and totalLengthTenths
            val lengthEntryMetric = it.totalLengthTenths ?: 0
            val centimeters =  lengthEntryMetric  / 10
            val millimeters = lengthEntryMetric  % 10
            // Format the time from dateTime
            val timeFormatted = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val parsedDate = inputFormat.parse(it.dateTime ?: "")
                outputFormat.format(parsedDate ?: Date())
            } catch (e: Exception) {
                "N/A"
            }

            "${it.species} - $centimeters.$millimeters Cms @ $timeFormatted"
        }
        runOnUiThread {
            val adapter = CatchItemAdapter(this, catchList)
            simpleCmListView.adapter = adapter
        }


    }


    //*************** DELETE ENTRY from list View of Catches ********************

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ ->
                showEditDialog(catchItem) // Call the new edit function
            }
            .setNegativeButton("Delete") { _, _ ->
                val dbHelper = CatchDatabaseHelper(this)
                dbHelper.deleteCatch(catchItem.id)
                updateListViewCm()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_cms, null)
        val edtLengthCms = dialogView.findViewById<EditText>(R.id.edtLengthCms)
        val edtLengthDecimal = dialogView.findViewById<EditText>(R.id.edtLengthDecimal)
        val spinnerSpeciesLbs = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditCms)

        // --- 1. Load user-selected species list ---
        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesLbs.adapter = adapter

        // --- 2. Set current values ---
        val totalLengthTenths = catchItem.totalLengthTenths ?: 0
        edtLengthCms.setText((totalLengthTenths / 10).toString())
        edtLengthDecimal.setText((totalLengthTenths % 10).toString())

        // --- 3. Set spinner selection based on normalized match ---
        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpeciesLbs.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        // --- 4. Show dialog and handle Save ---
        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newCm = edtLengthCms.text.toString().toIntOrNull() ?: 0
                val newDecimal = edtLengthDecimal.text.toString().toIntOrNull() ?: 0
                val totalLengthTenths = ((newCm * 10) + newDecimal)
                val species = spinnerSpeciesLbs.selectedItem.toString()

                val dbHelper = CatchDatabaseHelper(this)

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = null,
                    newLengthA8ths = null,
                    newLengthCm = totalLengthTenths,
                    species = species
                )

                Log.d("DB_DEBUG", "✅ Updating ID=${catchItem.id}, New Length=$totalLengthTenths, New Species=$species")

                updateListViewCm()
                Toast.makeText(this, "Catch updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // ############## GET DATE and TIME  ############################

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // ??????????????????? TODAYS DATE  ?????????????????????????????????
    private fun getTodaysDate(): String {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return todaysDate.format(Date())
    }

}//+++++++++++++ END  od CATCH ENTRY LBS OZS ++++++++++++++++++++++++++++++++++++++++
