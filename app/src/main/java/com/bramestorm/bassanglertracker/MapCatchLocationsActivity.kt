package com.bramestorm.bassanglertracker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.mappopups.PopupMapQuery
import com.bramestorm.bassanglertracker.models.MapQueryFilters
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapCatchLocationsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var btnApplyFilters: Button
    private lateinit var btnCloseMap: Button
    private lateinit var btnMapSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_catch_locations)

        btnApplyFilters = findViewById(R.id.btnApplyFilters)
        btnCloseMap = findViewById(R.id.btnCloseMap)
        btnMapSettings = findViewById(R.id.btnMapSettings)

        btnApplyFilters.setOnClickListener {
            PopupMapQuery(this) { filters ->
                applyMapFilters(filters)
            }.showPopup()
        }

        btnMapSettings.setOnClickListener {
            val options = arrayOf("Normal", "Satellite", "Terrain", "Hybrid")
            AlertDialog.Builder(this)
                .setTitle("Choose Map Type")
                .setItems(options) { _, which ->
                    map.mapType = when (which) {
                        0 -> GoogleMap.MAP_TYPE_NORMAL
                        1 -> GoogleMap.MAP_TYPE_SATELLITE
                        2 -> GoogleMap.MAP_TYPE_TERRAIN
                        3 -> GoogleMap.MAP_TYPE_HYBRID
                        else -> GoogleMap.MAP_TYPE_NORMAL
                    }
                }
                .show()
        }


        btnCloseMap.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Load default map with current month's catches
        val filters = MapQueryFilters(
            dateRange = "${getFirstDayOfMonth()} to ${getTodayAsString()}",
            species = "All",
            eventType = "Both",
            sizeType = "Weight",
            sizeRange = "0 - 9999",
            measurementType = "Imperial (lbs/oz, inches)"
        )

        applyMapFilters(filters)
    }

    fun applyMapFilters(filters: MapQueryFilters) {
        val db = CatchDatabaseHelper(this)
        db.logAllCatches()
        // Parse date range
        val (fromDate, toDate) = if (filters.dateRange.contains("to")) {
            filters.dateRange.split("to").map { it.trim() }
        } else {
            listOf(getFirstDayOfMonth(), getTodayAsString())
        }

        // Clean species: if "All", treat as no filter
        val speciesFilter =
            if (filters.species.equals("all", ignoreCase = true)) "" else filters.species

        // Clean catchType: if "Both", treat as no filter
        val catchType = when (filters.eventType.lowercase()) {
            "fun day" -> "fun"
            "tournament" -> "tournament"
            else -> "" // "Both" or "All"
        }

        // Extract numeric size range
        val (minValue, maxValue) = filters.sizeRange.split("-").mapNotNull {
            it.trim().toFloatOrNull()
        }.let {
            if (it.size == 2) it[0] to it[1] else 0f to 9999f
        }

        // Clean measurement type
        val measurementFilter =
            if (filters.measurementType.contains("all", ignoreCase = true)) "" else filters.measurementType


        // 🔍 Query database
        val catches = db.getFilteredCatchesWithLocationAdvanced(
            species = speciesFilter,
            catchType = catchType,
            measurementType = measurementFilter,
            minValue = minValue,
            maxValue = maxValue,
            fromDate = fromDate,
            toDate = toDate
        )

        Log.d("MapQuery", "Returned ${catches.size} catches")
        catches.forEachIndexed { index, catch ->
            Log.d(
                "MapCatch",
                "[$index] Species: ${catch.species}, Type: ${catch.catchType}, Lat: ${catch.latitude}, Lon: ${catch.longitude}"
            )
        }
        map.clear()

        if (catches.isEmpty()) {
            val fallbackLocation = LatLng(44.43342, -76.34939) // Gilmour Point
            map.addMarker(
                MarkerOptions()
                    .position(fallbackLocation)
                    .icon(getResizedMapIcon(R.drawable.map_icon_fall_back, 64, 64))
                    .title("No GPS data found")
                    .snippet("Default location")
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackLocation, 10f))
            Toast.makeText(
                this,
                "⚠️ No catches found.\nShowing default location.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 🗺️ Plot Pins on the Map
        for (catch in catches) {
            if (catch.latitude != null && catch.longitude != null) {
                val position = LatLng(catch.latitude!!, catch.longitude!!)

                val markerIcon = if (catch.catchType.lowercase() == "tournament") {
                    BitmapDescriptorFactory.fromResource(R.drawable.map_icon_tournament)
                } else {
                    BitmapDescriptorFactory.fromResource(R.drawable.map_icon_fun_day)
                }

                val info = when {
                    filters.sizeType.equals("weight", ignoreCase = true) -> {
                        if (filters.measurementType.contains("kg", true)) {
                            catch.totalWeightHundredthKg?.let { formatWeightKg(it) } ?: "No weight"
                        } else {
                            catch.totalWeightOz?.let { formatWeightOzToLbsOz(it) } ?: "No weight"
                        }
                    }

                    filters.sizeType.equals("length", ignoreCase = true) -> {
                        if (filters.measurementType.contains("cm", true)) {
                            catch.totalLengthTenths?.let { formatLengthCm(it) } ?: "No length"
                        } else {
                            catch.totalLengthA8th?.let { formatLengthA8thToInches(it) }
                                ?: "No length"
                        }
                    }

                    else -> "Unknown"
                }

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .icon(markerIcon)
                        .title("${catch.species}")
                        .snippet("Caught: ${catch.dateTime}\n$info")
                )
            }
        }

        // 🎯 Move to first catch with GPS
        val firstWithGps = catches.firstOrNull { it.latitude != null && it.longitude != null }
        firstWithGps?.let {
            Log.d("MapDebug", "Centering map on: ${it.species} @ ${it.latitude}, ${it.longitude}")
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude!!, it.longitude!!),
                    9f
                )
            )
        }
    }

    private fun getResizedMapIcon(resourceId: Int, width: Int, height: Int): BitmapDescriptor {
        val imageBitmap = BitmapFactory.decodeResource(resources, resourceId)
        val resized = Bitmap.createScaledBitmap(imageBitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resized)
    }

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

    private fun getFirstDayOfMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM-01", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTodayAsString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
