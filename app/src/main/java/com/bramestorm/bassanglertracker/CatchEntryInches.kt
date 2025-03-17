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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryInches : AppCompatActivity() {

    private lateinit var btnSetUp3Inch: Button
    private lateinit var btnOpenWeightPopupInch: Button
    private lateinit var simpleInchListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = ""
    private var totalLengthA8th: Int = 0
    private val requestLengthEntry = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_inches)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Inch = findViewById(R.id.btnSetUp3Inch)
        btnOpenWeightPopupInch = findViewById(R.id.btnOpenWeightPopupInch)
        simpleInchListView = findViewById(R.id.simpleInchListView)

        updateListViewInch() // Load today's catches into ListView

        btnOpenWeightPopupInch.setOnClickListener {
            openWeightPopupInch()
        }

        btnSetUp3Inch.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        simpleInchListView.setOnItemLongClickListener { parent, view, position, id ->
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



    private fun openWeightPopupInch() {
        val intent = Intent(this, PopupLengthEntryInches::class.java)
        startActivityForResult(intent, requestLengthEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestLengthEntry && resultCode == Activity.RESULT_OK) {
            totalLengthA8th = data?.getIntExtra("lengthTotalInches", 0) ?: 0
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: selectedSpecies


            //  CALL `saveCatch()` IMMEDIATELY AFTER WEIGHT IS RECEIVED
            if (totalLengthA8th > 0) {
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
            totalLengthA8th = totalLengthA8th,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            catchType = "inches",
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
            totalLengthA8th = 0 // ✅ Move this after successful save
        }
        updateListViewInch()  // ✅ Now only updates the UI, no extra insert
    }


    //:::::::::::::::: UPDATE LIST VIEW in time_Date Order ::::::::::::::::::::::::::::::::

    private fun updateListViewInch() {
        Log.d("DB_DEBUG", "🔍 We are in updateListViewLb.")
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("DB_DEBUG", "🔍 This is todaysDate in CatchEntry: $todaysDate")
        val todaysCatches = dbHelper.getCatchesForToday("inches", todaysDate)
            .sortedByDescending { it.dateTime }  // Sort by dateTime (newest first)

        Log.d("DB_DEBUG", "🔍 Catches retrieved from DB: ${todaysCatches.size}")

        // ✅ Make sure catchList is updated BEFORE updating the ListView
        catchList.clear()
        catchList.addAll(todaysCatches)

        val catchDisplayList = todaysCatches.map {   // are we mapping the viewList on the weight? it should be the ID # or dateTime... and totalWeightOz
            val totalLengthA8th = it.totalLengthA8th ?: 0
            val inches = totalLengthA8th / 8
            val a8ths = totalLengthA8th  % 8
            "${it.species} - $inches inches $a8ths 8ths"
        }
        runOnUiThread {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchDisplayList)
            simpleInchListView.adapter = adapter
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
                updateListViewInch()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_inches, null)
        val edtLengthInches = dialogView.findViewById<EditText>(R.id.edtLengthInches)
        val edtLengthEntry8ths = dialogView.findViewById<EditText>(R.id.edtLength8ths)
        val spinnerSpeciesEditInches = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditInches)

        // Load species list from strings.xml
        val speciesArray = resources.getStringArray(R.array.species_list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesEditInches.adapter = adapter

        // Set current values
        val newLengthA8ths= catchItem.totalLengthA8th ?: 0 // Default to 0 if null
        edtLengthInches.setText((newLengthA8ths / 8).toString())
        edtLengthEntry8ths.setText((newLengthA8ths % 8).toString())

        // Set spinner selection to the current species
        val speciesIndex = speciesArray.indexOf(catchItem.species)
        spinnerSpeciesEditInches.setSelection(if (speciesIndex != -1) speciesIndex else 0) // ✅ Default to first species

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
                val new8ths = edtLengthEntry8ths.text.toString().toIntOrNull() ?: 0
                val totalLengthA8th= (newInches * 16) + new8ths
                val species = spinnerSpeciesEditInches.selectedItem.toString()

                val dbHelper = CatchDatabaseHelper(this)

                // ✅ Call updateCatch() with all required parameters
                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = null,  // Since this is Lbs/Oz mode, set Kg to null
                    newLengthA8ths = totalLengthA8th,  // No length update
                    newLengthCm = null,  // No length update
                    species = species
                )

                Log.d("DB_DEBUG", "✅ Updating ID=${catchItem.id}, New Weight=$totalLengthA8th, New Species=$species")

                updateListViewInch()
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
