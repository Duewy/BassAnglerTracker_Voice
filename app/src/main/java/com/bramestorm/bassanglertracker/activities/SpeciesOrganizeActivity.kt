package com.bramestorm.bassanglertracker.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.adapters.SpeciesReorderAdapter
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class SpeciesOrganizeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeciesReorderAdapter
    private lateinit var btnGotoAddSpeciesList: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_organize)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSave = findViewById(R.id.btnSaveListOrder)
        btnGotoAddSpeciesList = findViewById(R.id.btnAddSpeciesList)
        btnCancel = findViewById(R.id.btnCancelSpecies)

        val speciesCatalogue =
            SharedPreferencesManager.getSpeciesCatalogue(this).toMutableList()

        adapter = SpeciesReorderAdapter(speciesCatalogue)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Drag & drop support
        val touchHelper = ItemTouchHelper(adapter.itemTouchHelperCallback)
        touchHelper.attachToRecyclerView(recyclerView)

        btnGotoAddSpeciesList.setOnClickListener {
            val intent = Intent(this,SpeciesCatalogueActivity::class.java)
            startActivity(intent)
        }

        btnSave.setOnClickListener {
            SharedPreferencesManager.saveSpeciesCatalogue(
                this,
                adapter.getCurrentList()
            )
            finish()
        }


        btnCancel.setOnClickListener {
            finish()
        }
    }
}
