    package com.bramestorm.bassanglertracker.activities

    import android.os.Bundle
    import android.widget.Button
    import android.widget.EditText
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AlertDialog
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.bramestorm.bassanglertracker.R
    import com.bramestorm.bassanglertracker.adapters.AllSpeciesAdapter
    import com.bramestorm.bassanglertracker.models.SpeciesItem
    import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
    import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper

    class AllSpeciesSelectionActivity : AppCompatActivity() {

        private lateinit var recyclerView: RecyclerView
        private lateinit var txtSelectedCount: TextView
        private lateinit var btnSaveSpeciesList: Button
        private lateinit var btnCancel: Button
        private lateinit var btnAddSpecies: Button

        private lateinit var allSpeciesList: MutableList<SpeciesItem>

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_all_species_selection)

            recyclerView = findViewById(R.id.recyclerUserSpeciesAddition)
            txtSelectedCount = findViewById(R.id.txtSelectedCount)
            btnSaveSpeciesList = findViewById(R.id.btnSaveSpeciesList)
            btnCancel = findViewById(R.id.btnCancel)
            btnAddSpecies = findViewById(R.id.btnAddSpeciesToList)

            // Load data
            val fullList = SharedPreferencesManager.getFullSpeciesList(this).toMutableList()
            val selectedSet = SharedPreferencesManager.getSelectedSpecies(this).toSet()

            allSpeciesList = fullList.map { name ->
                SpeciesItem(name, SpeciesImageHelper.getSpeciesImageResId(name), isSelected = selectedSet.contains(name))
            }.toMutableList()

            updateSelectedCount()

            val adapter = AllSpeciesAdapter(this, allSpeciesList) { selected ->
                updateSelectedCount()
                btnSaveSpeciesList.isEnabled = selected.size in 1..8
            }

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            btnSaveSpeciesList.setOnClickListener {
                val selected = allSpeciesList.filter { it.isSelected }
                if (selected.size > 8) {
                    Toast.makeText(this, "Only 8 species can be selected!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Save 8 selected
                SharedPreferencesManager.saveOrderedSpeciesList(this, selected.map { it.name })

                // Save full list too
                SharedPreferencesManager.saveFullSpeciesList(this, allSpeciesList.map { it.name })

                Toast.makeText(this, "Species list saved", Toast.LENGTH_SHORT).show()
                finish()
            }

            btnCancel.setOnClickListener {
                finish()
            }

            btnAddSpecies.setOnClickListener {
                showAddSpeciesDialog()
            }
        }

        private fun updateSelectedCount() {
            val count = allSpeciesList.count { it.isSelected }
            txtSelectedCount.text = "Selected: $count / 8"
        }

        private fun showAddSpeciesDialog() {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Add Custom Species")
                .setMessage("Enter a new fish species:")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Species name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val exists = allSpeciesList.any { it.name.equals(name, ignoreCase = true) }
                    if (exists) {
                        Toast.makeText(this, "Species already exists", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val newItem = SpeciesItem(name, SpeciesImageHelper.getSpeciesImageResId(name), isSelected = false)
                    allSpeciesList.add(newItem)
                    recyclerView.adapter?.notifyItemInserted(allSpeciesList.size - 1)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

