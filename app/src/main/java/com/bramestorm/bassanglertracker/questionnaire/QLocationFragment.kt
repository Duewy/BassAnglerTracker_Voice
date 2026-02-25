package com.bramestorm.bassanglertracker.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bramestorm.bassanglertracker.R

class QLocationFragment : Fragment() {

    private lateinit var spinnerCountry: Spinner
    private lateinit var spinnerRegion: Spinner
    private lateinit var tvRegionLabel: TextView

    private var selectedCountryCode: String = ""
    private var selectedRegionCode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_q_location, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerCountry = view.findViewById(R.id.spinnerCountry)
        spinnerRegion = view.findViewById(R.id.spinnerRegion)
        tvRegionLabel = view.findViewById(R.id.tvRegionLabel)

        // Restore saved values from arguments
        selectedCountryCode = arguments?.getString(ARG_COUNTRY, "") ?: ""
        selectedRegionCode = arguments?.getString(ARG_REGION, "") ?: ""

        setupCountrySpinner()
    }

    private fun setupCountrySpinner() {
        val countries = LocationData.countries
        val labels = listOf("Select country…") + countries.map { it.name }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountry.adapter = adapter

        // Restore selection
        val restoreIdx = countries.indexOfFirst { it.code == selectedCountryCode }
        if (restoreIdx >= 0) spinnerCountry.setSelection(restoreIdx + 1)

        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (pos == 0) {
                    selectedCountryCode = ""
                    selectedRegionCode = ""
                    hideRegionSpinner()
                } else {
                    val country = countries[pos - 1]
                    selectedCountryCode = country.code
                    selectedRegionCode = ""
                    if (country.regions.isNotEmpty()) {
                        setupRegionSpinner(country.code)
                    } else {
                        hideRegionSpinner()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRegionSpinner(countryCode: String) {
        val regions = LocationData.regionsFor(countryCode)
        tvRegionLabel.visibility = View.VISIBLE
        spinnerRegion.visibility = View.VISIBLE

        val labels = listOf("Select state/province…") + regions.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRegion.adapter = adapter

        // Restore selection
        val restoreIdx = regions.indexOfFirst { it.code == selectedRegionCode }
        if (restoreIdx >= 0) spinnerRegion.setSelection(restoreIdx + 1)

        spinnerRegion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedRegionCode = if (pos == 0) "" else regions[pos - 1].code
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun hideRegionSpinner() {
        tvRegionLabel.visibility = View.GONE
        spinnerRegion.visibility = View.GONE
    }

    fun isStepValid(): Boolean = selectedCountryCode.isNotBlank()

    fun getCountryCode(): String = selectedCountryCode
    fun getRegionCode(): String = selectedRegionCode

    companion object {
        private const val ARG_COUNTRY = "arg_country"
        private const val ARG_REGION = "arg_region"

        fun newInstance(countryCode: String, regionCode: String): QLocationFragment {
            val fragment = QLocationFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_COUNTRY, countryCode)
                putString(ARG_REGION, regionCode)
            }
            return fragment
        }
    }
}
