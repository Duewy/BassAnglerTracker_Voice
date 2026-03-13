package com.bramestorm.bassanglertracker.training

// TrainingWords.kt
// - Handles voice training phrases
// - Also runs voice-to-species mapping using VoiceInputMapper
// - Uses SpeechRecognizer for phrase comparison and species recognition
// - Bluetooth test: uses SpeechRecognizer + MODE_IN_COMMUNICATION (same path as real VCC)


import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import com.bramestorm.bassanglertracker.utils.BluetoothUtils
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import java.util.Locale


class TrainingWords : AppCompatActivity() {

    companion object {
        private const val TAG = "TrainingWords"
        private const val REQ_BT_CONNECT = 201
    }

    private lateinit var btnSetUpVCC: Button
    private lateinit var btnMenuVCC: Button
    private lateinit var btnUserTalk: Button
    private lateinit var btnTestBluetooth: Button
    private lateinit var txtWhatComputerHeard: TextView
    private lateinit var txtSayThis: TextView
    private lateinit var speechIntent: Intent
    private val phraseList: MutableList<PracticePhrase> = VoiceCommandList.phraseList
    private val speechRequestCode = 1001
    private val recordAudioRequestCode = 101
    private lateinit var textToSpeech: TextToSpeech
    private var selectedPhrase: PracticePhrase? = null
    private lateinit var userVoiceMap: MutableMap<String, String>

    // ── Bluetooth test fields ──
    private var btRecognizer: SpeechRecognizer? = null
    private var btTts: TextToSpeech? = null
    private var isBtListening = false


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
        btnTestBluetooth = findViewById(R.id.btnTestBluetooth)
        txtWhatComputerHeard = findViewById(R.id.txtWhatComputerHeard)

        btnSetUpVCC.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        btnMenuVCC.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        //~~~~~~~~~~ Opens the Phone Mic for User (Google popup) ~~~~~~~~~~~~~~~~~~~~
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
                @Suppress("DEPRECATION")
                startActivityForResult(intent, speechRequestCode)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Your device doesn't support speech input", Toast.LENGTH_SHORT).show()
            }
        }

        //~~~~~~~~~~ 🎧 Bluetooth Test Button ~~~~~~~~~~~~~~~~~~~~
        btnTestBluetooth.setOnClickListener {
            val phraseToSay = txtSayThis.text.toString().replace("Say This: ", "").trim()
            val isValidPhrase = phraseList.any { it.text.equals(phraseToSay, ignoreCase = true) }

            if (!isValidPhrase) {
                Toast.makeText(this, "Please select a word to practice first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!BluetoothUtils.isHeadsetConnected(this)) {
                Toast.makeText(this, "⚠️ No Bluetooth headset connected.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startBluetoothTest(phraseToSay)
        }


        // !!!!!!!!!!! Gets Words for User to SAY  !!!!!!!!!!!!!!!!!

        txtSayThis.setOnClickListener {
            showPhrasePopup()
        }

        // ── Initial BT button state ──
        updateBluetoothButton()

    }// ================== END On Create ===================================

    override fun onResume() {
        super.onResume()
        updateBluetoothButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveUserVoiceMap(this, userVoiceMap)
        shutdownBluetoothTest()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }


    // ═══════════════════════════════════════════════════════════
    // 🎧 BLUETOOTH TEST — uses the REAL VCC audio path
    // ═══════════════════════════════════════════════════════════

    /**
     * Updates the BT button text with the connected device name,
     * or disables it if no headset is connected.
     */
    private fun updateBluetoothButton() {
        // Check BT permission first (Android S+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            btnTestBluetooth.text = getString(R.string.no_bluetooth_device)
            btnTestBluetooth.isEnabled = false
            btnTestBluetooth.alpha = 0.4f
            return
        }

        val deviceName = BluetoothUtils.getConnectedHeadsetName(this)

        if (deviceName != null) {
            btnTestBluetooth.text = getString(R.string.test_with_bt, deviceName)
            btnTestBluetooth.isEnabled = true
            btnTestBluetooth.alpha = 1.0f
            Log.d(TAG, "🎧 BT device found: $deviceName")
        } else {
            btnTestBluetooth.text = getString(R.string.no_bluetooth_device)
            btnTestBluetooth.isEnabled = false
            btnTestBluetooth.alpha = 0.4f
            Log.d(TAG, "🎧 No BT headset connected")
        }
    }

    /**
     * Runs the Bluetooth test:
     * 1. Set audio mode to MODE_IN_COMMUNICATION (routes to BT SCO)
     * 2. TTS speaks "Say: [phrase]" through BT headset
     * 3. When TTS finishes, STT listens through BT mic
     * 4. Result displayed on screen + compared to expected phrase
     */
    private fun startBluetoothTest(phraseToSay: String) {
        Log.d(TAG, "🎧 Starting Bluetooth test for: '$phraseToSay'")
        txtWhatComputerHeard.text = "🎧 Listening via Bluetooth..."

        // ── 1. Route audio to Bluetooth ──
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        // ── 2. Create the SpeechRecognizer (same as VoiceInteractionManager) ──
        btRecognizer?.destroy()
        btRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        btRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "🎤 BT STT ready for speech")
            }

            override fun onResults(results: Bundle?) {
                isBtListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                Log.d(TAG, "🎧 BT STT result: '$spoken'")

                // Reset audio mode
                audioManager.mode = AudioManager.MODE_NORMAL

                // Process the result the same way as phone mic
                handleVoiceInput(spoken)
            }

            override fun onError(error: Int) {
                isBtListening = false
                Log.e(TAG, "🎧 BT STT error: $error")
                audioManager.mode = AudioManager.MODE_NORMAL

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected through Bluetooth"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Bluetooth mic timed out"
                    SpeechRecognizer.ERROR_AUDIO -> "Bluetooth audio error"
                    else -> "Bluetooth test error ($error)"
                }
                txtWhatComputerHeard.text = "❌ $errorMsg"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // ── 3. TTS speaks the prompt through BT, then STT listens ──
        btTts?.stop()
        btTts?.shutdown()
        btTts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                btTts!!.language = Locale.US

                btTts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "🗣️ BT TTS started")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "🗣️ BT TTS finished — starting BT STT")
                        // Small delay for BT audio handoff, then listen
                        Handler(Looper.getMainLooper()).postDelayed({
                            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L)
                                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                            }
                            btRecognizer?.startListening(recognizerIntent)
                            isBtListening = true
                        }, 800)
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "🗣️ BT TTS error")
                        Handler(Looper.getMainLooper()).post {
                            txtWhatComputerHeard.text = "❌ Bluetooth TTS failed"
                            audioManager.mode = AudioManager.MODE_NORMAL
                        }
                    }
                })

                val spokenPrompt = "Say: $phraseToSay"
                btTts!!.speak(spokenPrompt, TextToSpeech.QUEUE_FLUSH, null, "BT_TEST_PROMPT")
            } else {
                Log.e(TAG, "🗣️ BT TTS init failed")
                Handler(Looper.getMainLooper()).post {
                    txtWhatComputerHeard.text = "❌ TTS initialization failed"
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        }
    }

    private fun shutdownBluetoothTest() {
        btRecognizer?.destroy()
        btRecognizer = null
        btTts?.stop()
        btTts?.shutdown()
        btTts = null
        isBtListening = false

        // Restore audio mode
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }


    // ═══════════════════════════════════════════════════════════
    // 📊 Stats persistence
    // ═══════════════════════════════════════════════════════════

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


    // ═══════════════════════════════════════════════════════════
    // 🎤 Permissions
    // ═══════════════════════════════════════════════════════════

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            println("Permission is granted")
        } else {
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
        when (requestCode) {
            recordAudioRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("Audio permission was granted")
                } else {
                    println("Audio permission was denied")
                }
            }
            REQ_BT_CONNECT -> {
                updateBluetoothButton()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 📋 Phrase popup
    // ═══════════════════════════════════════════════════════════

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


    //_________________ Get Data from Phone Microphone (Google popup) _______________

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


    /**
     * Shared handler for both phone mic AND Bluetooth test results.
     * Compares spoken text against the selected phrase.
     */
    private fun handleVoiceInput(rawInput: String) {
        val normalizedInput = rawInput.lowercase().replace(" ", "").trim()
        val currentPhraseTextRaw = txtSayThis.text.toString().replace("Say This: ", "").trim()
        val currentPhraseText = currentPhraseTextRaw.lowercase().replace(" ", "")
        val speciesList = SharedPreferencesManager.loadSpeciesList(this)
        val matchedSpecies = VoiceInputMapper.getSpeciesFromVoice(rawInput, speciesList)
        val matchedNormalized = matchedSpecies.lowercase().replace(" ", "")

        val phrase = phraseList.find {
            it.text.lowercase().replace(" ", "") == currentPhraseText
        }

        if (matchedNormalized == currentPhraseText && phrase != null) {
            txtWhatComputerHeard.text = "✔ You said: \"$rawInput\" — That's a match for \"${phrase.text}\"!"
            phrase.successCount++
            phrase.recentFailures = 0
            phrase.lastMisheardInput = null
        } else {
            txtWhatComputerHeard.text = "❌ You said: \"$rawInput\"\nThat's not quite right."
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
            .setMessage("You've said \"$rawInput\" 4 times. Should we remember this as a shortcut for \"$intended\"?")
            .setPositiveButton("Yes") { _, _ ->
                val cleanedInput = rawInput.trim().lowercase()
                VoiceInputMapper.userVoiceMap[cleanedInput] = intended
                Toast.makeText(this, "Shortcut saved for \"$intended\"!", Toast.LENGTH_SHORT).show()
                phrase.recentFailures = 0
            }
            .setNegativeButton("No") { _, _ ->
                phrase.skipSuggestionsFor.add(rawInput)
                Toast.makeText(this, "No problem — we won't ask again for \"$rawInput\".", Toast.LENGTH_SHORT).show()
                phrase.recentFailures = 0
            }
            .show()
    }



}// +++++++++++ END Training-Words ++++++++++++++++++++++++