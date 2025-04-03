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
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryLbsOzs : AppCompatActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnOpenWeightPopup: Button
    private lateinit var simpleLbsListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = ""
    private var totalWeightOz: Int = 0
    private val requestWeightEntry = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_lbs_ozs)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnOpenWeightPopup = findViewById(R.id.btnOpenWeightPopup)
        simpleLbsListView = findViewById(R.id.simpleLbsListView)

        updateListViewLb() // Load today's catches into ListView

        btnOpenWeightPopup.setOnClickListener {
            openWeightPopup()
        }

        btnSetUp3.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        simpleLbsListView.setOnItemLongClickListener { _, _, position, _ ->
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



    private fun openWeightPopup() {
        val intent = Intent(this, PopupWeightEntryLbs::class.java)
        startActivityForResult(intent, requestWeightEntry)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n " +
            "     which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n " +
            "     contracts for common intents available in\n" +
            "      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n" +
            "      testing, and allow receiving results in separate, testable classes independent from your\n" +
            "      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n" +
            "      with the appropriate {@link ActivityResultContract} and handling the result in the\n " +
            "     {@link ActivityResultCallback#onActivityResult(Object) callback}.")

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestWeightEntry && resultCode == Activity.RESULT_OK) {
            totalWeightOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: selectedSpecies


            //  CALL `saveCatch()` IMMEDIATELY AFTER WEIGHT IS RECEIVED
            if (totalWeightOz > 0) {
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
            totalWeightOz = totalWeightOz,
            totalLengthA8th = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            catchType = "lbsOzs",
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
              totalWeightOz = 0 // ✅ Move this after successful save
              }
          updateListViewLb()  // ✅ Now only updates the UI, no extra insert
     }


 //:::::::::::::::: UPDATE LIST VIEW in time_Date Order ::::::::::::::::::::::::::::::::

    private fun updateListViewLb() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysCatches = dbHelper.getCatchesForToday("lbsOzs", todaysDate)
            .sortedByDescending { it.dateTime }

        Log.d("DB_DEBUG", "🔍 Catches retrieved from DB: ${todaysCatches.size}")

        catchList.clear()
        catchList.addAll(todaysCatches)

        runOnUiThread {
            val adapter = CatchItemAdapter(this, catchList)
            simpleLbsListView.adapter = adapter

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
                updateListViewLb()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_lbs, null)
        val edtWeightLbs = dialogView.findViewById<EditText>(R.id.edtWeightLbs)
        val edtWeightOzs = dialogView.findViewById<EditText>(R.id.edtWeightOzs)
        val spinnerSpeciesLbs = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditLbs)

        // Load species list from strings.xml
        val speciesArray = resources.getStringArray(R.array.species_list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesLbs.adapter = adapter

        // Set current values
        val totalWeightOz = catchItem.totalWeightOz ?: 0 // Default to 0 if null
        edtWeightLbs.setText((totalWeightOz / 16).toString())
        edtWeightOzs.setText((totalWeightOz % 16).toString())

        // Set spinner selection to the current species
        val speciesIndex = speciesArray.indexOf(catchItem.species)
        spinnerSpeciesLbs.setSelection(if (speciesIndex != -1) speciesIndex else 0) // ✅ Default to first species

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
                val newOzs = edtWeightOzs.text.toString().toIntOrNull() ?: 0
                val totalWeightOz = (newLbs * 16) + newOzs
                val species = spinnerSpeciesLbs.selectedItem.toString()

                val dbHelper = CatchDatabaseHelper(this)

                // ✅ Call updateCatch() with all required parameters
                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = totalWeightOz,
                    newWeightKg = null,  // Since this is Lbs/Oz mode, set Kg to null
                    newLengthA8ths = null,  // No length update
                    newLengthCm = null,  // No length update
                    species = species
                )

                Log.d("DB_DEBUG", "✅ Updating ID=${catchItem.id}, New Weight=$totalWeightOz, New Species=$species")

                updateListViewLb()
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


}//+++++++++++++ END of CATCH ENTRY LBS OZS ++++++++++++++++++++++++++++++++++++++++
