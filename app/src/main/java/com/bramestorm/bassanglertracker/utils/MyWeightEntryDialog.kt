package com.bramestorm.bassanglertracker.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bramestorm.bassanglertracker.R

class MyWeightEntryDialogFragment(
    private val speciesList: List<String>,
    private val clipColorList: List<String>,
    private val onSave: (lbs: Int, oz: Int, species: String, clipColor: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 1) Inflate the custom view
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_vcc_tourn_lbs, null)

        // 2) Find your spinners
        val spSpecies   = view.findViewById<Spinner>(R.id.spinnerSpecies)
        val spClipColor = view.findViewById<Spinner>(R.id.spinnerClipColor)
        val spLbsTens   = view.findViewById<Spinner>(R.id.spinnerLbsTens)
        val spLbsOnes   = view.findViewById<Spinner>(R.id.spinnerLbsOnes)
        val spOunces    = view.findViewById<Spinner>(R.id.spinnerOunces)

        // 3) Populate adapters
        spSpecies.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            speciesList
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spClipColor.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            clipColorList
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // 4) Wire up your own buttons (from the layout)
        val btnSave   = view.findViewById<Button>(R.id.btnSaveWeight)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // Build the AlertDialog with JUST the view
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        // 5) Hook your Save/Cancel logic
        btnSave.setOnClickListener {
            val lbs     = (spLbsTens.selectedItem as Int) * 10 + (spLbsOnes.selectedItem as Int)
            val oz      = spOunces.selectedItem as Int
            val species = spSpecies.selectedItem as String
            val color   = spClipColor.selectedItem as String

            onSave(lbs, oz, species, color)
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }
}
