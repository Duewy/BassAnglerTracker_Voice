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
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.getSpeciesImageResId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllSpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllSpeciesAdapter
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnAddSpecies: Button



    private val selectedSpecies = mutableSetOf<String>()
    private val allSpecies = mutableListOf<String>()
    private val speciesItems = mutableListOf<SpeciesItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_species_selection)

        recyclerView = findViewById(R.id.recyclerUserSpeciesAddition)
        btnSave = findViewById(R.id.btnSaveSpeciesList)
        btnCancel = findViewById(R.id.btnCancel)
        btnAddSpecies = findViewById(R.id.btnAddSpeciesToList)


        recyclerView.layoutManager = LinearLayoutManager(this)
        // --------- Keep Buttons off until list loads.
        btnSave.isEnabled = false
        btnCancel.isEnabled = false
        btnAddSpecies.isEnabled = false

        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val masterList = SharedPreferencesManager.getMasterSpeciesList(this@AllSpeciesSelectionActivity)
            val selectedList = SharedPreferencesManager.getSelectedSpeciesList(this@AllSpeciesSelectionActivity)
                .map { SharedPreferencesManager.normalizeSpeciesName(it) }

            allSpecies.clear()
            allSpecies.addAll(masterList) // 💥 Actually fill the master list
            selectedSpecies.clear()
            selectedSpecies.addAll(selectedList) // 💥 Actually fill selected species

            withContext(Dispatchers.Main) {
                setupAdapter()
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                btnAddSpecies.isEnabled = true

            }
        }


        btnSave.setOnClickListener {
            val finalList = adapter.getSelectedSpecies()
            SharedPreferencesManager.saveSelectedSpeciesList(this, finalList)
            finish()
        }

       //---------------  Cancel ----- GOTO SetUp Page ----------------
        btnCancel.setOnClickListener {
            finish()
        }

        //-------------- ADD SPECIES TO MASTER LIST ----------------------

        btnAddSpecies.setOnClickListener {
            val inputField = android.widget.EditText(this)
            inputField.hint = "Enter new species name"
            inputField.maxLines = 1

            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Add Custom Species")
                .setView(inputField)
                .setPositiveButton("Add") { dialogInterface, _ ->
                    val input = inputField.text.toString().trim()
                    val normalized = SharedPreferencesManager.normalizeSpeciesName(input)

                    if (input.isBlank()) {
                        Toast.makeText(this, "⚠️ Please enter a species name.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    if (allSpecies.any { SharedPreferencesManager.normalizeSpeciesName(it) == normalized }) {
                        Toast.makeText(this, "⚠️ Species already exists!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // ✅ Add to list and save
                    allSpecies.add(input)
                    SharedPreferencesManager.saveAllSpecies(this, allSpecies)

                    // ✅ Refresh full species list and adapter
                    setupAdapter()

                    Toast.makeText(this, "✅ '$input' added to species list.", Toast.LENGTH_SHORT).show()
                    dialogInterface.dismiss()
                }
                .setNegativeButton("Cancel") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()

            dialog.show()
        }






    }//-------------- END OnCreate ------------------------------------

    private fun setupAdapter() {
        val speciesItemList = allSpecies.map { name ->
            val normalizedName = SharedPreferencesManager.normalizeSpeciesName(name)
            val isChecked = selectedSpecies.contains(SharedPreferencesManager.normalizeSpeciesName(name))
            val imageResId = getSpeciesImageResId(name) // ✅ use raw name for image lookup
            SpeciesItem(name = name, imageResId = imageResId, isSelected = isChecked)
        }

        adapter = AllSpeciesAdapter(
            context = this,
            speciesList = speciesItemList.toMutableList(),
            initiallySelectedSpecies = selectedSpecies)
        { speciesName, isChecked ->
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
