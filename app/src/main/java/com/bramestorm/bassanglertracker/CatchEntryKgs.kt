package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryKgs : AppCompatActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnSaveCatch: Button
    private lateinit var btnOpenWeightPopup: Button
    private lateinit var simpleListView: ListView
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = "Large Mouth"
    private var totalWeightHundredthKg: Int = 0

    private val requestWeightEntry = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_kgs)

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnSaveCatch = findViewById(R.id.btnSaveCatch)
        btnOpenWeightPopup = findViewById(R.id.btnOpenWeightPopup)
        simpleListView = findViewById(R.id.simpleListView)

        updateListView()

        btnOpenWeightPopup.setOnClickListener {
            openWeightPopup()
        }

        btnSaveCatch.setOnClickListener {
            if (totalWeightHundredthKg > 0) {
                saveCatch()
            } else {
                Toast.makeText(this, "Enter a valid weight!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSetUp3.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }
    }

    private fun openWeightPopup() {
        val intent = Intent(this, PopupWeightEntryKgs::class.java)
        startActivityForResult(intent, requestWeightEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestWeightEntry && resultCode == Activity.RESULT_OK) {
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: "Unknown"
            totalWeightHundredthKg = data?.getIntExtra("weightTotalHundredthKg", 0) ?: 0

            val weightKg = totalWeightHundredthKg / 100.0
            Toast.makeText(this, "$selectedSpecies - $weightKg kg", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCatch() {
        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalLengthA8th = null,
            lengthDecimalTenthCm = null,
            totalWeightHundredthKg = totalWeightHundredthKg,
            catchType = "kgs",
            markerType = selectedSpecies,
            clipColor = null
        )

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()

        totalWeightHundredthKg = 0
        updateListView()
    }

    private fun updateListView() {
        val allCatches = dbHelper.getAllCatches().filter { it.catchType == "kgs" }

        val catchDisplayList = allCatches.map {
            val weightKg = it.totalWeightHundredthKg?.div(100.0) ?: 0.0
            "${it.species} - $weightKg kg"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchDisplayList)
        simpleListView.adapter = adapter
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
