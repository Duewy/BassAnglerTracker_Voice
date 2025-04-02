package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.getSpeciesImageResId
import java.util.Collections

class SpeciesReorderAdapter(
    private val speciesList: MutableList<String>
) : RecyclerView.Adapter<SpeciesReorderAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtSpeciesName: TextView = view.findViewById(R.id.txtSpeciesName)
        val imgSpecies: ImageView = view.findViewById(R.id.imgSpecies)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = speciesList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val species = speciesList[position]
        holder.txtSpeciesName.text = species
        holder.imgSpecies.setImageResource(getSpeciesImageResId(species))
    }

    fun moveItem(from: Int, to: Int) {
        Collections.swap(speciesList, from, to)
        notifyItemMoved(from, to)
    }

    fun getCurrentOrder(): List<String> = speciesList
}
