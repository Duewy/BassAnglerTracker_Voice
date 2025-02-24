package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelperKgs
import java.text.SimpleDateFormat
import java.util.*

class CatchEntryKgs : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var kgSpinner: Spinner
    private lateinit var decimalSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var dbHelper: CatchDatabaseHelperKgs
    private lateinit var catchAdapter: ArrayAdapter<String>

    private val catchList = mutableListOf<CatchItemKgs>()  // Uses CatchItemKgs for KG entries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_kgs)

        speciesSpinner = findViewById(R.id.speciesSpinner)
        kgSpinner = findViewById(R.id.kgSpinner)
        decimalSpinner = findViewById(R.id.decimalSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)

        dbHelper = CatchDatabaseHelperKgs(this)

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp3)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }

        // Populate Species Spinner
        val speciesList = arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
        val speciesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        // Populate KG Spinner (0-50 kg)
        val kgList = (0..50).toList()
        val kgAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kgList)
        kgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kgSpinner.adapter = kgAdapter

        // Populate Decimal Spinner (0.00 to 0.99)
        val decimalList = (0..99).map { String.format(".%02d", it) } // Creates .00 to .99
        val decimalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, decimalList)
        decimalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        decimalSpinner.adapter = decimalAdapter

        // Setup ListView Adapter
        catchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchList.map { it.toString() })
        listView.adapter = catchAdapter

        loadCatches()

        saveButton.setOnClickListener {
            saveCatch()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showDeleteDialog(selectedCatch)
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val todayDate = getCurrentDateOnly()
        val fetchedCatches = dbHelper.getAllCatches().filter { it.dateTime.startsWith(todayDate) }

        catchList.addAll(fetchedCatches)
        catchAdapter.clear()
        catchAdapter.addAll(fetchedCatches.map { "${it.species} - ${it.weightKgs} kg - ${it.dateTime}" })
        catchAdapter.notifyDataSetChanged()
    }

    private fun saveCatch() {
        val species = speciesSpinner.selectedItem?.toString() ?: ""
        val weightKg = kgSpinner.selectedItem?.toString()?.toIntOrNull() ?: 0
        val weightDecimal = decimalSpinner.selectedItem?.toString()?.replace(".", "")?.toFloatOrNull()?.div(100) ?: 0f
        val weightTotal = weightKg + weightDecimal
        val dateTime = getCurrentTimestamp()

        if (species.isEmpty() || weightTotal == 0f) {
            Toast.makeText(this, "Please enter a valid species and weight!", Toast.LENGTH_SHORT).show()
            return
        }

        dbHelper.insertCatch(dateTime, species, weightTotal.toDouble())
        catchList.add(0, CatchItemKgs(0, dateTime, species, weightTotal.toDouble()))
        catchAdapter.notifyDataSetChanged()

        Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()
        speciesSpinner.setSelection(0)
        kgSpinner.setSelection(0)
        decimalSpinner.setSelection(0)
    }

    private fun showDeleteDialog(catchItem: CatchItemKgs) {
        AlertDialog.Builder(this)
            .setTitle("Delete Catch")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteCatch(catchItem.id)
                loadCatches()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentDateOnly(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
