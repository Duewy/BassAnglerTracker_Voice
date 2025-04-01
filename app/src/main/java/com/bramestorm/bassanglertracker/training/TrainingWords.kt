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
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
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
import java.util.Locale


class TrainingWords : AppCompatActivity() {

    private lateinit var btnSetUpVCC: Button
    private lateinit var btnMenuVCC: Button
    private lateinit var btnUserTalk: Button
    private lateinit var txtWhatComputerHeard : TextView
    private lateinit var txtSayThis : TextView
    private val speechRecognizer by lazy { SpeechRecognizer.createSpeechRecognizer(this) }
    private lateinit var speechIntent: Intent
    private val phraseList: MutableList<PracticePhrase> = VoiceCommandList.phraseList
    private val SPEECH_REQUEST_CODE = 1001
    private val recordAudioRequestCode = 101
    private lateinit var textToSpeech: TextToSpeech



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_words)

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
            val prompt = if (phraseToSay.isNotEmpty()) {
                "Say: \"$phraseToSay\""
            } else {
                "Say a command or practice phrase"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)

            try {
                startActivityForResult(intent, SPEECH_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Your device doesn't support speech input", Toast.LENGTH_SHORT).show()
            }
        }



            // !!!!!!!!!!! Gets Words for User to SAY  !!!!!!!!!!!!!!!!!

        txtSayThis.setOnClickListener {
            showPhrasePopup()
        }


    }// ================== END On Create ===================================

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
        val listView = ListView(this)
        val adapter = PhraseListAdapter(this, phraseList)

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            // Clear previous selection
            phraseList.forEach { it.isMastered = false }

            // Set only the selected item as mastered
            phraseList[position].isMastered = true

            val selected = phraseList[position]
            txtSayThis.text = "Say This: ${selected.text}"
            adapter.notifyDataSetChanged()
            updateSayThisUI()
        }

        builder.setTitle("Practice Phrases")
        builder.setView(listView)
        builder.setPositiveButton("Done", null)
        builder.show()
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

    //???????????????? CHECK if WORDS MATCH ???????????????????????

    private fun checkPhraseMatch(spoken: String) {
        val currentPhrase = txtSayThis.text.toString().replace("Say This: ", "").trim()

        // Normalize both strings
        val normalizedSpoken = spoken.replace("\\s|-".toRegex(), "").lowercase()
        val normalizedTarget = currentPhrase.replace("\\s|-".toRegex(), "").lowercase()

        if (normalizedSpoken == normalizedTarget) {
            txtWhatComputerHeard.text = "You said: \"$spoken\"\n✔ That is a match!"
        } else {
            txtWhatComputerHeard.text = "You said: \"$spoken\"\n❌ That does not match \"$currentPhrase\""
        }
    }







    //_________________ Get DAta from Microphone _______________

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val resultList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = resultList?.firstOrNull()

            spokenText?.let {
                handleVoiceInput(it)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



    fun handleVoiceInput(rawInput: String) {
        val matchedSpecies = VoiceInputMapper.getSpeciesFromVoice(rawInput)

        if (matchedSpecies != null) {
            txtWhatComputerHeard.text = "You said: \"$rawInput\"\n✔ That is a match for \"$matchedSpecies\"!"
            // You could also set a spinner value or pass species to the next function here
        } else {
            txtWhatComputerHeard.text = "You said: \"$rawInput\"\n❌ Species not recognized."
        }
    }


}// +++++++++++ END Training-Words ++++++++++++++++++++++++