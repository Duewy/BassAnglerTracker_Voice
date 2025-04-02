package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.activities.AllSpeciesSelectionActivity
import com.bramestorm.bassanglertracker.adapters.SpeciesSelectAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class SpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeciesSelectAdapter
    private lateinit var btnSave: Button
    private lateinit var btnAddSpecies: Button
    private lateinit var txtTitle: TextView
    private lateinit var txtSelectedCount: TextView

    private var allSpecies = mutableListOf<String>()
    private var selectedSpecies = mutableSetOf<String>() // Max 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSave = findViewById(R.id.btnSaveSpecies)
        btnAddSpecies = findViewById(R.id.btnAdjustSpeciesList)
        txtTitle = findViewById(R.id.txtSpeciesTitle)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)

        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)


        // Load all saved species and current selection
        allSpecies = SharedPreferencesManager.getAllSavedSpecies(this).toMutableList()
        selectedSpecies = SharedPreferencesManager.getSelectedSpeciesList(this).toMutableSet()

        // Adapter Setup
        adapter = SpeciesSelectAdapter(allSpecies, selectedSpecies) { speciesName, isChecked ->
            if (isChecked) {
                if (selectedSpecies.size >= 8) {
                    Toast.makeText(this, "Only 8 species allowed", Toast.LENGTH_SHORT).show()
                    adapter.uncheckSpecies(speciesName)
                } else {
                    selectedSpecies.add(speciesName)
                }
            } else {
                selectedSpecies.remove(speciesName)
            }
            updateSelectedCount()
            btnSave.isEnabled = selectedSpecies.isNotEmpty()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Save the selected species list
        btnSave.setOnClickListener {
            SharedPreferencesManager.saveSelectedSpeciesList(this, selectedSpecies.toList())
            Toast.makeText(this, "Species list saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Opens the adjust list page
        btnAddSpecies.setOnClickListener {
            val intent = Intent(this, AllSpeciesSelectionActivity::class.java)
            startActivity(intent)
        }

        updateSelectedCount()
        btnSave.isEnabled = selectedSpecies.isNotEmpty()
    }

    private fun updateSelectedCount() {
        txtSelectedCount.text = "Selected: ${selectedSpecies.size}/8"
    }


    private fun showAddSpeciesDialog() {
        val input = EditText(this)
        input.hint = "Enter species name"

        AlertDialog.Builder(this)
            .setTitle("Add Custom Species")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val newSpecies = input.text.toString().trim()
                if (newSpecies.isNotEmpty() && !allSpecies.contains(newSpecies)) {
                    allSpecies.add(newSpecies)
                    SharedPreferencesManager.saveAllSpecies(this, allSpecies)
                    adapter.notifyItemInserted(allSpecies.size - 1)
                } else {
                    Toast.makeText(this, "Species already exists or is empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
