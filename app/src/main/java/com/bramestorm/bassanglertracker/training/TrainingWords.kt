package com.bramestorm.bassanglertracker.training

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity

class UserTrainingVoiceCommands : AppCompatActivity() {

    private lateinit var btnSetUpUser: Button
    private lateinit var btnMenuUser: Button
    private lateinit var btnWhatIsVCC: Button
    private lateinit var btnEnableVCC : Button
    private lateinit var btnTeachVCC : Button




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_words)


    }

    private fun showPhrasePopup() {
        val builder = AlertDialog.Builder(this)
        val listView = ListView(this)
        val adapter = PhraseListAdapter(this, phraseList)

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            phraseList[position].isMastered = !phraseList[position].isMastered
            adapter.notifyDataSetChanged()
            updateSayThisUI()
        }

        builder.setTitle("Practice Phrases")
        builder.setView(listView)
        builder.setPositiveButton("Done", null)
        builder.show()
    }
    txtSelectWords
    private fun updateSayThisUI() {
        val currentText = txtSayThis.text.toString().replace("Say This: ", "")
        val phrase = phraseList.find { it.text == currentText }

        val bgColor = if (phrase?.isMastered == true) {
            ContextCompat.getColor(this, R.color.clip_green)
        } else {
            ContextCompat.getColor(this, R.color.clip_yellow)
        }

        txtSayThis.setBackgroundColor(bgColor)
    }


    // ########## WORDS & PHRASES TO PRACTICE WITH  #####################

    data class PracticePhrase(val text: String, var isMastered: Boolean)

    private val phraseList = mutableListOf(
        PracticePhrase("Save Catch", false),
        PracticePhrase("Large Mouth", false),
        PracticePhrase("Log Entry", false),
        PracticePhrase("New Fish", false),
        PracticePhrase("Walleye", false),
        PracticePhrase("Clear List", false),
        PracticePhrase("Small Mouth", false)
    )
}