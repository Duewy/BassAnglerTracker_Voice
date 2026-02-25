package com.bramestorm.bassanglertracker.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bramestorm.bassanglertracker.R

/**
 * Generic multi-choice step fragment.
 * Configure via [newInstance] with a question title, list of option labels,
 * list of option values (enum name strings), and the currently selected values.
 */
class QMultiChoiceFragment : Fragment() {

    private lateinit var tvQuestion: TextView
    private lateinit var checkboxContainer: LinearLayout
    private var cbAllOfAbove: CheckBox? = null

    private val selectedValues = mutableSetOf<String>()
    private lateinit var optionValues: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_q_multi_choice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvQuestion = view.findViewById(R.id.tvMultiChoiceQuestion)
        checkboxContainer = view.findViewById(R.id.checkboxContainer)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val labels = arguments?.getStringArrayList(ARG_LABELS) ?: arrayListOf()
        optionValues = arguments?.getStringArrayList(ARG_VALUES) ?: arrayListOf()
        val preSelected = arguments?.getStringArrayList(ARG_SELECTED) ?: arrayListOf()

        selectedValues.clear()
        selectedValues.addAll(preSelected)

        tvQuestion.text = title
        checkboxContainer.removeAllViews()

        // Individual options
        labels.forEachIndexed { index, label ->
            val value = optionValues.getOrNull(index) ?: return@forEachIndexed
            val cb = CheckBox(requireContext())
            cb.text = label
            cb.textSize = 16f
            cb.setPadding(8, 10, 8, 10)
            cb.isChecked = selectedValues.contains(value)
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedValues.add(value) else selectedValues.remove(value)
                syncAllOfAbove()
            }
            cb.tag = value
            checkboxContainer.addView(cb)
        }

        // Divider
        val divider = View(requireContext())
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2
        ).also { it.setMargins(0, 8, 0, 8) }
        divider.setBackgroundColor(0xFFCCCCCC.toInt())
        checkboxContainer.addView(divider)

        // "All of the Above" checkbox
        val cbAll = CheckBox(requireContext())
        cbAll.text = "All of the Above"
        cbAll.textSize = 16f
        cbAll.setTypeface(cbAll.typeface, android.graphics.Typeface.BOLD)
        cbAll.setPadding(8, 10, 8, 10)
        cbAll.isChecked = selectedValues.size == optionValues.size && optionValues.isNotEmpty()
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedValues.addAll(optionValues)
            } else {
                selectedValues.clear()
            }
            setIndividualCheckboxes(isChecked)
        }
        cbAllOfAbove = cbAll
        checkboxContainer.addView(cbAll)
    }

    private fun syncAllOfAbove() {
        val cbAll = cbAllOfAbove ?: return
        cbAll.setOnCheckedChangeListener(null)
        cbAll.isChecked = selectedValues.size == optionValues.size && optionValues.isNotEmpty()
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedValues.addAll(optionValues) else selectedValues.clear()
            setIndividualCheckboxes(isChecked)
        }
    }

    private fun setIndividualCheckboxes(checked: Boolean) {
        for (i in 0 until checkboxContainer.childCount) {
            val child = checkboxContainer.getChildAt(i)
            if (child is CheckBox && child !== cbAllOfAbove) {
                child.isChecked = checked
            }
        }
    }

    // Multi-choice steps are optional — user can skip
    fun isStepValid(): Boolean = true

    fun getSelectedValues(): Set<String> = selectedValues.toSet()

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_LABELS = "arg_labels"
        private const val ARG_VALUES = "arg_values"
        private const val ARG_SELECTED = "arg_selected"

        fun newInstance(
            title: String,
            labels: List<String>,
            values: List<String>,
            selected: Set<String>
        ): QMultiChoiceFragment {
            val fragment = QMultiChoiceFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putStringArrayList(ARG_LABELS, ArrayList(labels))
                putStringArrayList(ARG_VALUES, ArrayList(values))
                putStringArrayList(ARG_SELECTED, ArrayList(selected))
            }
            return fragment
        }
    }
}
