package com.bramestorm.bassanglertracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class SpeciesSpinnerAdapter(
    context: Context,
    private val speciesList: List<SpeciesItem>
) : ArrayAdapter<SpeciesItem>(context, 0, speciesList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_species_item, parent, false)

        val item = speciesList[position]
        view.findViewById<ImageView>(R.id.imgSpecies).setImageResource(item.imageResId)
        view.findViewById<TextView>(R.id.txtSpeciesName).text = item.name

        return view
    }
}
