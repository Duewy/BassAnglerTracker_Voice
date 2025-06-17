package com.bramestorm.bassanglertracker.database

import java.util.Locale

        // todo rework this to lock down specific areas better...

object RegionClassifier {

    fun getRegionFromLocation(country: String?, admin: String?, subAdmin: String?): String {
        val countryLower = country?.lowercase(Locale.getDefault()) ?: return "unknown"
        val adminLower = admin?.lowercase(Locale.getDefault()) ?: ""
        val subAdminLower = subAdmin?.lowercase(Locale.getDefault()) ?: ""

        return when {
            countryLower.contains("united states") -> classifyUsRegion(adminLower, subAdminLower)
            countryLower.contains("canada") -> classifyCanadaRegion(adminLower, subAdminLower)
            countryLower.contains("australia") -> classifyAustraliaRegion(adminLower, subAdminLower)
            countryLower.contains("mexico") -> classifyMexicoRegion(adminLower, subAdminLower)
            else -> countryLower.replaceFirstChar { it.uppercase() }
        }
    }

    private fun classifyUsRegion(admin: String, subAdmin: String): String {
        // Example: Florida
        if (admin.contains("florida")) {
            return when {
                subAdmin.contains("miami") || subAdmin.contains("dade") || subAdmin.contains("broward") ->
                    "southeast florida coast"
                subAdmin.contains("orlando") || subAdmin.contains("ocala") || subAdmin.contains("lake") ->
                    "central florida inland"
                else -> "florida"
            }
        }

        // Example: New York
        if (admin.contains("new york")) {
            return when {
                subAdmin.contains("erie") || subAdmin.contains("buffalo") || subAdmin.contains("niagara") ->
                    "western new york"
                subAdmin.contains("syracuse") || subAdmin.contains("onondaga") -> "central new york"
                else -> "new york"
            }
        }

        // Add more U.S. states as needed
        return admin.replaceFirstChar { it.uppercase() }
    }

    private fun classifyCanadaRegion(admin: String, subAdmin: String): String {
        if (admin.contains("ontario")) {
            return when {
                subAdmin.contains("ottawa") || subAdmin.contains("leeds") -> "eastern ontario"
                subAdmin.contains("sudbury") -> "northern ontario"
                else -> "ontario"
            }
        }

        if (admin.contains("british columbia")) {
            return if (subAdmin.contains("prince george") || subAdmin.contains("kitimat"))
                "northern bc"
            else
                "british columbia"
        }

        if (admin.contains("alberta") || admin.contains("saskatchewan") || admin.contains("manitoba")) {
            return "prairies"
        }

        return admin.replaceFirstChar { it.uppercase() }
    }

    private fun classifyAustraliaRegion(admin: String, subAdmin: String): String {
        return when {
            admin.contains("new south wales") -> "new south wales"
            admin.contains("queensland") -> "queensland"
            admin.contains("northern territory") -> "northern territory"
            else -> admin.replaceFirstChar { it.uppercase() }
        }
    }

    private fun classifyMexicoRegion(admin: String, subAdmin: String): String {
        return when {
            admin.contains("baja california") -> "baja peninsula"
            admin.contains("sinaloa") || admin.contains("mazatlan") -> "western mexico coast"
            else -> admin.replaceFirstChar { it.uppercase() }
        }
    }
}
