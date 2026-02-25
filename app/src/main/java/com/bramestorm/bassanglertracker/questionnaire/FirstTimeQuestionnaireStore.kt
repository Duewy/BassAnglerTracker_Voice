package com.bramestorm.bassanglertracker.questionnaire

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────────────────────────────────────
// FirstTimeQuestionnaireStore.kt
//
// Persists first-time questionnaire answers to SharedPreferences.
// Data is local only – no network calls.
//
// Mirrors iOS: FirstTimeQuestionnaireStore in firstTimeQuestionnaire.swift
// ─────────────────────────────────────────────────────────────────────────────

object FirstTimeQuestionnaireStore {

    private const val PREFS_NAME = "ftq_answers"
    private const val KEY_ANSWERS = "FirstTimeQuestionnaireAnswers.v1"

    private val gson = Gson()
    private val type = object : TypeToken<FirstTimeQuestionnaireAnswers>() {}.type

    fun load(context: Context): FirstTimeQuestionnaireAnswers? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ANSWERS, null) ?: return null
        return runCatching { gson.fromJson<FirstTimeQuestionnaireAnswers>(json, type) }
            .getOrNull()
    }

    fun save(context: Context, answers: FirstTimeQuestionnaireAnswers) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ANSWERS, gson.toJson(answers)).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ANSWERS).apply()
    }
}
