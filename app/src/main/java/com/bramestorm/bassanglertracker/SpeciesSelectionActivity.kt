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
    private lateinit var btnResetSpecies:Button
    private lateinit var txtTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_selection)

        recyclerView = findViewById(R.id.recyclerSpecies)
        btnSaveSpecies = findViewById(R.id.btnSaveSpecies)
        btnAdjustList = findViewById(R.id.btnAdjustSpeciesList)
        btnResetSpecies = findViewById(R.id.btnResetSpecies)
        txtTitle = findViewById(R.id.txtSpeciesTitle)

        txtTitle.text = "The Species List"

        // Make sure defaults are set if missing
        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        val selectedSpecies = SharedPreferencesManager.getOrderedSelectedSpeciesList(this).toMutableList()

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
            val newOrderedList = adapter.getCurrentList()
            SharedPreferencesManager.saveSelectedSpeciesList(this, newOrderedList)
            Log.d("SaveReorder", "Saved species order: $newOrderedList")

            Toast.makeText(this, "Species list saved!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, SetUpActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Open full list editor
        btnAdjustList.setOnClickListener {
            val intent = Intent(this, AllSpeciesSelectionActivity::class.java)
            startActivityForResult(intent, 101)
        }

        btnResetSpecies.setOnClickListener {
            SharedPreferencesManager.clearSpeciesPreferences(this)
            SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)
            loadRecyclerView() // or similar function to refresh adapter
        }

    }//-------------------- END On Create ---------------------------

    //----------- RESET the Species List to Original State --------------------

    private fun loadRecyclerView() {
        val selectedSpecies = SharedPreferencesManager.getOrderedSelectedSpeciesList(this).toMutableList()
        adapter = SpeciesSelectAdapter(selectedSpecies)
        recyclerView.adapter = adapter
    }


    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            // Reload species list from SharedPreferences
            loadRecyclerView()
        }
    }

}
