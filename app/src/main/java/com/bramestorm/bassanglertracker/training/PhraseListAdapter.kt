    package com.bramestorm.bassanglertracker.training

    import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bramestorm.bassanglertracker.R


    class PhraseListAdapter(
        private val context: Context, private val phrases: List<PracticePhrase>) : BaseAdapter()
    {
        override fun getCount(): Int = phrases.size
        override fun getItem(position: Int): Any = phrases[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rowView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.popup_phrase_list_statues, parent, false)

            val imgProgress = rowView.findViewById<ImageView>(R.id.imgProgress)
            val txtPhrase = rowView.findViewById<TextView>(R.id.txtPhraseText)


            val phrase = phrases[position]
            txtPhrase.text = phrase.text

            // Change color based on mastered status
            val bgColor = if (phrase.isMastered) R.color.clip_green else R.color.clip_yellow
            imgProgress.setBackgroundResource(bgColor)

            return rowView
        }
    }
