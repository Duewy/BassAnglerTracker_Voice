package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import java.util.Collections

class SpeciesReorderAdapter(
    private val speciesList: MutableList<SpeciesItem>
) : RecyclerView.Adapter<SpeciesReorderAdapter.SpeciesViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class SpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val speciesName: TextView = itemView.findViewById(R.id.txtSpeciesName)
        val speciesIcon: ImageView = itemView.findViewById(R.id.imgSpecies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return SpeciesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        val item = speciesList[position]
        holder.speciesName.text = item.name
        holder.speciesIcon.setImageResource(item.imageResId)

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.color.highlight_yellow)
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getItemCount(): Int = speciesList.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(speciesList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        selectedPosition = toPosition
    }

    fun getCurrentOrder(): List<String> {
        return speciesList.map { it.name }
    }
}
