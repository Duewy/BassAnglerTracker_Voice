package com.bramestorm.bassanglertracker.mappopups

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class PopupQuerySpecies(
    private val context: Context,
    private val onSpeciesSelected: (String) -> Unit
) {
    fun showPopup() {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_query_species, null)
        val spinnerSpecies = view.findViewById<Spinner>(R.id.spinnerSpeciesQuery)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmSpeciesQuery)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelSpeciesQuery)

        val dialog = AlertDialog.Builder(context).setView(view).create()

        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(context)
        if (speciesList.isEmpty()) {
            Toast.makeText(context, "No species found. Please select species first.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        spinnerSpecies.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            speciesList
        )

        btnConfirm.setOnClickListener {
            val selected = spinnerSpecies.selectedItem.toString()
            onSpeciesSelected(selected)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
