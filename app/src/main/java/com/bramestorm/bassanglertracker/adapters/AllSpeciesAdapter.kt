package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class AllSpeciesAdapter(
    private val speciesList: List<SpeciesItem>,
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
    }

    fun getSelectedSpecies(): List<String> {
        return speciesList.filter { selectedState[it.name] == true }.map { it.name }
    }
} // ------------------- END ---------------------------------
