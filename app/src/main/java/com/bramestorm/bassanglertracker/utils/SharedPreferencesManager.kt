package com.bramestorm.bassanglertracker.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bramestorm.bassanglertracker.MeasurementMode
import com.bramestorm.bassanglertracker.alarm.AlarmReceiver
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPreferencesManager {

    // Preferences files
    private const val KEY_VCC_DOZE_AGREEMENT = "USER_AGREED_TO_VCC_DOZE"
    private const val SPECIES_PREFS = "SpeciesPrefs"
    private const val APP_PREFS = "BassAnglerTrackerPrefs"

    // Species keys
    private const val KEY_ALL_SPECIES_LIST = "ALL_SPECIES_LIST"

    // Catch entry type
    private const val KEY_CATCH_TYPE = "catchEntryType"

    // Voice & tournament keys
    private const val KEY_VOICE_CONTROL_ENABLED = "VOICE_CONTROL_ENABLED"
    private const val KEY_NUMBER_OF_CATCHES = "NUMBER_OF_CATCHES"
    private const val KEY_TOURNAMENT_SPECIES = "TOURNAMENT_SPECIES"
    private const val KEY_CULLING_ENABLED = "CULLING_ENABLED"


    // ALARM values for VCC to Use
    private const val KEY_ALARM_HOUR = "ALARM_HOUR"
    private const val KEY_ALARM_MINUTE = "ALARM_MINUTE"

    private const val TAG = "SharedPreferencesManager"

        // Ensures the App Voice Services all shut down when app is closed
    fun Context.cleanupAppServices() {
        Log.d("AppCleanup", "🧹 Cleaning up background services")
        stopService(Intent(this, VoiceControlService::class.java))

        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        (getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pendingIntent)
    }


    // === VOICE CONTROL ===
    fun setVccEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VOICE_CONTROL_ENABLED, enabled).apply()
    }

    fun isVccEnabled(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VOICE_CONTROL_ENABLED, false)
    }

    fun setUserAgreedToDeepDoze(context: Context, agreed: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VCC_DOZE_AGREEMENT, agreed).apply()
    }

    fun hasUserAgreedToDeepDoze(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VCC_DOZE_AGREEMENT, false)
    }

    // === CatchEntry Type ===
    fun saveCatchEntryType(context: Context, type: Int) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CATCH_TYPE, type).apply()
    }

    fun getCatchEntryType(context: Context): Int {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CATCH_TYPE, -1)
    }

  //=============== MEASUREMENT MODE #'s ===========================
    fun getFunDayUnit(context: Context): MeasurementMode {
        val type = getCatchEntryType(context)
        return when (type) {
            1 -> MeasurementMode.LBS_OZ
            2 -> MeasurementMode.POUNDS
            3 -> MeasurementMode.KG
            4 -> MeasurementMode.INCHES
            5 -> MeasurementMode.CM

            else -> MeasurementMode.LBS_OZ
        }
    }

    fun getTournamentUnit(context: Context): MeasurementMode {
        val type = getCatchEntryType(context)
        return when (type) {
            6   -> MeasurementMode.LBS_OZ
            7   -> MeasurementMode.POUNDS
            8   -> MeasurementMode.KG
            9   -> MeasurementMode.INCHES
            10  -> MeasurementMode.CM
            else -> MeasurementMode.LBS_OZ
        }
    }


    // === Tournament Settings 🏆 ===
    fun setNumberOfCatches(context: Context, number: Int) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_NUMBER_OF_CATCHES, number).apply()
    }

    fun getNumberOfCatches(context: Context): Int {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_NUMBER_OF_CATCHES, 5)
    }

    fun setTournamentSpecies(context: Context, species: String) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOURNAMENT_SPECIES, species).apply()
    }

    fun getTournamentSpecies(context: Context): String? {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOURNAMENT_SPECIES, null)
    }

    fun setCullingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CULLING_ENABLED, enabled).apply()
    }

    fun isCullingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CULLING_ENABLED, false)
    }


    // === Species Handling SYSTEM ===
    // -------- There is a set list on the FishSpecies that has Icons from SpeciesImageHelper -------

    fun initializeDefaultSpeciesIfNeeded(context: Context) {
        val prefs = getSpeciesPrefs(context)
        val gson = Gson()
        if (!prefs.contains(KEY_ALL_SPECIES_LIST)) {
            val defaultSpecies = FishSpecies.allSpeciesList
            prefs.edit().putString(KEY_ALL_SPECIES_LIST, gson.toJson(defaultSpecies)).apply()
        }
    }

    fun getSpeciesCatalogue(context: Context): List<String> {
        val saved = getAllSavedSpecies(context)
        return if (saved.isNotEmpty()) {
            saved
        } else {
            FishSpecies.allSpeciesList
        }
    }

    fun saveSpeciesCatalogue(context: Context, speciesList: List<String>) {
        val cleaned = speciesList
            .map { normalizeSpeciesName(it) }
            .filter { it.isNotBlank() }
            .distinct()

        getSpeciesPrefs(context)
            .edit()
            .putString(KEY_ALL_SPECIES_LIST, Gson().toJson(cleaned))
            .apply()

        Log.d(TAG, "Saved species catalogue: $cleaned")
    }


    fun removeUserSpecies(context: Context, speciesName: String) {
        val normalized = normalizeSpeciesName(speciesName)
        val all = getAllSavedSpecies(context).toMutableList()
        all.removeAll { normalizeSpeciesName(it) == normalized }
        saveSpeciesCatalogue(context, all)

        val updated = getSpeciesCatalogue(context).toMutableList()
        updated.removeAll { normalizeSpeciesName(it) == normalized }
        saveSpeciesCatalogue(context, updated)
    }

    private fun getAllSavedSpecies(context: Context): List<String> {
        val json = getSpeciesPrefs(context).getString(KEY_ALL_SPECIES_LIST, null)
        return if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        else emptyList()
    }

    fun updateUserSpeciesName(context: Context, oldName: String, newName: String) {
        val normalizedOld = normalizeSpeciesName(oldName)
        val normalizedNew = normalizeSpeciesName(newName)
        if (normalizedNew.isBlank()) return

        val all = getAllSavedSpecies(context).toMutableList()
        val idxAll = all.indexOfFirst { normalizeSpeciesName(it) == normalizedOld }
        if (idxAll >= 0) all[idxAll] = normalizedNew
        saveSpeciesCatalogue(context, all)

        val sel = getSpeciesCatalogue(context).toMutableList()
        val idxSel = sel.indexOfFirst { normalizeSpeciesName(it) == normalizedOld }
        if (idxSel >= 0) sel[idxSel] = normalizedNew
        saveSpeciesCatalogue(context, sel)

        Log.d(TAG, "Updated species from '$oldName' to '$normalizedNew'.")
    }

   // fun getUserAddedSpeciesList(context: Context): List<String> {
   //     val saved = getAllSavedSpecies(context).map { normalizeSpeciesName(it) }
   //     val default = FishSpecies.allSpeciesList.map { normalizeSpeciesName(it) }
   //     return saved.filterNot { it in default }
  //  }

    private fun getSpeciesPrefs(context: Context) =
        context.getSharedPreferences(SPECIES_PREFS, Context.MODE_PRIVATE)

    fun isSpeciesInitialized(context: Context): Boolean {
        return getSpeciesPrefs(context).getBoolean("SPECIES_INITIALIZED", false)
    }

    fun setSpeciesInitialized(context: Context, initialized: Boolean) {
        getSpeciesPrefs(context).edit().putBoolean("SPECIES_INITIALIZED", initialized).apply()
    }

    fun normalizeSpeciesName(name: String): String =
        name.trim().lowercase().replace(Regex("\\s+"), " ")


    //==== ADVERTISEMENT SECTION saveSpeciesCatalogue 📰 ======================

    fun logAdCloseTime(context: Context, adSource: String, durationMs: Long) {
        val prefs = context.getSharedPreferences("AdStatsPrefs", Context.MODE_PRIVATE)
        val key = "duration_${adSource}_${System.currentTimeMillis()}"
        prefs.edit().putLong(key, durationMs).apply()
        Log.d("AdTracker", "⏱️ $adSource ad closed after ${durationMs / 1000.0} sec")
    }

    fun logAdImpression(context: Context, adSource: String) {
        val prefs = context.getSharedPreferences("AdStatsPrefs", Context.MODE_PRIVATE)
        val key = "impressions_$adSource"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
        Log.d("AdTracker", "📊 Impression logged for $adSource → Total: ${count + 1}")
    }

    fun getAdImpressionCount(context: Context, adSource: String): Int {
        val prefs = context.getSharedPreferences("AdStatsPrefs", Context.MODE_PRIVATE)
        return prefs.getInt("impressions_$adSource", 0)
    }


}//===== END =======
