package com.bramestorm.bassanglertracker.activities

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.AllSpeciesAdapter
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.getSpeciesImageResId
import com.bramestorm.bassanglertracker.utils.positionedToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeciesCatalogueActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllSpeciesAdapter
    private lateinit var btnCancel: Button
    private lateinit var btnAddSpecies: Button

    private val speciesCatalogue = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_catalog_selection)

        recyclerView = findViewById(R.id.recyclerUserSpeciesAddition)
        btnCancel = findViewById(R.id.btnCancel)
        btnAddSpecies = findViewById(R.id.btnAddSpeciesToList)


        recyclerView.layoutManager = LinearLayoutManager(this)

        // --------- Keep Buttons off until list loads.
        btnCancel.isEnabled = false
        btnAddSpecies.isEnabled = false


        lifecycleScope.launch(Dispatchers.IO) {
            val list = SharedPreferencesManager.getSpeciesCatalogue(this@SpeciesCatalogueActivity)

            speciesCatalogue.clear()
            speciesCatalogue.addAll(list)

            withContext(Dispatchers.Main) {
                setupAdapter()
                btnCancel.isEnabled = true
                btnAddSpecies.isEnabled = true
            }
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
                        positionedToast("⚠️ Please enter a species name.")
                        return@setPositiveButton
                    }

                    if (speciesCatalogue.any {
                            SharedPreferencesManager.normalizeSpeciesName(it) ==
                                    SharedPreferencesManager.normalizeSpeciesName(input)
                        }) {
                        positionedToast("⚠️ Species already exists!")
                        return@setPositiveButton
                    }

                    SharedPreferencesManager.saveSpeciesCatalogue(
                        this,
                        speciesCatalogue
                    )


                    // ✅ Refresh full species list and adapter
                    setupAdapter()
                    positionedToast("New species has been added to species list.")
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

        val speciesItemList = speciesCatalogue.map { name ->
            SpeciesItem(
                name = name,
                imageResId = getSpeciesImageResId(name)
            )
        }

        adapter = AllSpeciesAdapter(
            context = this,
            speciesList = speciesItemList.toMutableList()
        ) { updatedList ->
            speciesCatalogue.clear()
            speciesCatalogue.addAll(updatedList)
        }

        recyclerView.adapter = adapter
    }


}
