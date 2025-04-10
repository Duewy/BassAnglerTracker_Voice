package com.bramestorm.bassanglertracker.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity
import com.bramestorm.bassanglertracker.adapters.SpeciesSelectAdapter
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.ItemMoveCallback
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.getSpeciesImageResId

class SpeciesSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSaveSpecies: Button
    private lateinit var btnCancel: Button
    private lateinit var btnAdjustSpeciesList: Button
    private lateinit var btnResetSpecies: Button
    private lateinit var adapter: SpeciesSelectAdapter
    private val speciesList = mutableListOf<SpeciesItem>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        //______________ FIND ID BY _____________________
        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSaveSpecies = findViewById(R.id.btnSaveSpecies)
        btnAdjustSpeciesList = findViewById(R.id.btnAdjustSpeciesList)
        btnResetSpecies = findViewById(R.id.btnResetSpecies)
        btnCancel = findViewById(R.id.btnCancelSpecies)


        val selectedSpeciesNames = SharedPreferencesManager.getSelectedSpeciesList(this)
        val speciesItems = selectedSpeciesNames.map { name ->
            SpeciesItem(
                name = name,
                imageResId = getSpeciesImageResId(name),
                isSelected = true
            )
        }.toMutableList()


        adapter = SpeciesSelectAdapter(speciesItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val callback = ItemMoveCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)

        // SAVE button → save reordered list to SharedPreferences
        btnSaveSpecies.setOnClickListener {
            val toast = Toast.makeText(this,   "👍 Species List Saved Successfully", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

            val ordered = adapter.getOrderedSpeciesNames()
            SharedPreferencesManager.saveSelectedSpeciesList(this, ordered)

            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        // CANCEL button → return to SetUpActivity
        btnCancel.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        // ADJUST button → open AllSpeciesSelectionActivity
        btnAdjustSpeciesList.setOnClickListener {
            val intent = Intent(this, AllSpeciesSelectionActivity::class.java)
            startActivity(intent)
        }

        // RESET button → restore default species list
        btnResetSpecies.setOnClickListener {
            val toast = Toast.makeText(this, "🐟 Species List RESET to Starting 8", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

                  SharedPreferencesManager.resetToDefaultSpecies(this)
            val defaultSpecies = SharedPreferencesManager.getSelectedSpeciesList(this)
            val updatedSpecies = defaultSpecies.map {
                SpeciesItem(it, getSpeciesImageResId(it), true)
            }.toMutableList()
            // Re-create and reassign the adapter
            adapter = SpeciesSelectAdapter(updatedSpecies)
            recyclerView.adapter = adapter


        }


    }//--------- END On Create -------------------------

    override fun onResume() {
        super.onResume()
        val selectedSpeciesNames = SharedPreferencesManager.getSelectedSpeciesList(this)
        val speciesItems = selectedSpeciesNames.map { name ->
            SpeciesItem(
                name = name,
                imageResId = getSpeciesImageResId(name),
                isSelected = true
            )
        }.toMutableList()

        adapter.updateList(speciesItems)
    }

}//-------END ------------------