package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.activities.AllSpeciesSelectionActivity
import com.bramestorm.bassanglertracker.adapters.ItemMoveCallback
import com.bramestorm.bassanglertracker.adapters.SpeciesSelectAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class SpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeciesSelectAdapter
    private lateinit var btnSaveSpecies: Button
    private lateinit var btnAdjustList: Button
    private lateinit var txtTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSaveSpecies = findViewById(R.id.btnSaveSpecies)
        btnAdjustList = findViewById(R.id.btnAdjustSpeciesList)
        txtTitle = findViewById(R.id.txtSpeciesTitle)

        // Make sure defaults are set if missing
        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        val selectedSpecies = SharedPreferencesManager.getSelectedSpeciesList(this).toMutableList()
        Log.d("SpeciesSelectionActivity", "Loaded selectedSpecies: $selectedSpecies")

        adapter = SpeciesSelectAdapter(selectedSpecies)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(ItemMoveCallback(adapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        adapter.onDragHandleTouch = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        btnSaveSpecies.isEnabled = true

        // Save reordered list
        btnSaveSpecies.setOnClickListener {
            val reorderedList = adapter.getCurrentList()
            SharedPreferencesManager.saveSelectedSpeciesList(this, reorderedList)

            Toast.makeText(this, "Species list saved!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, SetUpActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Open full list editor
        btnAdjustList.setOnClickListener {
            val intent = Intent(this, AllSpeciesSelectionActivity::class.java)
            startActivity(intent)
        }
    }
}
