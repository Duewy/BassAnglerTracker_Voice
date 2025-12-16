package com.bramestorm.bassanglertracker.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.FishSpecies

class AllSpeciesAdapter(
    private val context: Context,
    private val speciesList: MutableList<SpeciesItem>,
    private val onListChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<AllSpeciesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSpecies: TextView = itemView.findViewById(R.id.txtSpeciesNameSelect)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesSelect)
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
            if (speciesItem.imageResId != 0)
                speciesItem.imageResId
            else
                R.drawable.fish_default
        )

        val isUserAdded = !FishSpecies.allSpeciesList.contains(speciesItem.name)

        holder.btnEdit.visibility = if (isUserAdded) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener {
            showEditDeleteDialog(position)
        }
    }

    // ---------- Edit / Delete ----------

    private fun showEditDeleteDialog(position: Int) {
        val speciesItem = speciesList[position]

        AlertDialog.Builder(context)
            .setTitle("Edit or Delete '${speciesItem.name}'?")
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditDialog(position)
                    1 -> confirmDelete(position)
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
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (speciesList.any { it.name.equals(newName, ignoreCase = true) }) {
                    Toast.makeText(context, "That species already exists.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                speciesItem.name = newName
                notifyItemChanged(position)
                onListChanged(speciesList.map { it.name })
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
                speciesList.removeAt(position)
                notifyItemRemoved(position)
                onListChanged(speciesList.map { it.name })
                Toast.makeText(context, "Species deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
