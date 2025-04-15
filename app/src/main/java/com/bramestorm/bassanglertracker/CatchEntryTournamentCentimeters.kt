package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale.getDefault


class CatchEntryTournamentCentimeters : AppCompatActivity() {


        // Buttons
        private lateinit var btnTournamentCatch:Button
        private lateinit var btnMainCms: Button
        private lateinit var btnAlarmCms: Button
        private lateinit var btnSetUpCms: Button


        // Alarm Variables
        private var alarmHour: Int = -1
        private var alarmMinute: Int = -1
        private var alarmTriggered: Boolean = false

        private var mediaPlayer: MediaPlayer? = null

        // Weight Display TextViews
        private lateinit var firstRealLengthCms: TextView
        private lateinit var secondRealLengthCms: TextView
        private lateinit var thirdRealLengthCms: TextView
        private lateinit var fourthRealLengthCms: TextView
        private lateinit var fifthRealLengthCms: TextView
        private lateinit var sixthRealLengthCms: TextView

        private lateinit var firstDecLengthCms: TextView
        private lateinit var secondDecLengthCms: TextView
        private lateinit var thirdDecLengthCms: TextView
        private lateinit var fourthDecLengthCms: TextView
        private lateinit var fifthDecLengthCms: TextView
        private lateinit var sixthDecLengthCms: TextView


        private lateinit var txtTypeLetter1:TextView
        private lateinit var txtTypeLetter2:TextView
        private lateinit var txtTypeLetter3:TextView
        private lateinit var txtTypeLetter4:TextView
        private lateinit var txtTypeLetter5:TextView
        private lateinit var txtTypeLetter6:TextView

        private lateinit var txtColorLetter1:TextView
        private lateinit var txtColorLetter2:TextView
        private lateinit var txtColorLetter3:TextView
        private lateinit var txtColorLetter4:TextView
        private lateinit var txtColorLetter5:TextView
        private lateinit var txtColorLetter6:TextView


        private lateinit var totalRealLengthCms: TextView
        private lateinit var totalDecLengthCms: TextView

        private lateinit var txtGPSNotice: TextView

        private var availableClipColors: List<ClipColor> = emptyList()
        private val flashHandler = Handler(Looper.getMainLooper())


        // Database Helper
        private lateinit var dbHelper: CatchDatabaseHelper

        // Tournament Configuration
        private var tournamentCatchLimit: Int = 4
        private var measurementSystem: String = "weight"
        private var isCullingEnabled: Boolean = false
        private var typeOfMarkers: String = "Color"
        private var tournamentSpecies: String = "Unknown"
        private var lastTournamentCatch: CatchItem? = null

        // Request Codes
        private val requestAlarmSET = 1009


        // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
        private val lengthEntryLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val totalLengthTenths = data?.getIntExtra("lengthTotalCms", 0) ?: 0
                val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
                val clipColor = data?.getStringExtra("clip_color") ?: ""

                Log.d("DB_DEBUG", "✅ Received totalLengthTenths: $totalLengthTenths, selectedSpecies: $selectedSpecies, clip_color: $clipColor")

                if (totalLengthTenths > 0) {
                    saveTournamentCatch(totalLengthTenths, selectedSpecies, clipColor)
                }
            }
        }

        //================ ON CREATE =======================================
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_tournament_view_centimeters)

            dbHelper = CatchDatabaseHelper(this)
            btnTournamentCatch = findViewById(R.id.btnStartFishingCms)
            btnMainCms = findViewById(R.id.btnMainCms)
            btnSetUpCms = findViewById(R.id.btnSetUpCms)
            btnAlarmCms = findViewById(R.id.btnAlarmCms)
            txtGPSNotice = findViewById(R.id.txtGPSNotice)

            // Assign TextViews
            firstRealLengthCms = findViewById(R.id.firstRealLengthCms)
            secondRealLengthCms = findViewById(R.id.secondRealLengthCms)
            thirdRealLengthCms = findViewById(R.id.thirdRealLengthCms)
            fourthRealLengthCms = findViewById(R.id.fourthRealLengthCms)
            fifthRealLengthCms = findViewById(R.id.fifthRealLengthCms)
            sixthRealLengthCms = findViewById(R.id.sixthRealLengthCms)

            firstDecLengthCms = findViewById(R.id.firstDecLengthCms)
            secondDecLengthCms = findViewById(R.id.secondDecLengthCms)
            thirdDecLengthCms = findViewById(R.id.thirdDecLengthCms)
            fourthDecLengthCms = findViewById(R.id.fourthDecLengthCms)
            fifthDecLengthCms = findViewById(R.id.fifthDecLengthCms)
            sixthDecLengthCms = findViewById(R.id.sixthDecLengthCms)

            txtTypeLetter1 = findViewById(R.id.txtTypeLetterCms1)
            txtTypeLetter2 = findViewById(R.id.txtTypeLetterCms2)
            txtTypeLetter3 = findViewById(R.id.txtTypeLetterCms3)
            txtTypeLetter4 = findViewById(R.id.txtTypeLetterCms4)
            txtTypeLetter5 = findViewById(R.id.txtTypeLetterCms5)
            txtTypeLetter6 = findViewById(R.id.txtTypeLetterCms6)

            totalRealLengthCms = findViewById(R.id.totalRealLengthCms)
            totalDecLengthCms = findViewById(R.id.totalDecLengthCms)

            txtTypeLetter1 = findViewById(R.id.txtTypeLetterCms1)
            txtTypeLetter2 =findViewById(R.id.txtTypeLetterCms2)
            txtTypeLetter3 = findViewById(R.id.txtTypeLetterCms3)
            txtTypeLetter4= findViewById(R.id.txtTypeLetterCms4)
            txtTypeLetter5= findViewById(R.id.txtTypeLetterCms5)
            txtTypeLetter6= findViewById(R.id.txtTypeLetterCms6)

            txtColorLetter1 = findViewById(R.id.txtCmsColorLetter1)
            txtColorLetter2 = findViewById(R.id.txtCmsColorLetter2)
            txtColorLetter3 = findViewById(R.id.txtCmsColorLetter3)
            txtColorLetter4 = findViewById(R.id.txtCmsColorLetter4)
            txtColorLetter5 = findViewById(R.id.txtCmsColorLetter5)
            txtColorLetter6 = findViewById(R.id.txtCmsColorLetter6)



            tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
            typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
            tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
            measurementSystem = intent.getStringExtra("unitType") ?: "weight"
            isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

            btnTournamentCatch.setOnClickListener { showWeightPopup() }
            btnSetUpCms.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
            btnMainCms.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
            btnAlarmCms.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }
            val dbHelper = CatchDatabaseHelper(this)

            GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

            updateTournamentList()
        }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // ------------- On RESUME --------- Check GPS  Statues --------------
        override fun onResume() {
            super.onResume()
            GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
        }

      // +++++++++++++ On-Destroy +++++++++++++++++++

        override fun onDestroy() {
            super.onDestroy()

            // Clean up all handlers and media players
            flashHandler.removeCallbacksAndMessages(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

        private fun showWeightPopup() {
            val intent = Intent(this, PopupLengthEntryTourCms::class.java)
            intent.putExtra("isTournament", true)

            if (tournamentSpecies.equals("Large Mouth", true) || tournamentSpecies.equals("Largemouth", true))  {
                intent.putExtra("tournamentSpecies", "Large Mouth Bass")
            } else         if (tournamentSpecies.equals("Small Mouth", true) || tournamentSpecies.equals("Smallmouth", true))  {
                intent.putExtra("tournamentSpecies", "Small Mouth Bass")
            } else{
                intent.putExtra("tournamentSpecies", tournamentSpecies)
            }

            // 🔥 Send available clip colors as String array
            val colorNames = availableClipColors.map { it.name }.toTypedArray()
            intent.putExtra("availableClipColors", colorNames)

            lengthEntryLauncher.launch(intent)
        }

        // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
        private fun saveTournamentCatch(totalLengthTenths: Int, bassType: String, clipColor: String) {
            val availableColors = calculateAvailableClipColors(
                dbHelper,
                catchType = "metric",
                date = getCurrentDate(),
                tournamentCatchLimit = tournamentCatchLimit,
                isCullingEnabled = isCullingEnabled
            )
            val cleanClipColor = clipColor.uppercase() // This came from the popup

            val speciesInitial = if (bassType == "Large Mouth") "L" else "S"

            Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

            val catch = CatchItem(
                id = 0,
                dateTime = getCurrentDateTime(),
                species = bassType,
                totalWeightOz = null,
                totalLengthA8th = null,
                totalWeightHundredthKg = null,
                totalLengthTenths = totalLengthTenths,
                catchType = "metric",
                markerType = speciesInitial,
                clipColor = cleanClipColor
            )
            val result = dbHelper.insertCatch(catch)
            Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
            // ✅ Save the most recent catch for motivational messaging
            if (result) {
                lastTournamentCatch = catch
            }
            updateTournamentList()
        }


        // ``````````````` UPDATE TOTAL LENGTH ``````````````````````

        private fun updateTotalLength(tournamentCatches: List<CatchItem>) {
            // Always sort and limit to top N
            val catchesToUse = tournamentCatches
                .sortedByDescending { it.totalLengthTenths ?: 0 }
                .take(tournamentCatchLimit)  // ✅ Apply limit always

            val totalLengthCms = catchesToUse.sumOf { it.totalLengthTenths ?: 0 }
            val totalCms = totalLengthCms / 10
            val totalDec = totalLengthCms % 10

            totalRealLengthCms.text = totalCms.toString()
            totalDecLengthCms.text = totalDec.toString()

// !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
            val currentCount = dbHelper
                .getCatchesForToday("metric", getCurrentDate())
                .sortedByDescending { it.totalLengthTenths ?: 0 }
                .take(tournamentCatchLimit)
                .size

            if (currentCount >= 2) {
                lastTournamentCatch?.let {
                    val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "cms")
                    if (message != null) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }



        //################## UPDATE TOURNAMENT LIST   ###################################
        private fun updateTournamentList() {
            val formattedDate = getCurrentDate()

            val realLengthCms = listOf(
                firstRealLengthCms, secondRealLengthCms, thirdRealLengthCms,
                fourthRealLengthCms, fifthRealLengthCms, sixthRealLengthCms
            )

            val decLengthCms = listOf(
                firstDecLengthCms, secondDecLengthCms, thirdDecLengthCms,
                fourthDecLengthCms, fifthDecLengthCms, sixthDecLengthCms
            )

            val colorLetters = listOf(
                txtColorLetter1, txtColorLetter2, txtColorLetter3,
                txtColorLetter4, txtColorLetter5, txtColorLetter6
            )

            val typeLetters = listOf(
                txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
                txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
            )

            val allCatches = dbHelper.getCatchesForToday(catchType = "metric", formattedDate)
            val sortedCatches = allCatches.sortedByDescending { it.totalLengthTenths ?: 0 }

            val tournamentCatches = if (isCullingEnabled) {
                sortedCatches.take(tournamentCatchLimit)
            } else {
                sortedCatches
            }

            availableClipColors = calculateAvailableClipColors(
                dbHelper,
                catchType = "metric",
                date = formattedDate,
                tournamentCatchLimit = tournamentCatchLimit,
                isCullingEnabled = isCullingEnabled
            )

            Log.d("CLIP_COLOR", "🎨 Available Colors: $availableClipColors")

            clearTournamentTextViews()

            runOnUiThread {
                val loopLimit = minOf(sortedCatches.size, 6) // up to 6 total slots

                for (i in 0 until loopLimit) {
                    if (i >= realLengthCms.size) continue

                    val catch = sortedCatches[i]
                    val totalLengthCms = catch.totalLengthTenths ?: 0
                    val lengthCms = totalLengthCms / 10
                    val lengthDec = totalLengthCms % 10

                    val clipColor = try {
                        ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                    } catch (_: Exception) {
                        ClipColor.RED
                    }

                    realLengthCms[i].text = lengthCms.toString()
                    decLengthCms[i].text = lengthDec.toString()

                    val baseColor = ContextCompat.getColor(this, clipColor.resId)
                    val layeredDrawable = createLayeredDrawable(baseColor)
                    realLengthCms[i].background = layeredDrawable
                    decLengthCms[i].background = layeredDrawable

                    val textColor = if (clipColor == ClipColor.BLUE)
                        resources.getColor(R.color.clip_white, theme)
                    else
                        resources.getColor(R.color.black, theme)

                    realLengthCms[i].setTextColor(textColor)
                    decLengthCms[i].setTextColor(textColor)

                    realLengthCms[i].invalidate()
                    decLengthCms[i].invalidate()

                    // Clip color label
                    colorLetters[i].text = when (clipColor.name) {
                        "BLUE" -> "B"
                        "RED" -> "R"
                        "GREEN" -> "G"
                        "YELLOW" -> "Y"
                        "ORANGE" -> "O"
                        "WHITE" -> "W"
                        else -> "?"
                    }

                    // Species label
                    typeLetters[i].text = getSpeciesCode(catch.species ?: "")
                }

                updateTotalLength(tournamentCatches)
                adjustTextViewVisibility()

                if (tournamentCatches.size >= tournamentCatchLimit) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        when (tournamentCatchLimit) {
                            4 -> {
                                blinkTextViewTwice(fourthRealLengthCms)
                                blinkTextViewTwice(fourthDecLengthCms)
                            }
                            5 -> {
                                blinkTextViewTwice(fifthRealLengthCms)
                                blinkTextViewTwice(fifthDecLengthCms)
                            }
                            6 -> {
                                blinkTextViewTwice(sixthRealLengthCms)
                                blinkTextViewTwice(sixthDecLengthCms)
                            }
                        }
                    }, 300)
                }
            }
        }



        //########### Clear Tournament Text Views  ########################

        private fun clearTournamentTextViews() {
            firstRealLengthCms.text = ""
            secondRealLengthCms.text = ""
            thirdRealLengthCms.text = ""
            fourthRealLengthCms.text = ""
            fifthRealLengthCms.text = ""
            sixthRealLengthCms.text = ""

            firstDecLengthCms.text = ""
            secondDecLengthCms.text = ""
            thirdDecLengthCms.text = ""
            fourthDecLengthCms.text = ""
            fifthDecLengthCms.text = ""
            sixthDecLengthCms.text = ""

            totalRealLengthCms.text = "0"
            totalDecLengthCms.text = "0"
        }

        // %%%%%%%%%%%% Clip Color assignment  %%%%%%%%%%%%%%%%%%%%%%%

        enum class ClipColor(val resId: Int) {
            RED(R.color.clip_red),
            BLUE(R.color.clip_blue),
            GREEN(R.color.clip_green),
            YELLOW(R.color.clip_yellow),
            ORANGE(R.color.clip_orange),
            WHITE(R.color.clip_white);
        }

        //????????????? AVAILABLE COLORS   ???????????????????????
        private fun calculateAvailableClipColors(
            dbHelper: CatchDatabaseHelper,
            catchType: String,
            date: String,
            tournamentCatchLimit: Int,
            isCullingEnabled: Boolean
        ): List<ClipColor> {
            val allCatches = dbHelper.getCatchesForToday(catchType, date)
            val sorted = allCatches.sortedByDescending { it.totalLengthTenths ?: 0 }
            val topCatches = sorted.take(tournamentCatchLimit) // ✅ Always limit to top N

            val usedColors = topCatches.mapNotNull { it.clipColor }
                .mapNotNull {
                    try { ClipColor.valueOf(it.uppercase()) } catch (_: Exception) { null }
                }
                .toSet()

            return ClipColor.entries.filter { it !in usedColors }
        }



        // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for culling values ~~~~~~~~~~~~~
        private fun adjustTextViewVisibility() {
            when (tournamentCatchLimit) {
                4 -> {
                    fifthRealLengthCms.alpha = 0.3f
                    fifthDecLengthCms.alpha = 0.3f
                    fifthRealLengthCms.isEnabled = false
                    fifthDecLengthCms.isEnabled = false
                    sixthRealLengthCms.visibility = View.INVISIBLE
                    sixthDecLengthCms.visibility = View.INVISIBLE
                }
                5 -> {
                    sixthRealLengthCms.alpha = 0.3f
                    sixthDecLengthCms.alpha = 0.3f
                    sixthRealLengthCms.isEnabled = false
                    sixthDecLengthCms.isEnabled = false
                    txtTypeLetter6.isEnabled = false
                }
                else -> {
                    fifthRealLengthCms.alpha = 1.0f
                    fifthDecLengthCms.alpha = 1.0f
                    fifthRealLengthCms.isEnabled = true
                    fifthDecLengthCms.isEnabled = true
                    sixthRealLengthCms.visibility = View.VISIBLE
                    sixthDecLengthCms.visibility = View.VISIBLE
                    sixthRealLengthCms.alpha = 1.0f
                    sixthDecLengthCms.alpha = 1.0f
                    sixthRealLengthCms.isEnabled = true
                    sixthDecLengthCms.isEnabled = true
                }
            }
        }

        //!!!!!!!!!!!!!!!! Get SPECIES Letter !!!!!!!!!!!!!!!!!

        private fun getSpeciesCode(species: String): String {
            return when (species.uppercase()) {
                "LARGE MOUTH" -> "LM"
                "SMALL MOUTH" -> "SM"
                "WALLEYE"     -> "WE"
                "PIKE"        -> "PK"
                "PERCH"       -> "PH"
                "PANFISH"     -> "PF"
                "CATFISH"     -> "CF"
                "CRAPPIE"     -> "CP"
                else          -> "--"
            }
        }



   //@@@@@@@@@@@@ Alarm Triggering @@@@@@@@@@@@@@@

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestAlarmSET && resultCode == Activity.RESULT_OK) {
            alarmHour = data?.getIntExtra("ALARM_HOUR", -1) ?: -1
            alarmMinute = data?.getIntExtra("ALARM_MINUTE", -1) ?: -1
            alarmTriggered = false

            if (alarmHour != -1 && alarmMinute != -1) {
                val amPm = if (alarmHour >= 12) "PM" else "AM"
                val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
                val formattedMinute = String.format(getDefault(), "%02d", alarmMinute)
                val timeString = "$displayHour:$formattedMinute $amPm"

                btnAlarmCms.text = getString(R.string.alarm_set_to, timeString)
                val toastMessage = getString(R.string.alarm_toast_message, timeString)
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarmHour)
                    set(Calendar.MINUTE, alarmMinute)
                    set(Calendar.SECOND, 0)
                }

                val alarmIntent = Intent(this, com.bramestorm.bassanglertracker.alarm.AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    System.currentTimeMillis().toInt(), // unique ID
                    alarmIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                // ✅ First: Check exact alarm permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "⚠️ Please enable exact alarm permission in settings.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                    return  // ❌ Stop here — don't schedule if not allowed
                }

                // ✅ Then try to schedule
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("ALARM_DEBUG", "✅ Alarm scheduled for ${calendar.time}")
                } catch (e: SecurityException) {
                    Log.e("ALARM_DEBUG", "❌ Cannot schedule exact alarm. SecurityException.", e)
                    Toast.makeText(this, "Exact alarm could not be scheduled (permissions)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    //++++++++++++++++ Date and Time  +++++++++++++++++++++++++++++
        private fun getCurrentDateTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getDefault())
            return sdf.format(Date())
        }

        //************** DATE *****************************
        private fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", getDefault())
            return sdf.format(Date())
        }

        // @@@@@@@@@@@@@ BLINK Smallest Catch  @@@@@@@@@@@@@@@@@@@@@@@@@

        private fun blinkTextViewTwice(textView: TextView) {
            val blink = AnimationUtils.loadAnimation(this, R.anim.blink)

            // Delay 1 second before first blink
            Handler(Looper.getMainLooper()).postDelayed({
                textView.startAnimation(blink)

                // Delay slightly before doing the second blink
                Handler(Looper.getMainLooper()).postDelayed({
                    textView.startAnimation(blink)
                }, 700) // Wait ~1 blink duration
            }, 1000) // Initial 1 second delay
        }

// +++++++++++++++ Boarder Around Clip Colors  ++++++++++++++++++++++++

    private fun createLayeredDrawable(baseColor: Int): Drawable {
        val colorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4f
            setColor(baseColor)
        }

        val borderDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4f
            setStroke(4, Color.BLACK) // 4dp border
            setColor(Color.TRANSPARENT) // Don't cover the base
        }

        return LayerDrawable(arrayOf(colorDrawable, borderDrawable))
    }


    }//################## END  ################################