package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem

class SpeciesSelectAdapter(
    val speciesList: MutableList<SpeciesItem>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<SpeciesSelectAdapter.SpeciesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_species_drag, parent, false)
        return SpeciesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        holder.bind(speciesList[position])
    }

    override fun getItemCount(): Int = speciesList.size

    fun getOrderedSpeciesNames(): List<String> {
        return speciesList.map { it.name }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val movedItem = speciesList.removeAt(fromPosition)
        speciesList.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    inner class SpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSpeciesName: TextView = itemView.findViewById(R.id.txtSpeciesName)
        private val imgDragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)

        fun bind(speciesItem: SpeciesItem) {
            txtSpeciesName.text = speciesItem.name
            imgDragHandle.setOnTouchListener { _, _ ->
                onStartDrag(this)
                false
            }
        }
    }
}
