package com.bramestorm.bassanglertracker.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.getSpeciesImageResId

class AllSpeciesAdapter(
    private val context: Context,
    private val allSpeciesList: MutableList<SpeciesItem>,
    private val onSelectionChanged: (List<SpeciesItem>) -> Unit
) : RecyclerView.Adapter<AllSpeciesAdapter.SpeciesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return SpeciesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        val species = allSpeciesList[position]
        holder.bind(species)
    }

    override fun getItemCount(): Int = allSpeciesList.size

    inner class SpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSpeciesName: TextView = itemView.findViewById(R.id.txtSpeciesName)
        private val imgSpeciesIcon: ImageView = itemView.findViewById(R.id.imgSpeciesIcon)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditSpecies)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteSpecies)


        fun bind(species: SpeciesItem) {
            txtSpeciesName.text = species.name
            imgSpeciesIcon.setImageResource(species.imageResId)

            val backgroundColor = if (species.isSelected) {
                ContextCompat.getColor(context, R.color.selected_2_background)
            } else {
                Color.TRANSPARENT
            }
            itemView.setBackgroundColor(backgroundColor)

            itemView.setOnClickListener {
                val selectedCount = allSpeciesList.count { it.isSelected }

                if (!species.isSelected && selectedCount >= 8) {
                    Toast.makeText(
                        context,
                        "You can only select up to 8 species. Please deselect one first.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                species.isSelected = !species.isSelected
                notifyItemChanged(adapterPosition)
                onSelectionChanged(allSpeciesList.filter { it.isSelected })
            }
            btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete ${species.name}?")
                    .setMessage("This will remove the species from the list.")
                    .setPositiveButton("Delete") { _, _ ->
                        allSpeciesList.removeAt(adapterPosition)
                        notifyItemRemoved(adapterPosition)
                        onSelectionChanged(allSpeciesList.filter { it.isSelected })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            btnEdit.setOnClickListener {
                val input = EditText(context)
                input.setText(species.name)

                AlertDialog.Builder(context)
                    .setTitle("Edit Species Name")
                    .setView(input)
                    .setPositiveButton("Save") { _, _ ->
                        val newName = input.text.toString().trim()
                        if (newName.isNotEmpty() && allSpeciesList.none { it.name.equals(newName, true) }) {
                            species.name = newName
                            species.imageResId = getSpeciesImageResId(newName)
                            notifyItemChanged(adapterPosition)
                            onSelectionChanged(allSpeciesList.filter { it.isSelected })
                        } else {
                            Toast.makeText(context, "Invalid or duplicate name", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

        }
    }
}
