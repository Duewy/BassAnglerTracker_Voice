package com.bramestorm.bassanglertracker.adapters

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.AllSpeciesAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.getSpeciesImageResId


class AllSpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtSelectedCount: TextView
    private lateinit var btnSaveSpeciesList: Button
    private lateinit var btnCancel: Button

    private lateinit var allSpeciesList: MutableList<SpeciesItem>


    //''''''''''''' On Create ''''''''''''''''''''''''''''''''
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_species_selection)

        // Bind views
        recyclerView = findViewById(R.id.recyclerUserSpeciesAddition)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        btnSaveSpeciesList = findViewById(R.id.btnSaveSpeciesList)
        btnCancel = findViewById(R.id.btnCancel)

        // Load species (default + custom)
// Build the full list manually from all species names
        val allSpeciesNames = listOf(
            "Largemouth", "Smallmouth", "Crappie", "Walleye",
            "Catfish", "Perch", "Pike", "Bluegill", "Carp",
            "Musky", "Trout", "Salmon", "Other"
        )

        val selectedSpecies = SharedPreferencesManager.getSelectedSpecies(this).toSet()

        allSpeciesList = allSpeciesNames.map { name ->
            SpeciesItem(
                name = name,
                imageResId = getSpeciesImageResId(name),
                isSelected = selectedSpecies.contains(name)
            )
        }.toMutableList()


        // Initialize adapter
        val adapter = AllSpeciesAdapter(this, allSpeciesList) { selectedList: List<SpeciesItem> ->
            txtSelectedCount.text = "Selected: ${selectedList.size} /8 "
            btnSaveSpeciesList.isEnabled = selectedList.isNotEmpty()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Save selection
        btnSaveSpeciesList.setOnClickListener {
            val selectedSpecies = allSpeciesList.filter { it.isSelected }
            val selectedNames = allSpeciesList.filter { it.isSelected }.map { it.name }
            SharedPreferencesManager.saveOrderedSpeciesList(this, selectedNames)
            finish()
        }

        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }
    }// '''''''''''''''''' END On Create '''''''''''''''''''''''''''''''''
}
