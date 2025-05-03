package com.bramestorm.bassanglertracker.training

// TrainingWords.kt
// - Handles voice training phrases
// - Also runs voice-to-species mapping using VoiceInputMapper
// - Uses SpeechRecognizer for phrase comparison and species recognition


import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity
import com.bramestorm.bassanglertracker.training.VoiceInputMapper.loadUserVoiceMap
import com.bramestorm.bassanglertracker.training.VoiceInputMapper.saveUserVoiceMap
import java.util.Locale


class TrainingWords : AppCompatActivity() {

    private lateinit var btnSetUpVCC: Button
    private lateinit var btnMenuVCC: Button
    private lateinit var btnUserTalk: Button
    private lateinit var txtWhatComputerHeard : TextView
    private lateinit var txtSayThis : TextView
    private lateinit var speechIntent: Intent
    private val phraseList: MutableList<PracticePhrase> = VoiceCommandList.phraseList
    private val speechRequestCode = 1001
    private val recordAudioRequestCode = 101
    private lateinit var textToSpeech: TextToSpeech
    private var selectedPhrase: PracticePhrase? = null
    private lateinit var userVoiceMap: MutableMap<String, String>




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_words)

        VoiceInputMapper.loadUserVoiceMap(this)
        userVoiceMap = loadUserVoiceMap(this).toMutableMap()


        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the phrase...")
        }

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        checkAudioPermission()

        btnSetUpVCC = findViewById(R.id.btnSetUpVCC)
        btnMenuVCC = findViewById(R.id.btnMenuVCC)
        txtSayThis = findViewById(R.id.txtSayThis)
        btnUserTalk = findViewById(R.id.btnUserTalk)
        txtWhatComputerHeard = findViewById(R.id.txtWhatComputerHeard)

        btnSetUpVCC.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        btnMenuVCC.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        //~~~~~~~~~~ Opens the Mic for User ~~~~~~~~~~~~~~~~~~~~
        btnUserTalk.setOnClickListener {
            val phraseToSay = txtSayThis.text.toString().replace("Say This: ", "").trim()

            // Check if selected phrase exists in the list
            val isValidPhrase = phraseList.any { it.text.equals(phraseToSay, ignoreCase = true) }
            loadPhraseStatsFromPrefs()

            if (!isValidPhrase) {
                Toast.makeText(this, "Please select a word to practice.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prompt = "Say: \"$phraseToSay\""

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }

            try {
                startActivityForResult(intent, speechRequestCode)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Your device doesn't support speech input", Toast.LENGTH_SHORT).show()
            }
        }




            // !!!!!!!!!!! Gets Words for User to SAY  !!!!!!!!!!!!!!!!!

        txtSayThis.setOnClickListener {
            showPhrasePopup()
        }


    }// ================== END On Create ===================================

    override fun onDestroy() {
        super.onDestroy()
        saveUserVoiceMap(this, userVoiceMap)
    }


    private fun savePhraseStatsToPrefs() {
        val prefs = getSharedPreferences("PhraseTrainingPrefs", MODE_PRIVATE)
        val editor = prefs.edit()

        for (phrase in phraseList) {
            val keyBase = phrase.text.lowercase().replace(" ", "_")
            editor.putInt("${keyBase}_success", phrase.successCount)
            editor.putInt("${keyBase}_failure", phrase.failureCount)
            editor.putBoolean("${keyBase}_mastered", phrase.isMastered)
        }

        editor.apply()
    }

    private fun loadPhraseStatsFromPrefs() {
        val prefs = getSharedPreferences("PhraseTrainingPrefs", MODE_PRIVATE)

        for (phrase in phraseList) {
            val keyBase = phrase.text.lowercase().replace(" ", "_")
            phrase.successCount = prefs.getInt("${keyBase}_success", 0)
            phrase.failureCount = prefs.getInt("${keyBase}_failure", 0)
            phrase.isMastered = prefs.getBoolean("${keyBase}_mastered", false)
        }
    }


    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted
            // ... your code to start recording audio ...
            println("Permission is granted")
        } else {
            // Permission is not granted, request it
            requestAudioPermission()
        }
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            recordAudioRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == recordAudioRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                // ... your code to start recording audio ...
                println("Permission was granted")
            } else {
                // Permission was denied
                // ... handle the denial ...
                println("Permission was denied")
            }
        }
    }

// ___________________ Set the WORD / PHRASE List for the user to say  ___________________

    private fun showPhrasePopup() {
        val builder = AlertDialog.Builder(this)

        // Create a container layout to hold the ListView
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Dynamically sized ListView
        val listView = ListView(this).apply {
            val screenHeight = resources.displayMetrics.heightPixels
            val maxListHeight = (screenHeight * 0.5).toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxListHeight
            )

            adapter = PhraseListAdapter(this@TrainingWords, phraseList)
        }

        // Handle phrase selection
        listView.setOnItemClickListener { _, _, position, _ ->
            phraseList.forEach { it.isMastered = false }

            // ✅ Track which phrase is selected
            selectedPhrase = phraseList[position]
            selectedPhrase!!.isMastered = true
            savePhraseStatsToPrefs()

            Log.d("PhrasePopup", "Selected: ${selectedPhrase!!.text}")
            Toast.makeText(this, "Selected: ${selectedPhrase!!.text}", Toast.LENGTH_SHORT).show()

            txtSayThis.text = "Say This: ${selectedPhrase!!.text}"
            txtSayThis.requestLayout()
            txtSayThis.invalidate()

            // Update UI visuals
            (listView.adapter as PhraseListAdapter).notifyDataSetChanged()
            updateSayThisUI()
        }


        container.addView(listView)

        builder.setTitle("Practice Words/Phrases")
            .setView(container)
            .setPositiveButton("Done", null)
            .show()
    }




    //~~~~~~~~~~~ Say THIS  ~~~~~~~~~~~~~~~~~~~~~~~~~~

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


    //_________________ Get Data from Microphone _______________

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == speechRequestCode && resultCode == RESULT_OK) {
            val resultList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = resultList?.firstOrNull()

            spokenText?.let {
                handleVoiceInput(it)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



    private fun handleVoiceInput(rawInput: String) {
        val normalizedInput = rawInput.lowercase().replace(" ", "").trim()
        val currentPhraseTextRaw = txtSayThis.text.toString().replace("Say This: ", "").trim()
        val currentPhraseText = currentPhraseTextRaw.lowercase().replace(" ", "")
        val matchedSpecies = VoiceInputMapper.getSpeciesFromVoice(rawInput)
        val matchedNormalized = matchedSpecies.lowercase().replace(" ", "")

        val phrase = phraseList.find {
            it.text.lowercase().replace(" ", "") == currentPhraseText
        }

        if (matchedNormalized == currentPhraseText && phrase != null) {
            txtWhatComputerHeard.text = "✔ You said: \"$rawInput\" — That’s a match for \"${phrase.text}\"!"
            phrase.successCount++
            phrase.recentFailures = 0
            phrase.lastMisheardInput = null
        } else {
            txtWhatComputerHeard.text = "❌ You said: \"$rawInput\"\nThat’s not quite right."
            phrase?.failureCount = (phrase?.failureCount ?: 0) + 1

            if (phrase != null) {
                // Ignore if user said "no" previously
                if (phrase.skipSuggestionsFor.contains(rawInput)) return

                // Track misheard phrase
                if (phrase.lastMisheardInput == rawInput) {
                    phrase.recentFailures++
                } else {
                    phrase.lastMisheardInput = rawInput
                    phrase.recentFailures = 1
                }

                // Only after 4 identical mistakes
                if (phrase.recentFailures >= 4) {
                    showCorrectionDialog(rawInput, phrase.text, phrase)
                }
            }
        }
        savePhraseStatsToPrefs()
        updateSayThisUI()
    }


    private fun showCorrectionDialog(rawInput: String, intended: String, phrase: PracticePhrase) {
        AlertDialog.Builder(this)
            .setTitle("Having Trouble?")
            .setMessage("You’ve said \"$rawInput\" 4 times. Should we remember this as a shortcut for \"$intended\"?")
            .setPositiveButton("Yes") { _, _ ->
                val cleanedInput = rawInput.trim().lowercase()
                VoiceInputMapper.userVoiceMap[cleanedInput] = intended
                Toast.makeText(this, "Shortcut saved for \"$intended\"!", Toast.LENGTH_SHORT).show()
                phrase.recentFailures = 0
            }
            .setNegativeButton("No") { _, _ ->
                phrase.skipSuggestionsFor.add(rawInput)
                Toast.makeText(this, "No problem — we won’t ask again for \"$rawInput\".", Toast.LENGTH_SHORT).show()
                phrase.recentFailures = 0
            }
            .show()
    }



}// +++++++++++ END Training-Words ++++++++++++++++++++++++