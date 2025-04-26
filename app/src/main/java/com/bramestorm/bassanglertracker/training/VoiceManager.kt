package com.bramestorm.bassanglertracker.training

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

object VoiceManager {
    private var recognizer: SpeechRecognizer? = null
    private var intent: Intent? = null

    fun init(context: Context) {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
        }
    }

    fun setListener(listener: RecognitionListener) {
        recognizer?.setRecognitionListener(listener)
    }

    fun start() {
        recognizer?.apply {
            stopListening()
            cancel()
            startListening(intent)
        }
    }

    fun stop() {
        recognizer?.apply {
            stopListening()
            cancel()
        }
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        intent = null
    }
}
