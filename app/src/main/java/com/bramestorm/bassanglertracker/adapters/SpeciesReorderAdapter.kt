package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.getSpeciesImageResId

class SpeciesReorderAdapter(
    private val speciesList: MutableList<String>
) : RecyclerView.Adapter<SpeciesReorderAdapter.ViewHolder>() {


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgDragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesReorder)
        val txtSpeciesName: TextView = itemView.findViewById(R.id.txtSpeciesNameReorder)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_reorder, parent, false) // ✅ CORRECT
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = speciesList[position]
        holder.txtSpeciesName.text = name
        holder.imgSpecies.setImageResource(getSpeciesImageResId(name))
    }

    override fun getItemCount(): Int = speciesList.size

    fun getCurrentList(): List<String> {
        return speciesList
    }


    val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                val movedItem = speciesList.removeAt(fromPosition)
                speciesList.add(toPosition, movedItem)

                notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                // No swipe behavior
            }
        }

}

