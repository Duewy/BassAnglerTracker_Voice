package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryLbsOzs : AppCompatActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnSaveCatch: Button
    private lateinit var btnOpenWeightPopup: Button
    private lateinit var simpleListView: ListView
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = "Large Mouth"
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

        updateListView() // Load today's catches into ListView

        btnOpenWeightPopup.setOnClickListener {
            openWeightPopup()
        }

        btnSaveCatch.setOnClickListener {
            if (totalWeightOz > 0) {
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
        val intent = Intent(this, PopupWeightEntryLbs::class.java)
        startActivityForResult(intent, requestWeightEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestWeightEntry && resultCode == Activity.RESULT_OK) {
            totalWeightOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: selectedSpecies

            // Refresh ListView after adding a catch
            loadCatchList()
            Toast.makeText(this, "$selectedSpecies - ${totalWeightOz / 16} lbs ${totalWeightOz % 16} oz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCatchList() {
        val databaseHelper = CatchDatabaseHelper(this)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Get today's date
        val todayCatches = databaseHelper.getCatchesForToday("lbsOzs", todayDate) // Fetch only today's catches
        val catchDisplayList = todayCatches.map {
            "${it.species} - ${it.totalWeightOz?.div(16)} lbs ${it.totalWeightOz?.rem(16)} oz"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchDisplayList)
        simpleListView.adapter = adapter
    }

    private fun saveCatch() {
        val catch = CatchItem(
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

        dbHelper.insertCatch(catch)
        Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()

        totalWeightOz = 0
        btnSaveCatch.isEnabled = false  // Prevent accidental resaves
        updateListView()
    }

    private fun updateListView() {
        loadCatchList() // Now only loads today's catches
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
