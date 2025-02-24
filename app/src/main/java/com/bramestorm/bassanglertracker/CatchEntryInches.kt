package com.bramestorm.bassanglertracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelperInches
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class CatchEntryInches : AppCompatActivity() {

    private lateinit var speciesSpinner: Spinner
    private lateinit var inchesSpinner: Spinner
    private lateinit var a8thSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var listView: ListView
    private lateinit var catchAdapter: ArrayAdapter<String>
    private lateinit var dbHelper: CatchDatabaseHelperInches

    private val catchList = mutableListOf<CatchItemInches>()  // Uses CatchItemInches for Inches entries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_inches)

        speciesSpinner = findViewById(R.id.speciesSpinner)
        inchesSpinner = findViewById(R.id.inchesSpinner)
        a8thSpinner = findViewById(R.id.a8thSpinner)
        saveButton = findViewById(R.id.saveCatchButton)
        listView = findViewById(R.id.simpleListView)

        dbHelper = CatchDatabaseHelperInches(this)

        // Populate Species Spinner
        val speciesList = arrayOf("Large Mouth", "Small Mouth", "Crappie", "Pike", "Perch", "Walleye", "Catfish", "Panfish")
        val speciesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        // Populate KG Spinner (0-50 kg)
        val inchesList = (0..50).toList()
        val inchesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, inchesList)
        inchesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inchesSpinner.adapter = inchesAdapter

        // Populate Decimal Spinner (0 7)
        val eigthList = (0..7).toList()// Creates 0 to 7 8ths
        val eigthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eigthList)
        eigthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        a8thSpinner.adapter = eigthAdapter

        catchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, catchList.map { it.toString() })
        listView.adapter = catchAdapter

        loadCatches()

        saveButton.setOnClickListener {
            val species = speciesSpinner.selectedItem.toString()
            val lengthInches = inchesSpinner.selectedItem.toString().toInt()
            val lengtha8th = a8thSpinner.selectedItem.toString().toInt()
            val lengthInDec = (lengthInches + (lengtha8th / 8.0)).toFloat()
            val dateTime: String = getCurrentTimestamp()

            val newCatch = CatchItemInches(0, dateTime, species,lengtha8th,lengthInches , lengthInDec)


            dbHelper.insertCatch(dateTime, species, lengtha8th, lengthInches, lengthInDec) // ✅ Correct


            catchList.add(0, newCatch)
            catchAdapter.notifyDataSetChanged()

            Toast.makeText(this, "Catch saved!", Toast.LENGTH_SHORT).show()

            speciesSpinner.setSelection(0)
            inchesSpinner.setSelection(0)
            a8thSpinner.setSelection(0)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
        }

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUpInches)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadCatches() {
        catchList.clear()
        val fetchedCatches = mutableListOf<CatchItemInches>() // ✅ Matches List type


        if (fetchedCatches.isEmpty()) {
            Toast.makeText(this, "No catches found", Toast.LENGTH_SHORT).show()
        }

        catchList.addAll(fetchedCatches)
        catchAdapter.notifyDataSetChanged()
    }

    private fun showEditDeleteDialog(catchItem: CatchItemInches) {
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


