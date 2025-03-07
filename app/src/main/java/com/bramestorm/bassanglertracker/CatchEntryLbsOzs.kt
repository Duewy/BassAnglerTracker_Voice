package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.room.util.TableInfo
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper.Companion

class CatchEntryLbsOzs : AppCompatActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnSaveCatch: Button
    private lateinit var btnOpenWeightPopup: Button
    private lateinit var simpleListView: ListView
    private lateinit var listView: ListView
    private lateinit var catchAdapter: CatchItemAdapter
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper
    private var lastCatchCount = -1  // Prevent excessive UI updates

    private var selectedSpecies: String = ""
    private var totalWeightOz: Int = 0

    private val requestWeightEntry = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_lbs_ozs)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnSaveCatch = findViewById(R.id.btnSaveCatch)
        btnOpenWeightPopup = findViewById(R.id.btnOpenWeightPopup)
        simpleListView = findViewById(R.id.simpleListView)
        listView = findViewById(R.id.simpleListView)

        updateListView() // Load today's catches into ListView
        Log.d("DB_DEBUG", "✅ onCreate - Catch List Size: ${catchList.size}")

        btnOpenWeightPopup.setOnClickListener {
            openWeightPopup()
        }

        btnSetUp3.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        listView.setOnItemLongClickListener { parent, view, position, id ->
            Log.d("DB_DEBUG", "📌 Long-click detected on position: $position")
            Log.d("DB_DEBUG", "📌 Catch List Size at Click: ${catchList.size}")

            if (catchList.isEmpty()) {
                Toast.makeText(this, "No catches available", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener true
            }

            if (position >= catchList.size) {
                Log.e("DB_DEBUG", "⚠️ Invalid position: $position, Catch List Size: ${catchList.size}")
                return@setOnItemLongClickListener true
            }

            val selectedCatch = catchList[position] // ✅ This is now safe
            showEditDeleteDialog(selectedCatch)
            true
        }


    }//`````````` END ON-CREATE `````````````

    private fun openWeightPopup() {
        val intent = Intent(this, PopupWeightEntryLbs::class.java)
        startActivityForResult(intent, requestWeightEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("DB_DEBUG", "✅ onActivityResult triggered requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == requestWeightEntry && resultCode == Activity.RESULT_OK) {
            totalWeightOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: selectedSpecies

            Log.d("DB_DEBUG", "✅ Weight and species received: $selectedSpecies - $totalWeightOz oz")

            // 🚨 CALL `saveCatch()` IMMEDIATELY AFTER WEIGHT IS RECEIVED
            if (totalWeightOz > 0) {
                saveCatch()
                Log.d("DB_DEBUG", "✅ saveCatch is called")
            } else {
                Log.e("DB_DEBUG", "⚠️ Invalid weight! Catch not saved.")
            }
        }
    }


      private fun saveCatch() {
        Log.d("DB_DEBUG", "🔍 We are in saveCatch().")
        val newCatch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = totalWeightOz,
            totalLengthA8th = null,
            lengthDecimalTenthCm = null,
            totalWeightHundredthKg = null,
            catchType = "lbsOzs",
            markerType = selectedSpecies,
            clipColor = null
        )

        Log.d("DB_DEBUG", "🐟 Attempting to insert catch: $newCatch") // ✅ Log before insertion

        val success = dbHelper.insertCatch(newCatch)

        if (success) {
            Log.d("DB_DEBUG", "✅ Catch successfully inserted: $newCatch") // ✅ Log after successful insertion
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("DB_DEBUG", "⚠️ Failed to insert catch!")
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }

          if (success) {
              totalWeightOz = 0 // ✅ Move this after successful save
              btnSaveCatch.isEnabled = false
              updateListView()
          }


          updateListView()  // ✅ Now only updates the UI, no extra insert
     }



    private fun updateListView() {
        Log.d("DB_DEBUG", "🔍 We are in updateListView.")
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayCatches = dbHelper.getCatchesForToday("lbsOzs", todayDate)
            .sortedByDescending { it.dateTime }  // Sort by dateTime (newest first)


        Log.d("DB_DEBUG", "🔍 Catches retrieved from DB: ${todayCatches.size}")

        // ✅ Make sure catchList is updated BEFORE updating the ListView
        catchList.clear()
        catchList.addAll(todayCatches)

        Log.d("DB_DEBUG", "✅ Updated catchList size: ${catchList.size}")

        val catchDisplayList = todayCatches.map {   // are we mapping the viewList on the weight? it should be the ID # or dateTime... and totalWeightOz
            val weightOz = it.totalWeightOz ?: 0
            val pounds = weightOz / 16
            val ounces = weightOz % 16
            "${it.species} - $pounds lbs $ounces oz"
        }

        runOnUiThread {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchDisplayList)
            simpleListView.adapter = adapter
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
                updateListView()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch, null)
        val edtWeightLbs = dialogView.findViewById<EditText>(R.id.edtWeightLbs)
        val edtWeightOzs = dialogView.findViewById<EditText>(R.id.edtWeightOzs)
        val spinnerSpecies = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEdit)

        // Load species list from strings.xml
        val speciesArray = resources.getStringArray(R.array.species_list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter

        // Set current values
        val totalWeightOz = catchItem.totalWeightOz ?: 0 // Default to 0 if null
        edtWeightLbs.setText((totalWeightOz / 16).toString())
        edtWeightOzs.setText((totalWeightOz % 16).toString())

        // Set spinner selection to the current species
        val speciesIndex = speciesArray.indexOf(catchItem.species)
        spinnerSpecies.setSelection(if (speciesIndex != -1) speciesIndex else 0) // ✅ Default to first species

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
                val newOzs = edtWeightOzs.text.toString().toIntOrNull() ?: 0
                val totalWeightOz = (newLbs * 16) + newOzs
                val species = spinnerSpecies.selectedItem.toString()


                val dbHelper = CatchDatabaseHelper(this)
                dbHelper.updateCatch(catchItem.id, totalWeightOz, species)
                Log.d("DB_DEBUG", "Updating ID=$catchItem.id, New Weight=$totalWeightOz, New Species=$species")

                updateListView()
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
}
