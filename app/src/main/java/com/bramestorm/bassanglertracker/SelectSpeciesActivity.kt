package com.bramestorm.bassanglertracker

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager


class SpeciesSelectionActivity : AppCompatActivity() {

    private val allSpecies = mutableListOf(
        "Largemouth", "Smallmouth", "Crappie", "Walleye", "Catfish",
        "Perch", "Pike", "Bluegill", "Trout", "Salmon", "Carp", "Muskie",
        "White Bass", "Rock Bass", "Bowfin", "Burbot", "Gar", "Sucker",
        "Drum", "Goldeye", "Mooneye", "Shiner", "Chub", "Dace"
    )


    private lateinit var layoutSpeciesList: LinearLayout
    private lateinit var txtSelectedCount: TextView
    private lateinit var btnSaveSpecies: Button
    private lateinit var btnAddSpecies :Button
    private var selectedSpecies = mutableListOf<String>()
    private val maxSelection = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        layoutSpeciesList = findViewById(R.id.layoutSpeciesList)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        btnSaveSpecies= findViewById(R.id.btnSaveSpecies)
        btnAddSpecies = findViewById(R.id.btnAddSpecies)

        selectedSpecies = SharedPreferencesManager.getSelectedSpecies(this).toMutableList()
        updateSelectedCount()

        for (species in allSpecies) {
            val checkbox = CheckBox(this)
            checkbox.text = species
            checkbox.isChecked = selectedSpecies.contains(species)

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (selectedSpecies.size < maxSelection) {
                        selectedSpecies.add(species)
                    } else {
                        checkbox.isChecked = false
                        Toast.makeText(this, "You can only select up to $maxSelection species", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    selectedSpecies.remove(species)
                }
                updateSelectedCount()
            }

            layoutSpeciesList.addView(checkbox)
        }

        btnSaveSpecies.setOnClickListener {
            SharedPreferencesManager.saveSelectedSpecies(this, selectedSpecies)
            Toast.makeText(this, "Species selection saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnAddSpecies.setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Add Custom Species")
                .setMessage("Enter the species name:")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val newSpecies = input.text.toString().trim()
                    if (newSpecies.isNotEmpty() && !allSpecies.contains(newSpecies)) {
                        allSpecies.add(newSpecies)
                        addCheckboxForSpecies(newSpecies)
                    } else {
                        Toast.makeText(this, "Invalid or duplicate species", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

    }//++++++++++++ END On Create +++++++++++++++++++++++++++++++

    private fun addCheckboxForSpecies(species: String) {
        val checkbox = CheckBox(this)
        checkbox.text = species
        checkbox.isChecked = selectedSpecies.contains(species)

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (selectedSpecies.size < maxSelection) {
                    selectedSpecies.add(species)
                } else {
                    checkbox.isChecked = false
                    Toast.makeText(this, "You can only select up to $maxSelection species", Toast.LENGTH_SHORT).show()
                }
            } else {
                selectedSpecies.remove(species)
            }
            updateSelectedCount()
        }

        layoutSpeciesList.addView(checkbox)
    }


    private fun updateSelectedCount() {
        txtSelectedCount.text = getString(R.string.selected_species_count, selectedSpecies.size, maxSelection)
        btnSaveSpecies.isEnabled = selectedSpecies.isNotEmpty()
    }
}
