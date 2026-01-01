package com.bramestorm.bassanglertracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.FishSpecies
import com.bramestorm.bassanglertracker.utils.SpeciesImageResolver

class SpeciesReorderAdapter(
    private val speciesList: MutableList<String>,
    private val onDeleteRequested: (String) -> Unit) : RecyclerView.Adapter<SpeciesReorderAdapter.ViewHolder>(){

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgDragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)
        val imgSpecies: ImageView = itemView.findViewById(R.id.imgSpeciesReorder)
        val txtSpeciesName: TextView = itemView.findViewById(R.id.txtSpeciesNameReorder)
        val imgDelete: ImageView = itemView.findViewById(R.id.imgDeleteSpecies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_reorder, parent, false) // ✅ CORRECT
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = speciesList[position]

        holder.itemView.setBackgroundResource(R.color.selection_list)
        holder.txtSpeciesName.text = name

        SpeciesImageResolver.loadInto(
            holder.itemView.context,
            name,
            holder.imgSpecies
        )

        val isUserAdded = name !in FishSpecies.allSpeciesList

        if (isUserAdded) {
            holder.imgDelete.visibility = View.VISIBLE
            holder.imgDelete.setOnClickListener { onDeleteRequested(name) }
        } else {
            holder.imgDelete.visibility = View.GONE
            holder.imgDelete.setOnClickListener(null)
        }
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
            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.setBackgroundResource(
                        R.color.softlock_green
                    )
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.setBackgroundResource(
                    R.color.selection_list
                )
            }



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

    fun updateList(newList: List<String>) {
        speciesList.clear()
        speciesList.addAll(newList)
        notifyDataSetChanged()
    }

}

