package com.bramestorm.bassanglertracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper


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
    private val maxSelection = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerSpecies)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        btnSaveSpecies = findViewById(R.id.btnSaveSpecies)
        btnAddSpecies = findViewById(R.id.btnAddSpecies)

        val savedSpeciesNames = SharedPreferencesManager.getOrderedSpeciesList(this)
        val speciesList = savedSpeciesNames.map { name ->
            val icon = SpeciesImageHelper.getSpeciesImageResId(name)

            SpeciesItem(name, icon)
        }.toMutableList()

        val adapter = SpeciesReorderAdapter(speciesList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Enable drag-and-drop
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        updateSelectedCount(speciesList.size)

        // Save reordered list
        btnSaveSpecies.setOnClickListener {
            val newOrder = adapter.getOrderedList().map { it.name }
            SharedPreferencesManager.saveOrderedSpeciesList(this, newOrder)
            Toast.makeText(this, "Species selection saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Add custom species
        btnAddSpecies.setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Add Custom Species")
                .setMessage("Enter the species name:")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val newSpeciesName = input.text.toString().trim()
                    // ⛔ Check species limit INSIDE this block
                    if (speciesList.size >= maxSelection) {
                        Toast.makeText(this, "You can only select up to $maxSelection species.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (newSpeciesName.isNotEmpty() &&
                        speciesList.none { it.name.equals(newSpeciesName, ignoreCase = true) }
                    ) {
                        val imageRes = SpeciesImageHelper.getSpeciesImageResId(newSpeciesName)
                        val newSpecies = SpeciesItem(newSpeciesName, imageRes)
                        speciesList.add(newSpecies)
                        adapter.notifyItemInserted(speciesList.size - 1)
                        updateSelectedCount(speciesList.size)
                    } else {
                        Toast.makeText(this, "Invalid or duplicate species", Toast.LENGTH_SHORT).show()
                    }
                }

                .setNegativeButton("Cancel", null)
                .show()
        }
    }
        //++++++++++++ END On Create +++++++++++++++++++++++++++++++


    private fun updateSelectedCount(count: Int) {
        txtSelectedCount.text = getString(R.string.selected_species_count, count, 8)
        btnSaveSpecies.isEnabled = count > 0
    }

}
