package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.activities.AllSpeciesSelectionActivity
import com.bramestorm.bassanglertracker.adapters.SpeciesReorderAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class SpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeciesReorderAdapter
    private lateinit var btnSave: Button
    private lateinit var btnAdjustList: Button
    private lateinit var txtTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSave = findViewById(R.id.btnSaveSpecies)
        btnAdjustList = findViewById(R.id.btnAdjustSpeciesList)
        txtTitle = findViewById(R.id.txtSpeciesTitle)

        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this).toMutableList()
        adapter = SpeciesReorderAdapter(speciesList)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(SpeciesItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnSave.setOnClickListener {
            val reorderedList = adapter.getCurrentOrder()
            SharedPreferencesManager.saveSelectedSpeciesList(this, reorderedList)
            Toast.makeText(this, "Species list saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnAdjustList.setOnClickListener {
            val intent = Intent(this, AllSpeciesSelectionActivity::class.java)
            startActivity(intent)
        }
    }
}
