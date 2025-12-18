package com.bramestorm.bassanglertracker.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class AllSpeciesAdapter(
    private val context: Context,
    private val speciesList: MutableList<SpeciesItem>,
    private val onDeleteConfirmed: (speciesName: String) -> Unit
) : RecyclerView.Adapter<AllSpeciesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSpecies: TextView = itemView.findViewById(R.id.txtSpeciesNameSelect)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = speciesList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = speciesList[position]

        holder.txtSpecies.text = item.name
        when {
            item.imageUri != null -> {
                holder.imgSpecies.setImageURI(Uri.parse(item.imageUri))
            }
            item.imageResId != 0 -> {
                holder.imgSpecies.setImageResource(item.imageResId)
            }
            else -> {
                holder.imgSpecies.setImageResource(R.drawable.fish_default)
            }
        }

        // Long-press = "Oops, delete this entry"
        holder.itemView.setOnLongClickListener {
            confirmDelete(item.name, position)
            true
        }
    }

    private fun confirmDelete(speciesName: String, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Species")
            .setMessage("Are you sure you want to delete '$speciesName'?")
            .setPositiveButton("Delete") { _, _ ->
                // Update UI list immediately
                speciesList.removeAt(position)
                notifyItemRemoved(position)

                // Clean up stored image (if any)
                SharedPreferencesManager.saveSpeciesImageUri(context, speciesName, null)

                // Tell Activity to persist delete
                onDeleteConfirmed(speciesName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
