package com.bramestorm.bassanglertracker.questionnaire

// ─────────────────────────────────────────────────────────────────────────────
// LocationData.kt
//
// Offline country + state/province list used by the first-time questionnaire.
// No network calls – data is compiled in. Expand as needed.
//
// Mirrors iOS: LocationData enum in firstTimeQuestionnaire.swift
// ─────────────────────────────────────────────────────────────────────────────

data class Country(val id: String, val name: String, val regions: List<Region>)
data class Region(val id: String, val name: String)

object LocationData {

    val countries: List<Country> = listOf(
        Country(
            id = "CA", name = "Canada",
            regions = listOf(
                Region("AB", "Alberta"),
                Region("BC", "British Columbia"),
                Region("MB", "Manitoba"),
                Region("NB", "New Brunswick"),
                Region("NL", "Newfoundland and Labrador"),
                Region("NS", "Nova Scotia"),
                Region("NT", "Northwest Territories"),
                Region("NU", "Nunavut"),
                Region("ON", "Ontario"),
                Region("PE", "Prince Edward Island"),
                Region("QC", "Quebec"),
                Region("SK", "Saskatchewan"),
                Region("YT", "Yukon")
            )
        ),
        Country(
            id = "US", name = "United States",
            regions = listOf(
                Region("AL", "Alabama"), Region("AK", "Alaska"),
                Region("AZ", "Arizona"), Region("AR", "Arkansas"),
                Region("CA", "California"), Region("CO", "Colorado"),
                Region("CT", "Connecticut"), Region("DE", "Delaware"),
                Region("FL", "Florida"), Region("GA", "Georgia"),
                Region("HI", "Hawaii"), Region("ID", "Idaho"),
                Region("IL", "Illinois"), Region("IN", "Indiana"),
                Region("IA", "Iowa"), Region("KS", "Kansas"),
                Region("KY", "Kentucky"), Region("LA", "Louisiana"),
                Region("ME", "Maine"), Region("MD", "Maryland"),
                Region("MA", "Massachusetts"), Region("MI", "Michigan"),
                Region("MN", "Minnesota"), Region("MS", "Mississippi"),
                Region("MO", "Missouri"), Region("MT", "Montana"),
                Region("NE", "Nebraska"), Region("NV", "Nevada"),
                Region("NH", "New Hampshire"), Region("NJ", "New Jersey"),
                Region("NM", "New Mexico"), Region("NY", "New York"),
                Region("NC", "North Carolina"), Region("ND", "North Dakota"),
                Region("OH", "Ohio"), Region("OK", "Oklahoma"),
                Region("OR", "Oregon"), Region("PA", "Pennsylvania"),
                Region("RI", "Rhode Island"), Region("SC", "South Carolina"),
                Region("SD", "South Dakota"), Region("TN", "Tennessee"),
                Region("TX", "Texas"), Region("UT", "Utah"),
                Region("VT", "Vermont"), Region("VA", "Virginia"),
                Region("WA", "Washington"), Region("WV", "West Virginia"),
                Region("WI", "Wisconsin"), Region("WY", "Wyoming")
            )
        ),
        Country(
            id = "GB", name = "United Kingdom",
            regions = listOf(
                Region("ENG", "England"), Region("SCT", "Scotland"),
                Region("WLS", "Wales"), Region("NIR", "Northern Ireland")
            )
        ),
        Country(
            id = "AU", name = "Australia",
            regions = listOf(
                Region("ACT", "Australian Capital Territory"),
                Region("NSW", "New South Wales"),
                Region("NT",  "Northern Territory"),
                Region("QLD", "Queensland"),
                Region("SA",  "South Australia"),
                Region("TAS", "Tasmania"),
                Region("VIC", "Victoria"),
                Region("WA",  "Western Australia")
            )
        ),
        Country(id = "OTHER", name = "Other / Not Listed", regions = emptyList())
    )

    /** Returns the country for the given ISO-style id, or null if not found. */
    fun forId(id: String): Country? = countries.firstOrNull { it.id == id }
}
