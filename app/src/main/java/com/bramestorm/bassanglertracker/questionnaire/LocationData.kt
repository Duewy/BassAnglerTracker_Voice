package com.bramestorm.bassanglertracker.questionnaire

data class Country(val code: String, val name: String, val regions: List<Region>)
data class Region(val code: String, val name: String)

object LocationData {

    val countries: List<Country> = listOf(
        Country(
            "CA", "Canada", listOf(
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
            "US", "United States", listOf(
                Region("AL", "Alabama"),
                Region("AK", "Alaska"),
                Region("AZ", "Arizona"),
                Region("AR", "Arkansas"),
                Region("CA", "California"),
                Region("CO", "Colorado"),
                Region("CT", "Connecticut"),
                Region("DE", "Delaware"),
                Region("FL", "Florida"),
                Region("GA", "Georgia"),
                Region("HI", "Hawaii"),
                Region("ID", "Idaho"),
                Region("IL", "Illinois"),
                Region("IN", "Indiana"),
                Region("IA", "Iowa"),
                Region("KS", "Kansas"),
                Region("KY", "Kentucky"),
                Region("LA", "Louisiana"),
                Region("ME", "Maine"),
                Region("MD", "Maryland"),
                Region("MA", "Massachusetts"),
                Region("MI", "Michigan"),
                Region("MN", "Minnesota"),
                Region("MS", "Mississippi"),
                Region("MO", "Missouri"),
                Region("MT", "Montana"),
                Region("NE", "Nebraska"),
                Region("NV", "Nevada"),
                Region("NH", "New Hampshire"),
                Region("NJ", "New Jersey"),
                Region("NM", "New Mexico"),
                Region("NY", "New York"),
                Region("NC", "North Carolina"),
                Region("ND", "North Dakota"),
                Region("OH", "Ohio"),
                Region("OK", "Oklahoma"),
                Region("OR", "Oregon"),
                Region("PA", "Pennsylvania"),
                Region("RI", "Rhode Island"),
                Region("SC", "South Carolina"),
                Region("SD", "South Dakota"),
                Region("TN", "Tennessee"),
                Region("TX", "Texas"),
                Region("UT", "Utah"),
                Region("VT", "Vermont"),
                Region("VA", "Virginia"),
                Region("WA", "Washington"),
                Region("WV", "West Virginia"),
                Region("WI", "Wisconsin"),
                Region("WY", "Wyoming")
            )
        ),
        Country("AU", "Australia", listOf(
            Region("ACT", "Australian Capital Territory"),
            Region("NSW", "New South Wales"),
            Region("NT", "Northern Territory"),
            Region("QLD", "Queensland"),
            Region("SA", "South Australia"),
            Region("TAS", "Tasmania"),
            Region("VIC", "Victoria"),
            Region("WA", "Western Australia")
        )),
        Country("GB", "United Kingdom", listOf(
            Region("ENG", "England"),
            Region("SCT", "Scotland"),
            Region("WLS", "Wales"),
            Region("NIR", "Northern Ireland")
        )),
        Country("NZ", "New Zealand", emptyList()),
        Country("IE", "Ireland", emptyList()),
        Country("ZA", "South Africa", emptyList()),
        Country("OTHER", "Other", emptyList())
    )

    fun regionsFor(countryCode: String): List<Region> =
        countries.firstOrNull { it.code == countryCode }?.regions ?: emptyList()
}
