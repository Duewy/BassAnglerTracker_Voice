package com.bramestorm.bassanglertracker.questionnaire

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R

class FirstTimeQuestionnaireActivity : AppCompatActivity() {

    private var currentStep = 0
    private val totalSteps = 8

    private val answers = FirstTimeQuestionnaireAnswers()

    private lateinit var tvStepHeader: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: Button
    private lateinit var btnNext: Button

    private val stepTitles = listOf(
        "Where do you fish?",
        "What type of water?",
        "Why do you fish?",
        "How often do you fish?",
        "How do you fish?",
        "What techniques do you use?",
        "What species do you target?",
        "What gear interests you?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_time_questionnaire)

        val bottomBar = findViewById<View>(R.id.bottomBar)
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        tvStepHeader = findViewById(R.id.tvStepHeader)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnNext = findViewById(R.id.btnNext)

        progressBar.max = totalSteps

        btnBack.setOnClickListener { navigateBack() }
        btnNext.setOnClickListener { navigateNext() }

        if (savedInstanceState != null) {
            currentStep = savedInstanceState.getInt(KEY_STEP, 0)
            restoreAnswers(savedInstanceState)
        }

        showStep(currentStep)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        collectCurrentAnswers()
        outState.putInt(KEY_STEP, currentStep)
        saveAnswersToBundle(outState)
    }

    private fun showStep(step: Int) {
        currentStep = step
        tvStepHeader.text = stepTitles.getOrElse(step) { "Set Up Your Profile" }
        progressBar.progress = step + 1
        btnBack.text = if (step == 0) "Skip" else "Back"
        btnNext.text = if (step == totalSteps - 1) "Finish" else "Next"

        val fragment: Fragment = when (step) {
            0 -> QLocationFragment.newInstance(answers.countryCode, answers.regionCode)
            1 -> QSingleChoiceFragment.newInstance(
                title = "What type of water do you fish?",
                labels = WaterType.entries.map { it.label },
                values = WaterType.entries.map { it.name },
                selected = answers.waterType?.name ?: ""
            )
            2 -> QSingleChoiceFragment.newInstance(
                title = "Why do you fish?",
                labels = Purpose.entries.map { it.label },
                values = Purpose.entries.map { it.name },
                selected = answers.purpose?.name ?: ""
            )
            3 -> QSingleChoiceFragment.newInstance(
                title = "How often do you go fishing?",
                labels = Frequency.entries.map { it.label },
                values = Frequency.entries.map { it.name },
                selected = answers.frequency?.name ?: ""
            )
            4 -> QMultiChoiceFragment.newInstance(
                title = "What platforms do you fish from?",
                labels = FishingPlatform.entries.map { it.label },
                values = FishingPlatform.entries.map { it.name },
                selected = answers.platforms.map { it.name }.toSet()
            )
            5 -> QMultiChoiceFragment.newInstance(
                title = "What techniques do you use?",
                labels = FishingTechnique.entries.map { it.label },
                values = FishingTechnique.entries.map { it.name },
                selected = answers.techniques.map { it.name }.toSet()
            )
            6 -> QMultiChoiceFragment.newInstance(
                title = "What species do you target?",
                labels = SpeciesGroup.entries.map { it.label },
                values = SpeciesGroup.entries.map { it.name },
                selected = answers.speciesGroups.map { it.name }.toSet()
            )
            7 -> QMultiChoiceFragment.newInstance(
                title = "What gear interests you?",
                labels = GearInterest.entries.map { it.label },
                values = GearInterest.entries.map { it.name },
                selected = answers.gearInterests.map { it.name }.toSet()
            )
            else -> return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, FRAGMENT_TAG)
            .commit()
    }

    private fun navigateNext() {
        if (!validateCurrentStep()) return
        collectCurrentAnswers()

        if (currentStep == totalSteps - 1) {
            finishQuestionnaire()
        } else {
            showStep(currentStep + 1)
        }
    }

    private fun navigateBack() {
        if (currentStep == 0) {
            // "Skip" — complete questionnaire with empty answers
            finishQuestionnaire()
        } else {
            collectCurrentAnswers()
            showStep(currentStep - 1)
        }
    }

    private fun validateCurrentStep(): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) ?: return true
        return when (fragment) {
            is QLocationFragment -> {
                if (!fragment.isStepValid()) {
                    tvStepHeader.text = "⚠ Please select a country first"
                    false
                } else true
            }
            is QSingleChoiceFragment -> {
                if (!fragment.isStepValid()) {
                    tvStepHeader.text = "⚠ Please make a selection"
                    false
                } else true
            }
            else -> true
        }
    }

    private fun collectCurrentAnswers() {
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) ?: return
        when (fragment) {
            is QLocationFragment -> {
                answers.countryCode = fragment.getCountryCode()
                answers.regionCode = fragment.getRegionCode()
            }
            is QSingleChoiceFragment -> {
                val value = fragment.getSelectedValue()
                when (currentStep) {
                    1 -> answers.waterType = runCatching { WaterType.valueOf(value) }.getOrNull()
                    2 -> answers.purpose = runCatching { Purpose.valueOf(value) }.getOrNull()
                    3 -> answers.frequency = runCatching { Frequency.valueOf(value) }.getOrNull()
                }
            }
            is QMultiChoiceFragment -> {
                val values = fragment.getSelectedValues()
                when (currentStep) {
                    4 -> answers.platforms = values.mapNotNull { runCatching { FishingPlatform.valueOf(it) }.getOrNull() }.toSet()
                    5 -> answers.techniques = values.mapNotNull { runCatching { FishingTechnique.valueOf(it) }.getOrNull() }.toSet()
                    6 -> answers.speciesGroups = values.mapNotNull { runCatching { SpeciesGroup.valueOf(it) }.getOrNull() }.toSet()
                    7 -> answers.gearInterests = values.mapNotNull { runCatching { GearInterest.valueOf(it) }.getOrNull() }.toSet()
                }
            }
        }
    }

    private fun finishQuestionnaire() {
        collectCurrentAnswers()
        FirstTimeQuestionnaireStore.save(this, answers)
        FirstTimeQuestionnaireGate.markCompleted(this)
        AdvertisingSelectionStore.seedIfNeeded(this, answers)
        getSharedPreferences("BassAnglerTrackerPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("SKIP_DAILY_AD_ON_NEXT_MAIN", true)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun saveAnswersToBundle(bundle: Bundle) {
        bundle.putString("ans_country", answers.countryCode)
        bundle.putString("ans_region", answers.regionCode)
        bundle.putString("ans_waterType", answers.waterType?.name)
        bundle.putString("ans_purpose", answers.purpose?.name)
        bundle.putString("ans_frequency", answers.frequency?.name)
        bundle.putStringArrayList("ans_platforms", ArrayList(answers.platforms.map { it.name }))
        bundle.putStringArrayList("ans_techniques", ArrayList(answers.techniques.map { it.name }))
        bundle.putStringArrayList("ans_speciesGroups", ArrayList(answers.speciesGroups.map { it.name }))
        bundle.putStringArrayList("ans_gearInterests", ArrayList(answers.gearInterests.map { it.name }))
    }

    private fun restoreAnswers(bundle: Bundle) {
        answers.countryCode = bundle.getString("ans_country", "")
        answers.regionCode = bundle.getString("ans_region", "")
        answers.waterType = bundle.getString("ans_waterType")?.let { runCatching { WaterType.valueOf(it) }.getOrNull() }
        answers.purpose = bundle.getString("ans_purpose")?.let { runCatching { Purpose.valueOf(it) }.getOrNull() }
        answers.frequency = bundle.getString("ans_frequency")?.let { runCatching { Frequency.valueOf(it) }.getOrNull() }
        answers.platforms = (bundle.getStringArrayList("ans_platforms") ?: emptyList<String>())
            .mapNotNull { runCatching { FishingPlatform.valueOf(it) }.getOrNull() }.toSet()
        answers.techniques = (bundle.getStringArrayList("ans_techniques") ?: emptyList<String>())
            .mapNotNull { runCatching { FishingTechnique.valueOf(it) }.getOrNull() }.toSet()
        answers.speciesGroups = (bundle.getStringArrayList("ans_speciesGroups") ?: emptyList<String>())
            .mapNotNull { runCatching { SpeciesGroup.valueOf(it) }.getOrNull() }.toSet()
        answers.gearInterests = (bundle.getStringArrayList("ans_gearInterests") ?: emptyList<String>())
            .mapNotNull { runCatching { GearInterest.valueOf(it) }.getOrNull() }.toSet()
    }

    override fun onBackPressed() {
        if (currentStep > 0) {
            collectCurrentAnswers()
            showStep(currentStep - 1)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        private const val KEY_STEP = "key_current_step"
        private const val FRAGMENT_TAG = "questionnaire_step"
    }
}
