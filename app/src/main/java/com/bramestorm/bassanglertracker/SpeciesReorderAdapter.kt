package com.bramestorm.bassanglertracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.models.SpeciesItem
import java.util.Collections

class SpeciesReorderAdapter(
    private val speciesList: MutableList<SpeciesItem>
) : RecyclerView.Adapter<SpeciesReorderAdapter.SpeciesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return SpeciesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        val speciesItem = speciesList[position]
        holder.bind(speciesItem)
    }

    override fun getItemCount(): Int = speciesList.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(speciesList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getOrderedList(): List<SpeciesItem> = speciesList

    inner class SpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val speciesName: TextView = itemView.findViewById(R.id.txtSpeciesName)
        private val speciesIcon: ImageView = itemView.findViewById(R.id.imgSpecies)

        fun bind(item: SpeciesItem) {
            speciesName.text = item.name
            speciesIcon.setImageResource(item.imageResId)

            //--------- Highlight the Item that is Selected ----------------
            if (item.isSelected) {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.selected_background))
            } else {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.unselected_background))
            }

            // Toggle selection when clicked
            itemView.setOnClickListener {
                val selectedCount = speciesList.count { it.isSelected }

                if (!item.isSelected && selectedCount >= 8) {
                    // Already 8 selected, can't add more
                    Toast.makeText(
                        itemView.context,
                        "You can only select up to 8 species. Please deselect one first.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Toggle selection
                item.isSelected = !item.isSelected
                notifyItemChanged(adapterPosition)
            }

        }

    }
}
