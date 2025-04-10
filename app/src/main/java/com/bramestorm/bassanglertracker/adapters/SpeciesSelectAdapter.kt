package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.ItemMoveCallback.ItemTouchHelperContract
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager

class SpeciesSelectAdapter(
    private val speciesList: MutableList<SpeciesItem>
) : RecyclerView.Adapter<SpeciesSelectAdapter.SpeciesViewHolder>(), ItemTouchHelperContract {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_reorder, parent, false)
        return SpeciesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        holder.bind(speciesList[position])
    }

    override fun getItemCount(): Int = speciesList.size

    fun getOrderedSpeciesNames(): List<String> {
        return speciesList.map { it.name }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        val movedItem = speciesList.removeAt(fromPosition)
        speciesList.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    // ✅ Proper interface implementation using RecyclerView.ViewHolder
    override fun onRowSelected(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.setBackgroundResource(R.drawable.selected_background)
        viewHolder.itemView.alpha = 0.6f
    }

    override fun onRowClear(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.setBackgroundResource(R.color.lite_grey)
        viewHolder.itemView.alpha = 1.0f

        // Save reordered list here
        val newOrder = getOrderedSpeciesNames()
        SharedPreferencesManager.saveSelectedSpeciesList(viewHolder.itemView.context, newOrder)
    }



    inner class SpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSpeciesName: TextView = itemView.findViewById(R.id.txtSpeciesNameReorder)
        private val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesReorder)
        private val imgDragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)

        fun bind(speciesItem: SpeciesItem) {
            txtSpeciesName.text = speciesItem.name
            imgSpecies.setImageResource(speciesItem.imageResId)
        }
    }
    fun updateList(newList: List<SpeciesItem>) {
        speciesList.clear()
        speciesList.addAll(newList)
        notifyDataSetChanged() // This should now work!
    }

}
