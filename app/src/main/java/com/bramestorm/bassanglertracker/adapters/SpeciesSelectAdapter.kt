package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.getSpeciesImageResId

class SpeciesSelectAdapter(
    private val speciesList: MutableList<String>
) : RecyclerView.Adapter<SpeciesSelectAdapter.ViewHolder>() {

    var onDragHandleTouch: ((RecyclerView.ViewHolder) -> Unit)? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSpecies: TextView = itemView.findViewById(R.id.txtSpeciesNameReorder)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesReorder)
        val dragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_reorder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val species = speciesList[position]
        holder.txtSpecies.text = species

        val imageRes = getSpeciesImageResId(species)
        holder.imgSpecies.setImageResource(
            if (imageRes != 0) imageRes else R.drawable.fish_default
        )

        holder.dragHandle.setOnTouchListener { _, _ ->
            onDragHandleTouch?.invoke(holder)
            false
        }
    }

    override fun getItemCount(): Int = speciesList.size

    fun moveItem(from: Int, to: Int) {
        val item = speciesList.removeAt(from)
        speciesList.add(to, item)
        notifyItemMoved(from, to)
    }

    fun getCurrentList(): List<String> = speciesList.toList()
}
