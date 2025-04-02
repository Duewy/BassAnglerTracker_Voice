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

class AllSpeciesAdapter(
    private val speciesList: List<String>,
    private val initiallySelectedSpecies: Set<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AllSpeciesAdapter.ViewHolder>() {

    private val selectedState = mutableMapOf<String, Boolean>().apply {
        speciesList.forEach { species ->
            this[species] = initiallySelectedSpecies.contains(species)
        }
    }

    fun uncheckSpecies(species: String) {
        selectedState[species] = false
        notifyItemChanged(speciesList.indexOf(species))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSpecies: TextView = itemView.findViewById(R.id.txtSpeciesNameSelect)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesSelect)
        val chkSpecies: CheckBox = itemView.findViewById(R.id.chkSelectSpeciesNameSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_select, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = speciesList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val species = speciesList[position]

        holder.txtSpecies.text = species
        val imageRes = getSpeciesImageResId(species)
        holder.imgSpecies.setImageResource(if (imageRes != 0) imageRes else R.drawable.fish_default)

        holder.chkSpecies.setOnCheckedChangeListener(null)
        holder.chkSpecies.isChecked = selectedState[species] == true

        holder.chkSpecies.setOnCheckedChangeListener { _, isChecked ->
            selectedState[species] = isChecked
            onSelectionChanged(species, isChecked)
        }
    }

    fun getSelectedSpecies(): List<String> {
        return selectedState.filterValues { it }.keys.toList()
    }
}
