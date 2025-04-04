package com.bramestorm.bassanglertracker.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.AllSpeciesAdapter
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllSpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllSpeciesAdapter
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private val selectedSpecies = mutableSetOf<String>()
    private val speciesItems = mutableListOf<SpeciesItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_species_selection)

        recyclerView = findViewById(R.id.recyclerUserSpeciesAddition)
        btnSave = findViewById(R.id.btnSaveSpeciesList)
        btnCancel = findViewById(R.id.btnCancel)

        recyclerView.layoutManager = LinearLayoutManager(this)
        btnSave.isEnabled = false
        btnCancel.isEnabled = false

        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val masterList = SharedPreferencesManager.getMasterSpeciesList(this@AllSpeciesSelectionActivity)
                .map { SharedPreferencesManager.normalizeSpeciesName(it) }
            val selectedList = SharedPreferencesManager.getSelectedSpeciesList(this@AllSpeciesSelectionActivity)
                .map { SharedPreferencesManager.normalizeSpeciesName(it) }

            Log.d("AllSpeciesSelection", "Loaded master: $masterList")
            Log.d("AllSpeciesSelection", "Loaded selected: $selectedList")

            selectedSpecies.clear()
            selectedSpecies.addAll(selectedList)

            speciesItems.clear()
            speciesItems.addAll(masterList.map { name ->
                SpeciesItem(name, selectedSpecies.contains(name))
            })

            withContext(Dispatchers.Main) {
                setupAdapter()
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
            }
        }

        btnSave.setOnClickListener {
            val finalList = adapter.getSelectedSpecies()
            SharedPreferencesManager.saveSelectedSpeciesList(this, finalList)
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupAdapter() {
        adapter = AllSpeciesAdapter(speciesItems) { speciesName, isChecked ->
            val normalized = SharedPreferencesManager.normalizeSpeciesName(speciesName)
            if (isChecked) {
                if (selectedSpecies.size >= 8) {
                    Toast.makeText(this, "Only 8 species allowed!", Toast.LENGTH_SHORT).show()
                    adapter.uncheckSpecies(speciesName)
                } else {
                    selectedSpecies.add(normalized)
                }
            } else {
                selectedSpecies.remove(normalized)
            }
        }

        recyclerView.adapter = adapter
    }
}
