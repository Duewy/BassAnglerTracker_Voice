package com.bramestorm.bassanglertracker.utils

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * Stub implementation of MyWeightEntryDialogFragment.
 * Accepts species and clip color lists plus a save callback.
 * Currently invokes the callback immediately with default values.
 */
class MyWeightEntryDialogFragment(
    private val speciesList: Array<String>,
    private val clipColorList: Array<String>,
    private val onSave: (inches: Int, quarters: Int, species: String, clipColor: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Immediately call the save callback with placeholder values
        onSave(
            /* inches = */ 0,
            /* quarters = */ 0,
            /* species = */ speciesList.firstOrNull().orEmpty(),
            /* clipColor = */ clipColorList.firstOrNull().orEmpty()
        )
        // Provide a minimal dialog so the fragment shows
        return AlertDialog.Builder(requireContext())
            .setTitle("Weight Entry")
            .setMessage("Stub dialog - real implementation pending.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
    }
}
