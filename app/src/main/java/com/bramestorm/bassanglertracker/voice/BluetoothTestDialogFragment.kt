package com.bramestorm.bassanglertracker.voice

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bramestorm.bassanglertracker.R
import java.util.Locale

class BluetoothTestDialogFragment : DialogFragment() {
    private lateinit var tts: TextToSpeech
    private lateinit var recognizer: SpeechRecognizer

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.popup_bluetooth_test, null)

        val tvPrompt      = view.findViewById<TextView>(R.id.tvPrompt)
        val btnStart      = view.findViewById<Button>(R.id.btnStartTest)
        val tvUserSpoken  = view.findViewById<TextView>(R.id.tvUserSpoken)
        val btnFinish     = view.findViewById<Button>(R.id.btnFinish)

        // init TTS
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        // init Recognizer
        recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object: RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?: "—"
                    tvUserSpoken.text = "I heard: \"$spoken\""
                    tvUserSpoken.visibility = View.VISIBLE

                    // speak back
                    tts.speak(
                        "I heard you say $spoken. Everything sounds good. Have a great day fishing!",
                        TextToSpeech.QUEUE_FLUSH, null, "feedback"
                    )
                    btnFinish.visibility = View.VISIBLE
                }
                // other callbacks no-op...
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    tvUserSpoken.text = "Recognition error $error"
                    tvUserSpoken.visibility = View.VISIBLE
                    btnFinish.visibility = View.VISIBLE
                }
                override fun onPartialResults(p0: Bundle?) {}
                override fun onEvent(p0: Int, p1: Bundle?) {}
            })
        }

        btnStart.setOnClickListener {
            // route audio over SCO
            val audioMgr = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioMgr.startBluetoothSco()

            // play TTS prompt
            tts.speak(
                "Please say: The speaker is working properly",
                TextToSpeech.QUEUE_FLUSH, null, "prompt"
            )

            // start listening after a brief delay
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer.startListening(intent)
            }, 2000)
        }

        btnFinish.setOnClickListener { dismiss() }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        recognizer.destroy()
    }
}
