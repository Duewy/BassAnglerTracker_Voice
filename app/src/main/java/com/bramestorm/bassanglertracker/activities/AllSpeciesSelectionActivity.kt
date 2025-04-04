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

    private val allSpecies = mutableListOf<String>()
    private val selectedSpecies = mutableSetOf<String>() // Max 8 allowed

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
            val masterSpeciesRaw = SharedPreferencesManager.getMasterSpeciesList(this@AllSpeciesSelectionActivity)
            val selectedListRaw = SharedPreferencesManager.getSelectedSpeciesList(this@AllSpeciesSelectionActivity)

            val masterSpeciesList = masterSpeciesRaw.map { SharedPreferencesManager.normalizeSpeciesName(it) }.distinct()
            val selectedList = selectedListRaw.map { SharedPreferencesManager.normalizeSpeciesName(it) }

            Log.d("AllSpeciesSelection", "Loaded master species list: $masterSpeciesList")
            Log.d("AllSpeciesSelection", "Loaded selected species list: $selectedList")

            if (masterSpeciesList.isEmpty()) {
                Log.w("AllSpeciesSelection", "No master species found! Initialization may have failed.")
            }

            withContext(Dispatchers.Main) {
                allSpecies.clear()
                allSpecies.addAll(masterSpeciesList)
                selectedSpecies.clear()
                selectedSpecies.addAll(selectedList)

                setupAdapter()

                btnSave.isEnabled = true
                btnCancel.isEnabled = true
            }
        }

        btnSave.setOnClickListener {
            val finalList = adapter.getSelectedSpecies().map { SharedPreferencesManager.normalizeSpeciesName(it) }
            SharedPreferencesManager.saveSelectedSpeciesList(this, finalList)
            Log.d("FinalSaveList", finalList.toString())
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupAdapter() {
        val speciesItems = allSpecies.map { speciesName ->
            SpeciesItem(
                name = speciesName,
                isSelected = selectedSpecies.contains(SharedPreferencesManager.normalizeSpeciesName(speciesName))
            )
        }

        adapter = AllSpeciesAdapter(speciesItems.toMutableList()) { speciesName, isChecked ->
            val normalizedSpecies = SharedPreferencesManager.normalizeSpeciesName(speciesName)
            if (isChecked) {
                if (selectedSpecies.size >= 8) {
                    Toast.makeText(this, "Only 8 species allowed!", Toast.LENGTH_SHORT).show()
                    adapter.uncheckSpecies(speciesName)
                } else {
                    selectedSpecies.add(normalizedSpecies)
                }
            } else {
                selectedSpecies.remove(normalizedSpecies)
            }
        }

        recyclerView.adapter = adapter
    }

}
