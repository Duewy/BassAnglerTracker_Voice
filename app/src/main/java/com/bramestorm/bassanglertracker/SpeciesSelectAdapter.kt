package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.getSpeciesImageResId

class SpeciesSelectAdapter(
    private val speciesList: List<String>,
    private val selectedSpecies: MutableSet<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<SpeciesSelectAdapter.ViewHolder>() {

    // To track checkbox state
    private val selectedMap = mutableMapOf<String, Boolean>().apply {
        speciesList.forEach { this[it] = selectedSpecies.contains(it) }
    }

    fun uncheckSpecies(species: String) {
        selectedMap[species] = false
        notifyItemChanged(speciesList.indexOf(species))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSpecies: TextView = itemView.findViewById(R.id.txtSpeciesName)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpecies)
        val chkSpecies: CheckBox = itemView.findViewById(R.id.chkSelectSpecies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_species_select, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = speciesList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val species = speciesList[position]

        holder.txtSpecies.text = species
        holder.imgSpecies.setImageResource(getSpeciesImageResId(species))

        holder.chkSpecies.setOnCheckedChangeListener(null)
        holder.chkSpecies.isChecked = selectedMap[species] == true

        holder.chkSpecies.setOnCheckedChangeListener { _, isChecked ->
            selectedMap[species] = isChecked
            onSelectionChanged(species, isChecked)
        }
    }
}
