package com.bramestorm.bassanglertracker

import android.app.Activity
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

    private lateinit var btnSetUp3: Button
    private lateinit var btnSaveCatch: Button
    private lateinit var btnOpenCmLengthPopup: Button
    private lateinit var simpleListView: ListView
    private lateinit var dbHelper: CatchDatabaseHelper
    private var lastCatchCount = -1
    private var selectedSpecies: String = "Large Mouth"
    private var lengthDecimalTenthCm: Int = 0

    private val requestLengthEntry = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_metric)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnSaveCatch = findViewById(R.id.btnSaveCatch)
        btnOpenCmLengthPopup = findViewById(R.id.btnOpenCmLengthPopup)
        simpleListView = findViewById(R.id.simpleListView)

        updateListView() // Load existing catches into ListView

        btnOpenCmLengthPopup.setOnClickListener {
            openLengthPopup()
        }

        btnSaveCatch.setOnClickListener {
            if (lengthDecimalTenthCm > 0) {
                saveCatch()
            } else {
                Toast.makeText(this, "Enter a valid length!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSetUp3.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }
    }

    private fun openLengthPopup() {
        val intent = Intent(this, PopupLengthEntryMetric::class.java)
        startActivityForResult(intent, requestLengthEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestLengthEntry && resultCode == Activity.RESULT_OK) {
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: "Unknown"
            lengthDecimalTenthCm = data?.getIntExtra("lengthTotalTenthCm", 0) ?: 0

            Toast.makeText(this, "$selectedSpecies - ${lengthDecimalTenthCm / 10}.${lengthDecimalTenthCm % 10} cm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCatch() {
        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalLengthA8th = null,
            lengthDecimalTenthCm = lengthDecimalTenthCm,
            totalWeightHundredthKg = null,
            catchType = "metric",
            markerType = selectedSpecies,
            clipColor = null
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()

        lengthDecimalTenthCm = 0
        updateListView()
    }

    private fun updateListView() {
        Log.d("DB_DEBUG", "🔍 We are in updateListView.")
        val databaseHelper = CatchDatabaseHelper(this)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayCatches = databaseHelper.getCatchesForToday("lbsOzs", todayDate)

        if (todayCatches.size == lastCatchCount) {
            Log.d("DB_DEBUG", "ListView update skipped (no new catches).")
            return
        }

        lastCatchCount = todayCatches.size

        val catchDisplayList = todayCatches.map {
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


    private fun loadCatchList() {
        Log.d("DB_DEBUG", "Loading today's catch list...")  // Debug log

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Get today's date
        val todayCatches = dbHelper.getCatchesForToday("lbsOzs", todayDate) // ✅ Pass the required parameters

        if (todayCatches.isEmpty()) {
            Log.d("DB_DEBUG", "No catches found for today.")
        } else {
            Log.d("DB_DEBUG", "Today's catches: ${todayCatches.size}")
        }

        val catchDisplayList = todayCatches.map {
            val weightOz = it.totalWeightOz ?: 0  // Ensure it is not null
            val pounds = weightOz / 16
            val ounces = weightOz % 16

            "${it.species} - $pounds lbs $ounces oz"
        }

        runOnUiThread {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchDisplayList)
            simpleListView.adapter = adapter
        }
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
                loadCatchList()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
