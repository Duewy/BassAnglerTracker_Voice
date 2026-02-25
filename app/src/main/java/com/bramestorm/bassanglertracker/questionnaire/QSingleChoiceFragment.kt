package com.bramestorm.bassanglertracker.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bramestorm.bassanglertracker.R

/**
 * Generic single-choice step fragment.
 * Configure via [newInstance] with a question title, list of option labels,
 * list of option values (enum name strings), and the currently selected value.
 */
class QSingleChoiceFragment : Fragment() {

    private lateinit var tvQuestion: TextView
    private lateinit var radioGroup: RadioGroup

    private var selectedValue: String = ""
    private lateinit var optionValues: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_q_single_choice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvQuestion = view.findViewById(R.id.tvSingleChoiceQuestion)
        radioGroup = view.findViewById(R.id.radioGroupOptions)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val labels = arguments?.getStringArrayList(ARG_LABELS) ?: arrayListOf()
        optionValues = arguments?.getStringArrayList(ARG_VALUES) ?: arrayListOf()
        selectedValue = arguments?.getString(ARG_SELECTED, "") ?: ""

        tvQuestion.text = title
        radioGroup.removeAllViews()

        labels.forEachIndexed { index, label ->
            val rb = RadioButton(requireContext())
            rb.id = View.generateViewId()
            rb.text = label
            rb.textSize = 16f
            rb.setPadding(8, 12, 8, 12)
            rb.tag = optionValues.getOrNull(index) ?: ""
            radioGroup.addView(rb)
        }

        // Restore selection
        for (i in 0 until radioGroup.childCount) {
            val rb = radioGroup.getChildAt(i) as? RadioButton ?: continue
            if (rb.tag == selectedValue) {
                radioGroup.check(rb.id)
                break
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val rb = radioGroup.findViewById<RadioButton>(checkedId)
            selectedValue = rb?.tag as? String ?: ""
        }
    }

    fun isStepValid(): Boolean = selectedValue.isNotBlank()

    fun getSelectedValue(): String = selectedValue

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_LABELS = "arg_labels"
        private const val ARG_VALUES = "arg_values"
        private const val ARG_SELECTED = "arg_selected"

        fun newInstance(
            title: String,
            labels: List<String>,
            values: List<String>,
            selected: String
        ): QSingleChoiceFragment {
            val fragment = QSingleChoiceFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putStringArrayList(ARG_LABELS, ArrayList(labels))
                putStringArrayList(ARG_VALUES, ArrayList(values))
                putString(ARG_SELECTED, selected)
            }
            return fragment
        }
    }
}
