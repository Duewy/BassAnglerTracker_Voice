package com.bramestorm.bassanglertracker.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.AllSpeciesAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllSpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllSpeciesAdapter
    private lateinit var btnSave: Button
    private lateinit var btnCancel:Button

    private val allSpecies = mutableListOf<String>()
    private val selectedSpecies = mutableSetOf<String>() // max 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_species_selection)

        recyclerView = findViewById(R.id.recyclerUserSpeciesAddition)
        btnSave = findViewById(R.id.btnSaveSpeciesList)
        btnCancel = findViewById(R.id.btnCancel)

        recyclerView.layoutManager = LinearLayoutManager(this)

        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        // Load SharedPreferences safely off the main thread
        lifecycleScope.launch(Dispatchers.IO) {
            val savedSpecies = SharedPreferencesManager.getAllSavedSpecies(this@AllSpeciesSelectionActivity)
            val selected = SharedPreferencesManager.getSelectedSpeciesList(this@AllSpeciesSelectionActivity)

            withContext(Dispatchers.Main) {
                allSpecies.addAll(savedSpecies)
                selectedSpecies.addAll(selected)

                adapter = AllSpeciesAdapter(allSpecies, selectedSpecies) { speciesName, isChecked ->
                    if (isChecked) {
                        if (selectedSpecies.size >= 8) {
                            Toast.makeText(this@AllSpeciesSelectionActivity, "Only 8 species allowed!", Toast.LENGTH_SHORT).show()
                            adapter.uncheckSpecies(speciesName)
                        } else {
                            selectedSpecies.add(speciesName)
                        }
                    } else {
                        selectedSpecies.remove(speciesName)
                    }
                }

                recyclerView.adapter = adapter
                btnSave.isEnabled = true
            }
        }

        //------------------------ SAVE Button --------------------------
        btnSave.setOnClickListener {
            SharedPreferencesManager.saveSelectedSpeciesList(this, selectedSpecies.toList())
            finish()
        }

        //------------------------- CANCEL Button ------------------------------
        btnCancel.setOnClickListener {
            SharedPreferencesManager.saveSelectedSpeciesList(this, selectedSpecies.toList())
            finish()
        }
    }
}
