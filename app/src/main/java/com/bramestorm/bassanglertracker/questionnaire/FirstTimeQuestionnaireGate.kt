package com.bramestorm.bassanglertracker.questionnaire

import android.content.Context

// ─────────────────────────────────────────────────────────────────────────────
// FirstTimeQuestionnaireGate.kt
//
// Tracks whether the first-time questionnaire has been completed.
// Stored in SharedPreferences (local only, no network).
//
// Mirrors iOS: FirstTimeQuestionnaireGate in firstTimeQuestionnaire.swift
//
// ── Developer / QA reset ────────────────────────────────────────────────────
// To re-show the questionnaire during testing, call:
//     FirstTimeQuestionnaireGate.reset(context)
// This also clears saved answers and the advertising seed flag.
//
// Example in a debug menu option:
//     FirstTimeQuestionnaireGate.reset(this)
//     startActivity(Intent(this, FirstTimeQuestionnaireActivity::class.java))
// ─────────────────────────────────────────────────────────────────────────────

object FirstTimeQuestionnaireGate {

    private const val PREFS_NAME    = "ftq_gate"
    private const val KEY_COMPLETED = "FirstTimeQuestionnaireCompleted.v1"

    /** Returns true once the user has finished (or skipped) the questionnaire. */
    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    /** Called when the questionnaire is finished or skipped. */
    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    /**
     * Resets the gate so the questionnaire will appear on next launch.
     * Also clears stored answers and the advertising seed flag.
     * For developer / QA use only.
     */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_COMPLETED).apply()
        FirstTimeQuestionnaireStore.clear(context)
        AdvertisingSelectionStore.reset(context)
    }
}
