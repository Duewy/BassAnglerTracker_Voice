package com.bramestorm.bassanglertracker.voice

/**
 * Links the onWake and shutdown functions for both FunDayVoiceHandler.kt and
 * TournamentVoiceHandler.kt to interact via VoiceControlService.kt.
 */

interface VoiceSessionHandler {
    fun onWake()
    fun shutdown()
}