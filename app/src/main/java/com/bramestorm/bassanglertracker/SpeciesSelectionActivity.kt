package com.bramestorm.bassanglertracker.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.SpeciesSelectAdapter
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class SpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeciesSelectAdapter
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnResetSpecies:Button

    private val selectedSpecies = mutableListOf<SpeciesItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSave = findViewById(R.id.btnSaveSpecies)
        btnCancel = findViewById(R.id.btnCancelSpecies)
        btnResetSpecies = findViewById(R.id.btnResetSpecies)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load previously selected species
        val savedSpecies = SharedPreferencesManager
            .getSelectedSpeciesList(this)
            .map { SpeciesItem(it, isSelected = true) }

        selectedSpecies.clear()
        selectedSpecies.addAll(savedSpecies)

        adapter = SpeciesSelectAdapter(selectedSpecies) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        recyclerView.adapter = adapter
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnSave.setOnClickListener {
            val orderedSpecies = adapter.getOrderedSpeciesNames()
            SharedPreferencesManager.saveSelectedSpeciesList(this, orderedSpecies)
            Log.d("SpeciesSelection", "Saved ordered species: $orderedSpecies")
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int = makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        )

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // No swipe actions
        }

        override fun isLongPressDragEnabled(): Boolean = false // use handle
    })
}
