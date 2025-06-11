package com.bramestorm.bassanglertracker.voice

interface VoiceUiHelper {
    fun speak(text: String)
    fun speak(text: String, utteranceId: String)
    fun showToast(message: String)
}
