package com.bramestorm.bassanglertracker.questionnaire

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────────────────────────────────────
// FirstTimeQuestionnaireActivity.kt
//
// One-question-at-a-time onboarding wizard shown on first launch only.
//
// Architecture:
//  • Single Activity, no Fragments needed.
//  • Step state + answers saved in onSaveInstanceState → rotation / process-death safe.
//  • Answers stored locally in SharedPreferences via FirstTimeQuestionnaireStore.
//  • Gate marked complete via FirstTimeQuestionnaireGate.
//  • On finish, AdvertisingSelectionStore.seedIfNeeded seeds ad profile (once).
//
// Developer reset (re-shows questionnaire on next launch):
//     FirstTimeQuestionnaireGate.reset(context)
//
// Mirrors iOS: firstTimeQuestionnaire.swift + RootLaunchView.swift
// ─────────────────────────────────────────────────────────────────────────────

class FirstTimeQuestionnaireActivity : AppCompatActivity() {

    // ── Steps ──────────────────────────────────────────────────────────────────
    private enum class Step(val title: String) {
        INTRO("Welcome"),
        LOCATION("Location"),
        WATER_TYPE("Water Type"),
        PLATFORM("Fishing Platform"),
        TECHNIQUE("Fishing Style"),
        PURPOSE("Purpose"),
        SPECIES("Target Species"),
        GEAR("Gear Interests"),
        FREQUENCY("Usage Frequency"),
        COMPLETION("All Set")
    }

    // ── State ──────────────────────────────────────────────────────────────────
    private val steps      = Step.values()
    private var stepIndex  = 0
    private lateinit var answers: FirstTimeQuestionnaireAnswers
    private val gson       = Gson()
    private val answersType = object : TypeToken<FirstTimeQuestionnaireAnswers>() {}.type

    // ── Views ──────────────────────────────────────────────────────────────────
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle:     TextView
    private lateinit var content:     LinearLayout
    private lateinit var btnBack:     android.widget.Button
    private lateinit var btnNext:     android.widget.Button
    private lateinit var btnSkip:     android.widget.Button

    companion object {
        private const val KEY_STEP    = "ftq_step"
        private const val KEY_ANSWERS = "ftq_answers"
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftq)

        // Restore state after rotation or process death
        if (savedInstanceState != null) {
            stepIndex = savedInstanceState.getInt(KEY_STEP, 0)
            answers   = savedInstanceState.getString(KEY_ANSWERS)
                ?.let { runCatching { gson.fromJson<FirstTimeQuestionnaireAnswers>(it, answersType) }.getOrNull() }
                ?: FirstTimeQuestionnaireAnswers()
        } else {
            answers = FirstTimeQuestionnaireStore.load(this) ?: FirstTimeQuestionnaireAnswers()
        }

        progressBar = findViewById(R.id.ftq_progress)
        tvTitle     = findViewById(R.id.ftq_title)
        content     = findViewById(R.id.ftq_content)
        btnBack     = findViewById(R.id.ftq_btn_back)
        btnNext     = findViewById(R.id.ftq_btn_next)
        btnSkip     = findViewById(R.id.ftq_btn_skip)

        btnBack.setOnClickListener { goBack() }
        btnNext.setOnClickListener { goNext() }
        btnSkip.setOnClickListener { completeAndExit(completed = false) }

        // Back-press navigates within steps rather than exiting immediately
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (stepIndex > 0) goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })

        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP, stepIndex)
        outState.putString(KEY_ANSWERS, gson.toJson(answers))
    }

    // ── Navigation ─────────────────────────────────────────────────────────────
    private fun goNext() {
        if (stepIndex < steps.lastIndex) { stepIndex++; render() }
        else completeAndExit(completed = true)
    }

    private fun goBack() {
        if (stepIndex > 0) { stepIndex--; render() }
    }

    /** Saves answers, marks gate completed, optionally seeds ad profile, then goes to MainActivity. */
    private fun completeAndExit(completed: Boolean) {
        FirstTimeQuestionnaireStore.save(this, answers)
        FirstTimeQuestionnaireGate.markCompleted(this)
        if (completed) AdvertisingSelectionStore.seedIfNeeded(this, answers)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ── Step rendering ─────────────────────────────────────────────────────────
    private fun render() {
        progressBar.progress =
            if (steps.lastIndex == 0) 100 else stepIndex * 100 / steps.lastIndex

        tvTitle.text = steps[stepIndex].title

        btnBack.visibility = if (stepIndex == 0) View.GONE else View.VISIBLE
        btnNext.text       = if (stepIndex == steps.lastIndex) "Finish" else "Next"
        btnSkip.visibility = if (stepIndex == steps.lastIndex) View.GONE else View.VISIBLE

        content.removeAllViews()
        when (steps[stepIndex]) {
            Step.INTRO      -> buildIntro()
            Step.LOCATION   -> buildLocation()
            Step.WATER_TYPE -> buildWaterType()
            Step.PLATFORM   -> buildPlatform()
            Step.TECHNIQUE  -> buildTechnique()
            Step.PURPOSE    -> buildPurpose()
            Step.SPECIES    -> buildSpecies()
            Step.GEAR       -> buildGear()
            Step.FREQUENCY  -> buildFrequency()
            Step.COMPLETION -> buildCompletion()
        }
    }

    // ── Step builders ──────────────────────────────────────────────────────────

    private fun buildIntro() {
        card {
            heading("Purpose")
            body("This optional questionnaire helps tailor Catch and Call features and in-app deals to your fishing style.")
            divider()
            heading("Privacy Notice")
            body("All answers are stored locally on your device. Nothing is uploaded or shared.")
        }
    }

    private fun buildLocation() {
        card {
            heading("Country")

            val countrySpinner = Spinner(this@FirstTimeQuestionnaireActivity).apply {
                layoutParams = lp(bottomMargin = dp(8))
            }
            val countryEntries = listOf("Select…") + LocationData.countries.map { it.name }
            countrySpinner.adapter = simpleAdapter(countryEntries)
            val selCountryIdx = LocationData.countries
                .indexOfFirst { it.id == answers.countryCode }
                .let { if (it < 0) 0 else it + 1 }
            countrySpinner.setSelection(selCountryIdx)
            addView(countrySpinner)

            // Region row – shown only when selected country has regions
            val regionLabel = TextView(this@FirstTimeQuestionnaireActivity).apply {
                text = "State / Province (optional)"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
                layoutParams = lp(topMargin = dp(8), bottomMargin = dp(4))
                visibility = View.GONE
            }
            val regionSpinner = Spinner(this@FirstTimeQuestionnaireActivity).apply {
                layoutParams = lp(bottomMargin = dp(8))
                visibility = View.GONE
            }
            addView(regionLabel)
            addView(regionSpinner)

            fun refreshRegions(countryId: String) {
                val country = LocationData.forId(countryId)
                if (country != null && country.regions.isNotEmpty()) {
                    regionLabel.visibility  = View.VISIBLE
                    regionSpinner.visibility = View.VISIBLE
                    val names = listOf("None") + country.regions.map { it.name }
                    regionSpinner.adapter = simpleAdapter(names)
                    val rIdx = country.regions.indexOfFirst { it.id == answers.regionCode }
                        .let { if (it < 0) 0 else it + 1 }
                    regionSpinner.setSelection(rIdx)
                    regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                            answers.regionCode = if (pos == 0) "" else country.regions[pos - 1].id
                        }
                        override fun onNothingSelected(p: AdapterView<*>?) {}
                    }
                } else {
                    regionLabel.visibility   = View.GONE
                    regionSpinner.visibility = View.GONE
                    answers.regionCode = ""
                }
            }

            // Initialise region state for whatever country is already stored
            refreshRegions(answers.countryCode)

            countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    answers.countryCode = if (pos == 0) "" else LocationData.countries[pos - 1].id
                    answers.regionCode  = ""
                    refreshRegions(answers.countryCode)
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }

            divider()
            heading("Note")
            body("Location helps determine available species groups and fishing environments. You can fish freshwater, saltwater, or both regardless of location.")
        }
    }

    private fun buildWaterType() {
        singleChoice(
            prompt  = "Which water types do you fish?",
            options = listOf(
                "Freshwater" to (answers.waterType == FirstTimeQuestionnaireAnswers.WaterType.FRESHWATER),
                "Saltwater"  to (answers.waterType == FirstTimeQuestionnaireAnswers.WaterType.SALTWATER),
                "Both"       to (answers.waterType == FirstTimeQuestionnaireAnswers.WaterType.BOTH)
            )
        ) { idx ->
            answers.waterType = FirstTimeQuestionnaireAnswers.WaterType.values()[idx]
        }
    }

    private fun buildPlatform() {
        multiChoice(
            prompt    = "How do you usually fish? (Select all that apply)",
            options   = FishingPlatform.values().map { it.label },
            isChecked = { FishingPlatform.values()[it] in answers.platforms }
        ) { idx, checked ->
            val v = FishingPlatform.values()[idx]
            if (checked) answers.platforms.add(v) else answers.platforms.remove(v)
        }
    }

    private fun buildTechnique() {
        multiChoice(
            prompt    = "Which styles apply to you? (Optional)",
            options   = FishingTechnique.values().map { it.label },
            isChecked = { FishingTechnique.values()[it] in answers.techniques }
        ) { idx, checked ->
            val v = FishingTechnique.values()[idx]
            if (checked) answers.techniques.add(v) else answers.techniques.remove(v)
        }
    }

    private fun buildPurpose() {
        singleChoice(
            prompt  = "Why do you primarily use Catch and Call?",
            options = listOf(
                "Fun / Personal Logging"    to (answers.purpose == FirstTimeQuestionnaireAnswers.Purpose.FUN),
                "Competition / Tournaments" to (answers.purpose == FirstTimeQuestionnaireAnswers.Purpose.COMPETITION),
                "Both"                      to (answers.purpose == FirstTimeQuestionnaireAnswers.Purpose.BOTH)
            )
        ) { idx ->
            answers.purpose = FirstTimeQuestionnaireAnswers.Purpose.values()[idx]
        }
    }

    private fun buildSpecies() {
        multiChoice(
            prompt    = "Select the species groups you care about most.",
            options   = SpeciesGroup.values().map { it.label },
            isChecked = { SpeciesGroup.values()[it] in answers.speciesGroups }
        ) { idx, checked ->
            val v = SpeciesGroup.values()[idx]
            if (checked) answers.speciesGroups.add(v) else answers.speciesGroups.remove(v)
        }
    }

    private fun buildGear() {
        multiChoice(
            prompt    = "What gear are you most interested in?\n(Select as many as you like)",
            options   = GearInterest.values().map { it.label },
            isChecked = { GearInterest.values()[it] in answers.gearInterests }
        ) { idx, checked ->
            val v = GearInterest.values()[idx]
            if (checked) answers.gearInterests.add(v) else answers.gearInterests.remove(v)
        }
    }

    private fun buildFrequency() {
        singleChoice(
            prompt  = "How often do you fish?",
            options = listOf(
                "A few times a year"               to (answers.frequency == FirstTimeQuestionnaireAnswers.Frequency.FEW_PER_YEAR),
                "Monthly"                          to (answers.frequency == FirstTimeQuestionnaireAnswers.Frequency.MONTHLY),
                "Weekly"                           to (answers.frequency == FirstTimeQuestionnaireAnswers.Frequency.WEEKLY),
                "Very frequent / Tournament level" to (answers.frequency == FirstTimeQuestionnaireAnswers.Frequency.VERY_FREQUENT)
            )
        ) { idx ->
            answers.frequency = FirstTimeQuestionnaireAnswers.Frequency.values()[idx]
        }
    }

    private fun buildCompletion() {
        card {
            heading("You're all set! 🎣")
            body("Your preferences have been saved. Catch and Call is ready to go.")
            divider()
            heading("Your Profile Summary")

            val countryName = LocationData.forId(answers.countryCode)?.name
                ?: answers.countryCode.ifEmpty { "Not specified" }
            body("Country: $countryName")

            val regionName = LocationData.forId(answers.countryCode)?.regions
                ?.firstOrNull { it.id == answers.regionCode }?.name
                ?: answers.regionCode.ifEmpty { null }
            if (regionName != null) body("Region: $regionName")

            answers.waterType?.let { body("Water type: ${it.displayLabel}") }
            answers.purpose?.let   { body("Purpose: ${it.displayLabel}") }
            answers.frequency?.let { body("Frequency: ${it.displayLabel}") }

            if (answers.platforms.isNotEmpty())
                body("Platforms: ${answers.platforms.joinToString { it.label }}")
            if (answers.speciesGroups.isNotEmpty())
                body("Species: ${answers.speciesGroups.joinToString { it.label }}")
        }
    }

    // ── Card DSL helpers ───────────────────────────────────────────────────────

    /** Creates a styled card container and runs [block] inside it. */
    private inline fun card(block: LinearLayout.() -> Unit) {
        val c = makeCard()
        c.block()
        content.addView(c)
    }

    private fun LinearLayout.heading(text: String): TextView {
        return TextView(this@FirstTimeQuestionnaireActivity).also {
            it.text      = text
            it.textSize  = 16f
            it.setTypeface(null, Typeface.BOLD)
            it.setTextColor(Color.BLACK)
            it.layoutParams = lp(topMargin = dp(4), bottomMargin = dp(4))
            addView(it)
        }
    }

    private fun LinearLayout.body(text: String): TextView {
        return TextView(this@FirstTimeQuestionnaireActivity).also {
            it.text      = text
            it.textSize  = 14f
            it.setTextColor(0xFF333333.toInt())
            it.layoutParams = lp(topMargin = dp(2), bottomMargin = dp(2))
            addView(it)
        }
    }

    private fun LinearLayout.divider() {
        addView(View(this@FirstTimeQuestionnaireActivity).apply {
            layoutParams = lp(height = dp(1), topMargin = dp(8), bottomMargin = dp(8))
            setBackgroundColor(Color.LTGRAY)
        })
    }

    // ── Reusable question cards ────────────────────────────────────────────────

    /**
     * Builds a single-choice (radio) question card.
     * [options] pairs are (label, isCurrentlySelected).
     * [onSelect] receives the 0-based index of the chosen option.
     */
    private fun singleChoice(
        prompt:   String,
        options:  List<Pair<String, Boolean>>,
        onSelect: (Int) -> Unit
    ) {
        card {
            heading(prompt)
            val rg = RadioGroup(this@FirstTimeQuestionnaireActivity).apply {
                orientation = RadioGroup.VERTICAL
                layoutParams = lp()
            }
            options.forEachIndexed { idx, (label, selected) ->
                rg.addView(RadioButton(this@FirstTimeQuestionnaireActivity).apply {
                    text        = label
                    textSize    = 14f
                    setTextColor(Color.BLACK)
                    id          = idx + 1          // IDs must be > 0
                    isChecked   = selected
                    layoutParams = RadioGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                })
            }
            rg.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId > 0) onSelect(checkedId - 1)
            }
            addView(rg)
        }
    }

    /**
     * Builds a multi-choice (checkbox) question card with an "All of the Above" toggle.
     * [isChecked] returns whether the option at the given index is currently selected.
     * [onToggle] is called with the index and new checked state for each individual change.
     */
    private fun multiChoice(
        prompt:    String,
        options:   List<String>,
        isChecked: (Int) -> Boolean,
        onToggle:  (Int, Boolean) -> Unit
    ) {
        card {
            heading(prompt)

            val checkBoxes = mutableListOf<CheckBox>()
            var guard = false

            // "All of the Above" – initialised after individual checkboxes so its
            // initial checked state reflects the current selection.
            val allCb = CheckBox(this@FirstTimeQuestionnaireActivity).apply {
                text     = "All of the Above"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
                layoutParams = lp(topMargin = dp(4))
            }

            // Individual checkboxes
            options.forEachIndexed { idx, label ->
                val cb = CheckBox(this@FirstTimeQuestionnaireActivity).apply {
                    this.text      = label
                    textSize       = 14f
                    setTextColor(Color.BLACK)
                    this.isChecked = isChecked(idx)
                    layoutParams   = lp()
                }
                cb.setOnCheckedChangeListener { _, checked ->
                    if (!guard) {
                        onToggle(idx, checked)
                        // Keep "All" checkbox in sync
                        guard = true
                        allCb.isChecked = checkBoxes.all { it.isChecked }
                        guard = false
                    }
                }
                checkBoxes.add(cb)
                addView(cb)
            }

            divider()

            // Wire "All of the Above" after individual boxes are ready
            allCb.isChecked = checkBoxes.all { it.isChecked }
            // Only respond to direct user taps on "All of the Above", not
            // programmatic changes from individual checkbox listeners.
            allCb.setOnCheckedChangeListener { _, checked ->
                if (!guard) {
                    guard = true
                    checkBoxes.forEachIndexed { idx, cb ->
                        cb.isChecked = checked
                        onToggle(idx, checked)
                    }
                    guard = false
                }
            }
            addView(allCb)
        }
    }

    // ── Layout / style helpers ─────────────────────────────────────────────────

    private fun makeCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        }
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundResource(R.drawable.ftq_card_background)
    }

    private fun lp(
        width:        Int = MATCH_PARENT,
        height:       Int = WRAP_CONTENT,
        topMargin:    Int = 0,
        bottomMargin: Int = 0,
        startMargin:  Int = 0,
        endMargin:    Int = 0
    ) = LinearLayout.LayoutParams(width, height).apply {
        this.topMargin    = topMargin
        this.bottomMargin = bottomMargin
        this.marginStart  = startMargin
        this.marginEnd    = endMargin
    }

    /** Converts dp to pixels. */
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun simpleAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
}

// ── Display label helpers (file-private extension properties) ──────────────────

private val FirstTimeQuestionnaireAnswers.WaterType.displayLabel
    get() = when (this) {
        FirstTimeQuestionnaireAnswers.WaterType.FRESHWATER -> "Freshwater"
        FirstTimeQuestionnaireAnswers.WaterType.SALTWATER  -> "Saltwater"
        FirstTimeQuestionnaireAnswers.WaterType.BOTH       -> "Both"
    }

private val FirstTimeQuestionnaireAnswers.Purpose.displayLabel
    get() = when (this) {
        FirstTimeQuestionnaireAnswers.Purpose.FUN         -> "Fun / Personal"
        FirstTimeQuestionnaireAnswers.Purpose.COMPETITION -> "Competition / Tournaments"
        FirstTimeQuestionnaireAnswers.Purpose.BOTH        -> "Both"
    }

private val FirstTimeQuestionnaireAnswers.Frequency.displayLabel
    get() = when (this) {
        FirstTimeQuestionnaireAnswers.Frequency.FEW_PER_YEAR  -> "A few times a year"
        FirstTimeQuestionnaireAnswers.Frequency.MONTHLY       -> "Monthly"
        FirstTimeQuestionnaireAnswers.Frequency.WEEKLY        -> "Weekly"
        FirstTimeQuestionnaireAnswers.Frequency.VERY_FREQUENT -> "Very frequent"
    }
