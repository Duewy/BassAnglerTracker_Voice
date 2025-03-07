package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryInches : AppCompatActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnSaveCatch: Button
    private lateinit var btnOpenLengthPopup: Button
    private lateinit var simpleListView: ListView
    private lateinit var dbHelper: CatchDatabaseHelper
    private var lastCatchCount = -1
    private var selectedSpecies: String = "Large Mouth"
    private var totalLengthA8th: Int = 0

    private val requestLengthEntry = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_inches)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnSaveCatch = findViewById(R.id.btnSaveCatch)
        btnOpenLengthPopup = findViewById(R.id.btnOpenLengthPopup)
        simpleListView = findViewById(R.id.simpleListView)

        updateListView()

        btnOpenLengthPopup.setOnClickListener {
            openLengthPopup()
        }

        btnSaveCatch.setOnClickListener {
            if (totalLengthA8th > 0) {
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
        val intent = Intent(this, PopupLengthEntryInches::class.java)
        startActivityForResult(intent, requestLengthEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestLengthEntry && resultCode == Activity.RESULT_OK) {
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: "Unknown"
            totalLengthA8th = data?.getIntExtra("lengthTotalA8th", 0) ?: 0

            val inches = totalLengthA8th / 8
            val eighths = totalLengthA8th % 8
            Toast.makeText(this, "$selectedSpecies - $inches" + "-${eighths}/8 inches", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCatch() {
        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalLengthA8th = totalLengthA8th,
            lengthDecimalTenthCm = null,
            totalWeightHundredthKg = null,
            catchType = "inches",
            markerType = selectedSpecies,
            clipColor = null
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()

        totalLengthA8th = 0
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

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}