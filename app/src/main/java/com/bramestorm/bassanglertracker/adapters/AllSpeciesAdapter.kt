package com.bramestorm.bassanglertracker.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.FishSpecies
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class AllSpeciesAdapter(
    private val context: Context,
    private var speciesList: MutableList<SpeciesItem>,
    private val initiallySelectedSpecies: Set<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AllSpeciesAdapter.ViewHolder>() {

    private val selectedState = mutableMapOf<String, Boolean>().apply {
        speciesList.forEach { speciesItem ->
            val normalized = SharedPreferencesManager.normalizeSpeciesName(speciesItem.name)
            this[speciesItem.name] = initiallySelectedSpecies.contains(normalized)
        }
    }

    fun uncheckSpecies(species: String) {
        selectedState[species] = false
        val index = speciesList.indexOfFirst { it.name == species }
        if (index != -1) notifyItemChanged(index)
    }

    fun getSelectedSpecies(): List<String> {
        return speciesList.filter { selectedState[it.name] == true }.map { it.name }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSpecies: TextView = itemView.findViewById(R.id.txtSpeciesNameSelect)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesSelect)
        val chkSpecies: CheckBox = itemView.findViewById(R.id.chkSelectSpeciesNameSelect)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditSpeciesNameSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = speciesList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val speciesItem = speciesList[position]

        holder.txtSpecies.text = speciesItem.name
        holder.imgSpecies.setImageResource(
            if (speciesItem.imageResId != 0) speciesItem.imageResId else R.drawable.fish_default
        )

        holder.chkSpecies.setOnCheckedChangeListener(null)
        holder.chkSpecies.isChecked = selectedState[speciesItem.name] == true
        holder.chkSpecies.setOnCheckedChangeListener { _, isChecked ->
            selectedState[speciesItem.name] = isChecked
            onSelectionChanged(speciesItem.name, isChecked)
        }

        val isUserAdded = !FishSpecies.allSpeciesList
            .map { SharedPreferencesManager.normalizeSpeciesName(it) }
            .contains(SharedPreferencesManager.normalizeSpeciesName(speciesItem.name))


        holder.btnEdit.visibility = if (isUserAdded) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener {
            showPopupOptions(holder.btnEdit, position)
        }



    }//--------- END onBindViewHolder________________

    private fun showPopupOptions(anchorView: View, position: Int) {
        val speciesItem = speciesList[position]

        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(context)
            .setTitle("Edit or Delete '${speciesItem.name}'?")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditDialog(position)  // Edit
                    1 -> confirmDelete(position)   // Delete
                }
            }
            .show()
    }

    private fun showEditDialog(position: Int) {
        val speciesItem = speciesList[position]
        val editText = EditText(context).apply {
            setText(speciesItem.name)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Species Name")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val normalizedNew = SharedPreferencesManager.normalizeSpeciesName(newName)
                    val alreadyExists = SharedPreferencesManager.getMasterSpeciesList(context).any {
                        SharedPreferencesManager.normalizeSpeciesName(it) == normalizedNew &&
                                SharedPreferencesManager.normalizeSpeciesName(it) != SharedPreferencesManager.normalizeSpeciesName(speciesItem.name)
                    }

                    if (alreadyExists) {
                        Toast.makeText(context, "That species name already exists!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val oldName = speciesItem.name
                    speciesItem.name = newName
                    notifyItemChanged(position)
                    SharedPreferencesManager.updateUserSpeciesName(context, oldName, newName)
                    Toast.makeText(context, "Species name updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun confirmDelete(position: Int) {
        val speciesItem = speciesList[position]

        AlertDialog.Builder(context)
            .setTitle("Delete Species")
            .setMessage("Are you sure you want to delete '${speciesItem.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                SharedPreferencesManager.removeUserSpecies(context, speciesItem.name)
                speciesList.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(context, "Species deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}
